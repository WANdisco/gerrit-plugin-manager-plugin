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

import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.AcceptsCreate;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestCollection;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.server.plugins.PluginResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class AvailablePluginsCollection implements
    RestCollection<TopLevelResource, PluginResource>,
    AcceptsCreate<TopLevelResource> {

  private final DynamicMap<RestView<PluginResource>> views;
  private final Provider<ListAvailablePlugins> list;

  @Inject
  AvailablePluginsCollection(DynamicMap<RestView<PluginResource>> views,
                             Provider<ListAvailablePlugins> list) {
    this.views = views;
    this.list = list;
  }

  @Override
  public RestView<TopLevelResource> list() throws ResourceNotFoundException {
    return list.get();
  }

  @Override
  public PluginResource parse(TopLevelResource parent, IdString id)
      throws ResourceNotFoundException {
      throw new ResourceNotFoundException(id);
  }

  @Override
  public DynamicMap<RestView<PluginResource>> views() {
    return views;
  }

  @Override
  public <I> RestModifyView<TopLevelResource, I> create(
      TopLevelResource parent, IdString id) throws RestApiException {
    throw new IllegalArgumentException("Operation not supported");
  }
}
