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

package com.googlesource.gerrit.plugins.manager;

import com.google.common.cache.CacheLoader;
import com.google.gerrit.common.Version;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.manager.PluginsCentralLoader.ListKey;
import com.googlesource.gerrit.plugins.manager.repository.PluginInfo;
import com.googlesource.gerrit.plugins.manager.repository.PluginsRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class PluginsCentralLoader extends CacheLoader<ListKey, Collection<PluginInfo>> {

  public static class ListKey {
    static final ListKey ALL = new ListKey();

    private ListKey() {}
  }

  private static final String GERRIT_VERSION = Version.getVersion();

  private final DynamicSet<PluginsRepository> repositories;

  @Inject
  public PluginsCentralLoader(DynamicSet<PluginsRepository> repositories) {
    this.repositories = repositories;
  }

  @Override
  public Collection<PluginInfo> load(ListKey all) throws Exception {
    Map<String, PluginInfo> pluginsMap = new HashMap<>();
    Collection<PluginInfo> plugins;
    for (PluginsRepository pluginsRepository : repositories) {
      plugins = pluginsRepository.list(GERRIT_VERSION);
      addAll(pluginsMap, plugins);
    }
    return pluginsMap.values();
  }

  private void addAll(Map<String, PluginInfo> pluginsMap, Collection<PluginInfo> plugins) {
    for (PluginInfo pluginInfo : plugins) {
      PluginInfo currPlugin = pluginsMap.get(pluginInfo.name);
      if (currPlugin == null || isLaterVersion(pluginInfo.version, currPlugin.version)) {
        pluginsMap.put(pluginInfo.name, pluginInfo);
      }
    }
  }

  private boolean isLaterVersion(String newVersion, String currVersion) {
    String[] vals1 = newVersion.replaceAll("-", ".").split("\\.");
    String[] vals2 = currVersion.replaceAll("-", ".").split("\\.");
    int i = 0;

    while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
      i++;
    }

    if (i < vals1.length && i < vals2.length) {
      return compareNumOrStrings(vals1[i], vals2[i]) > 0;
    }
    return vals1.length - vals2.length > 0;
  }

  private int compareNumOrStrings(String v1, String v2) {
    try {
      return Integer.parseInt(v1) - Integer.parseInt(v2);
    } catch (NumberFormatException e) {
      return v1.compareTo(v2);
    }
  }
}
