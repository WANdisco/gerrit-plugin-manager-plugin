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

import com.google.common.collect.Maps;
import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.server.OutputFormat;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.manager.repository.PluginInfo;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** List plugins available for installation. */
@RequiresCapability(GlobalCapability.VIEW_PLUGINS)
public class ListAvailablePlugins implements RestReadView<TopLevelResource> {
  private final PluginsCentralLoader loader;

  @Inject
  public ListAvailablePlugins(PluginsCentralLoader loader) {
    this.loader = loader;
  }

  @Override
  public Object apply(TopLevelResource resource) throws RestApiException {
    return display();
  }

  public JsonElement display() throws RestApiException {
    Map<String, PluginInfo> output = Maps.newTreeMap();
    List<PluginInfo> plugins;
    try {
      plugins = loader.availablePlugins();
    } catch (IOException e) {
      throw new RestApiException(
          "Unable to load the list of available plugins", e);
    }
    Collections.sort(plugins, new Comparator<PluginInfo>() {
      @Override
      public int compare(PluginInfo a, PluginInfo b) {
        return a.name.compareTo(b.name);
      }
    });

    for (PluginInfo p : plugins) {
      output.put(p.name, p);
    }

    return OutputFormat.JSON.newGson().toJsonTree(output,
        new TypeToken<Map<String, Object>>() {}.getType());
  }
}
