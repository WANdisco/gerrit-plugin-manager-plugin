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

  private static final Optional<PluginInfo> noPluginInfo = Optional.empty();

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

    return lastSuccessfulBuild
        .map(
            new Function<SmartJson, Optional<PluginInfo>>() {
              @Override
              public Optional<PluginInfo> apply(SmartJson build) {
                String buildUrl = build.getString("url");
                try {
                  return getPluginArtifactInfo(gson, buildUrl);
                } catch (IOException e) {
                  log.warn("Cannot retrieve plugin artifact info from {}", buildUrl);
                  return noPluginInfo;
                }
              }
            })
        .orElse(noPluginInfo);
  }

  private Optional<PluginInfo> getPluginArtifactInfo(SmartGson gson, String url)
      throws IOException {
    SmartJson buildExecution = gson.get(url + "/api/json");
    JsonArray artifacts = buildExecution.get("artifacts").get().getAsJsonArray();
    if (artifacts.size() == 0) {
      return Optional.empty();
    }

    Optional<SmartJson> artifactJson = findArtifact(artifacts, ".jar");
    if (!artifactJson.isPresent()) {
      return Optional.empty();
    }

    String pluginPath = artifactJson.get().getString("relativePath");

    String[] pluginPathParts = pluginPath.split("/");
    String pluginName =
        isMavenBuild(pluginPathParts)
            ? fixPluginNameForMavenBuilds(pluginPathParts)
            : pluginPathParts[pluginPathParts.length - 2];

    String pluginUrl = String.format("%s/artifact/%s", buildExecution.getString("url"), pluginPath);

    String pluginVersion = "";
    Optional<SmartJson> verArtifactJson = findArtifact(artifacts, ".jar-version");
    if (verArtifactJson.isPresent()) {
      String versionUrl =
          String.format(
              "%s/artifact/%s",
              buildExecution.getString("url"), verArtifactJson.get().getString("relativePath"));
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(new URL(versionUrl).openStream()), 4096)) {
        pluginVersion = reader.readLine();
      }
    }

    String sha1 = "";
    for (JsonElement elem : buildExecution.get("actions").get().getAsJsonArray()) {
      SmartJson elemJson = SmartJson.of(elem);
      Optional<SmartJson> lastBuildRevision = elemJson.getOptional("lastBuiltRevision");

      if (lastBuildRevision.isPresent()) {
        sha1 = lastBuildRevision.get().getString("SHA1").substring(0, 8);
      }
    }

    return Optional.of(new PluginInfo(pluginName, pluginVersion, sha1, pluginUrl));
  }

  private String fixPluginNameForMavenBuilds(String[] pluginPathParts) {
    String mavenPluginFilename =
        StringUtils.substringBeforeLast(pluginPathParts[pluginPathParts.length - 1], ".");
    int versionDelim = mavenPluginFilename.indexOf('-');
    return versionDelim > 0 ? mavenPluginFilename.substring(0, versionDelim) : mavenPluginFilename;
  }

  private boolean isMavenBuild(String[] pluginPathParts) {
    return pluginPathParts[pluginPathParts.length - 2].equals("target");
  }

  private Optional<SmartJson> findArtifact(JsonArray artifacts, String string) {
    for (int i = 0; i < artifacts.size(); i++) {
      SmartJson artifact = SmartJson.of(artifacts.get(i));
      if (artifact.getString("relativePath").endsWith(string)) {
        return Optional.of(artifact);
      }
    }

    return Optional.empty();
  }
}
