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

import com.google.gerrit.extensions.client.MenuItem;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.plugins.PluginLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Singleton
public class PluginManagerTopMenu implements TopMenu {

  private final PluginLoader loader;
  private final PluginManagerConfig config;
  private final List<MenuEntry> menuEntries;

  @Inject
  public PluginManagerTopMenu(
      @PluginCanonicalWebUrlPath String myUrl, PluginLoader loader, PluginManagerConfig config) {
    this.loader = loader;
    this.config = config;
    this.menuEntries =
        Arrays.asList(
            new MenuEntry(
                "Plugins",
                Arrays.asList(new MenuItem("Manage", myUrl + "static/index.html", "_self"))));
  }

  @Override
  public List<MenuEntry> getEntries() {
    if (loader.isRemoteAdminEnabled() && config.canAdministerPlugins()) {
      return menuEntries;
    }
    return Collections.emptyList();
  }
}
