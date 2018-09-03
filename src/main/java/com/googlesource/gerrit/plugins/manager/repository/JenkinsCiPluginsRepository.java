// Copyright (C) 2015 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.manager.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.manager.GerritVersionBranch;
import com.googlesource.gerrit.plugins.manager.PluginManagerConfig;
import com.googlesource.gerrit.plugins.manager.gson.SmartGson;
import com.googlesource.gerrit.plugins.manager.gson.SmartJson;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class JenkinsCiPluginsRepository implements PluginsRepository {

  private static final Logger log = LoggerFactory.getLogger(JenkinsCiPluginsRepository.class);

  private final PluginManagerConfig config;

  private HashMap<String, List<PluginInfo>> cache = new HashMap<>();

  static class View {
    String name;
    Job[] jobs;
  }

  static class Job {
    String name;
    String url;
    String color;
  }

  private final Provider<SmartGson> gsonProvider;

  @Inject
  public JenkinsCiPluginsRepository(Provider<SmartGson> gsonProvider, PluginManagerConfig config) {
    this.gsonProvider = gsonProvider;
    this.config = config;
  }

  @Override
  public List<PluginInfo> list(String gerritVersion) throws IOException {
    List<PluginInfo> list = cache.get(gerritVersion);
    if (list == null) {
      list = getList(gerritVersion);
      cache.put(gerritVersion, list);
    }
    return list;
  }

  private List<PluginInfo> getList(String gerritVersion) throws IOException {
    SmartGson gson = gsonProvider.get();
    String viewName = "Plugins-" + GerritVersionBranch.getBranch(gerritVersion);
    List<PluginInfo> plugins = new ArrayList<>();

    try {
      Job[] jobs =
          gson.get(config.getJenkinsUrl() + "/view/" + viewName + "/api/json", View.class).jobs;

      for (Job job : jobs) {
        if (job.color.equals("blue")) {
          Optional<PluginInfo> pluginInfo = getPluginInfo(gson, job.url);
          if (pluginInfo.isPresent()) {
            plugins.add(pluginInfo.get());
          }
        }
      }
    } catch (FileNotFoundException e) {
      log.warn("No plugins available for Gerrit version " + gerritVersion, e);
    }

    return plugins;
  }

  private Optional<PluginInfo> getPluginInfo(final SmartGson gson, String url) throws IOException {
    SmartJson jobDetails = gson.get(url + "/api/json");
    Optional<SmartJson> lastSuccessfulBuild = jobDetails.getOptional("lastSuccessfulBuild");

    return lastSuccessfulBuild.flatMap(
        new Function<SmartJson, Optional<PluginInfo>>() {
          @Override
          public Optional<PluginInfo> apply(SmartJson build) {
            String buildUrl = build.getString("url");
            return getPluginArtifactInfo(buildUrl);
          }
        });
  }

  private Optional<PluginInfo> getPluginArtifactInfo(String url) {
    Optional<SmartJson> buildExecution = tryGetJson(url + "/api/json");
    Optional<JsonArray> artifacts =
        buildExecution.map(json -> json.get("artifacts").get().getAsJsonArray());
    if (artifacts.orElse(new JsonArray()).size() == 0) {
      return Optional.empty();
    }

    Optional<SmartJson> artifactJson = artifacts.flatMap(a -> findArtifact(a, ".jar"));
    if (!artifactJson.isPresent()) {
      return Optional.empty();
    }

    String pluginPath = artifactJson.get().getString("relativePath");

    String[] pluginPathParts = pluginPath.split("/");
    String pluginName =
        isMavenBuild(pluginPathParts)
            ? fixPluginNameForMavenBuilds(pluginPathParts)
            : pluginNameOfJar(pluginPathParts);

    String pluginUrl =
        String.format("%s/artifact/%s", buildExecution.get().getString("url"), pluginPath);

    Optional<String> pluginVersion =
        fetchArtifact(buildExecution.get(), artifacts.get(), ".jar-version");
    Optional<String> pluginDescription =
        fetchArtifactJson(buildExecution.get(), artifacts.get(), ".json")
            .flatMap(json -> json.getOptionalString("description"));

    for (JsonElement elem : buildExecution.get().get("actions").get().getAsJsonArray()) {
      SmartJson elemJson = SmartJson.of(elem);
      Optional<SmartJson> lastBuildRevision = elemJson.getOptional("lastBuiltRevision");

      if (lastBuildRevision.isPresent()) {
        String sha1 = lastBuildRevision.get().getString("SHA1").substring(0, 8);
        return pluginVersion.map(
            version ->
                new PluginInfo(pluginName, pluginDescription.orElse(""), version, sha1, pluginUrl));
      }
    }

    return Optional.empty();
  }

  private Optional<String> fetchArtifact(
      SmartJson buildExecution, JsonArray artifacts, String artifactSuffix) {
    StringBuilder artifactBody = new StringBuilder();
    Optional<SmartJson> verArtifactJson = findArtifact(artifacts, artifactSuffix);
    if (verArtifactJson.isPresent()) {
      String versionUrl =
          String.format(
              "%s/artifact/%s",
              buildExecution.getString("url"), verArtifactJson.get().getString("relativePath"));
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(new URL(versionUrl).openStream()), 4096)) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (artifactBody.length() > 0) {
            artifactBody.append("\n");
          }
          artifactBody.append(line);
        }
      } catch (Exception e) {
        log.error("Unable to fetch artifact from " + versionUrl);
        return Optional.empty();
      }
    }
    return Optional.of(artifactBody.toString());
  }

  private Optional<SmartJson> fetchArtifactJson(
      SmartJson buildExecution, JsonArray artifacts, String artifactSuffix) {
    Optional<SmartJson> jsonArtifact = findArtifact(artifacts, artifactSuffix);
    return jsonArtifact.flatMap(
        artifactJson ->
            tryGetJson(
                String.format(
                    "%s/artifact/%s",
                    buildExecution.getString("url"),
                    jsonArtifact.get().getString("relativePath"))));
  }

  private Optional<SmartJson> tryGetJson(String url) {
    try {
      return Optional.of(gsonProvider.get().get(url));
    } catch (IOException e) {
      log.error("Cannot get JSON from " + url, e);
      return Optional.empty();
    }
  }

  private String fixPluginNameForMavenBuilds(String[] pluginPathParts) {
    String mavenPluginFilename =
        StringUtils.substringBeforeLast(pluginPathParts[pluginPathParts.length - 1], ".");
    int versionDelim = mavenPluginFilename.indexOf('-');
    return versionDelim > 0 ? mavenPluginFilename.substring(0, versionDelim) : mavenPluginFilename;
  }

  private String pluginNameOfJar(String[] pluginJarParts) {
    int filePos = pluginJarParts.length - 1;
    int pathPos = filePos - 1;

    if (pluginJarParts[filePos].startsWith(pluginJarParts[pathPos])) {
      return pluginJarParts[pathPos];
    }

    int jarExtPos = pluginJarParts[filePos].indexOf(".jar");
    return pluginJarParts[filePos].substring(0, jarExtPos);
  }

  private boolean isMavenBuild(String[] pluginPathParts) {
    return pluginPathParts[pluginPathParts.length - 2].equals("target");
  }

  private Optional<SmartJson> findArtifact(JsonArray artifacts, String string) {
    for (int i = 0; i < artifacts.size(); i++) {
      SmartJson artifact = SmartJson.of(artifacts.get(i));
      String path = artifact.getString("relativePath");
      if (path.endsWith(string) && !path.endsWith("-static" + string)) {
        return Optional.of(artifact);
      }
    }

    return Optional.empty();
  }
}
