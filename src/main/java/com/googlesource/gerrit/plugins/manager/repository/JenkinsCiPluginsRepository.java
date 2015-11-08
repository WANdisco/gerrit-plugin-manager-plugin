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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.manager.GerritVersionBranch;
import com.googlesource.gerrit.plugins.manager.PluginManagerConfig;
import com.googlesource.gerrit.plugins.manager.gson.SmartGson;
import com.googlesource.gerrit.plugins.manager.gson.SmartJson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Singleton
public class JenkinsCiPluginsRepository implements PluginsRepository {

  private static final Logger log = LoggerFactory
      .getLogger(JenkinsCiPluginsRepository.class);

  private static final Optional<PluginInfo> noPluginInfo = Optional.absent();

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
  public JenkinsCiPluginsRepository(Provider<SmartGson> gsonProvider,
      PluginManagerConfig config) {
    this.gsonProvider = gsonProvider;
    this.config = config;
  }

  @Override
  public List<PluginInfo> list(String gerritVersion) throws IOException {
    List<PluginInfo> list = cache.get(gerritVersion);
    if(list == null) {
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
          gson.get(config.getJenkinsUrl() + "/view/" + viewName + "/api/json",
              View.class).jobs;

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

  private Optional<PluginInfo> getPluginInfo(final SmartGson gson, String url)
      throws IOException {
    SmartJson jobDetails = gson.get(url + "/api/json");
    Optional<SmartJson> lastSuccessfulBuild =
        jobDetails.getOptional("lastSuccessfulBuild");

    return lastSuccessfulBuild.transform(
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
        }).or(noPluginInfo);
  }

  private Optional<PluginInfo> getPluginArtifactInfo(SmartGson gson, String url)
      throws IOException {
    SmartJson buildExecution = gson.get(url + "/api/json");
    JsonArray artifacts =
        buildExecution.get("artifacts").get().getAsJsonArray();
    if (artifacts.size() == 0) {
      return Optional.absent();
    }

    SmartJson artifactJson = SmartJson.of(artifacts.get(0));
    String pluginFileName = artifactJson.getString("fileName");

    String pluginVersion = "";
    for (JsonElement elem : buildExecution.get("actions").get()
        .getAsJsonArray()) {
      SmartJson elemJson = SmartJson.of(elem);
      Optional<SmartJson> lastBuildRevision =
          elemJson.getOptional("lastBuiltRevision");

      if (lastBuildRevision.isPresent()) {
        pluginVersion = lastBuildRevision.get().getString("SHA1");
      }
    }

    return Optional.of(new PluginInfo(pluginFileName, pluginVersion));
  }
}
