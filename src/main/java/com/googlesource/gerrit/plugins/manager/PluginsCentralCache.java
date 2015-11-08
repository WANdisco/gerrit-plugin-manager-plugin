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

import com.google.common.cache.LoadingCache;
import com.google.gerrit.server.cache.CacheModule;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

import com.googlesource.gerrit.plugins.manager.PluginsCentralLoader.ListKey;
import com.googlesource.gerrit.plugins.manager.repository.PluginInfo;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PluginsCentralCache {

  private final LoadingCache<ListKey, Collection<PluginInfo>> pluginsCache;

  public static final String PLUGINS_LIST_CACHE_NAME = "plugins_list";

  @Inject
  public PluginsCentralCache(
      @Named(PLUGINS_LIST_CACHE_NAME) LoadingCache<ListKey, Collection<PluginInfo>> pluginsCache) {
    this.pluginsCache = pluginsCache;
  }

  public Collection<PluginInfo> availablePlugins() throws ExecutionException {
    return pluginsCache.get(ListKey.ALL);
  }

  static CacheModule module() {
    return new CacheModule() {
      @Override
      protected void configure() {
        cache(PluginsCentralCache.PLUGINS_LIST_CACHE_NAME, ListKey.class,
            new TypeLiteral<Collection<PluginInfo>>() {}).expireAfterWrite(1,
            TimeUnit.DAYS).loader(PluginsCentralLoader.class);

        bind(PluginsCentralCache.class);
      }
    };
  }
}
