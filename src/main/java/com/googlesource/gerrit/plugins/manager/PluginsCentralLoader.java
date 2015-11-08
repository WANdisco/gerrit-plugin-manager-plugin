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
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.manager.PluginsCentralLoader.ListKey;
import com.googlesource.gerrit.plugins.manager.repository.PluginInfo;
import com.googlesource.gerrit.plugins.manager.repository.PluginsRepository;

import java.util.List;

@Singleton
public class PluginsCentralLoader extends
    CacheLoader<ListKey, List<PluginInfo>> {

  public static class ListKey {
    static final ListKey ALL = new ListKey();

    private ListKey() {}
  }

  private final PluginsRepository repository;

  @Inject
  public PluginsCentralLoader(PluginsRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<PluginInfo> load(ListKey all) throws Exception {
    return repository.list(Version.getVersion());
  }
}
