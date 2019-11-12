// Copyright (C) 2019 The Android Open Source Project
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

package com.google.gerrit.server.restapi.config; // Needed to get access to the ListTopMenus

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.gerrit.acceptance.testsuite.request.RequestScopeOperations;
import com.google.gerrit.extensions.client.MenuItem;
import com.google.gerrit.extensions.webui.TopMenu.MenuEntry;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

@NoHttpd
@TestPlugin(
    name = "plugin-manager",
    sysModule = "com.googlesource.gerrit.plugins.manager.Module",
    httpModule = "com.googlesource.gerrit.plugins.manager.WebModule")
public class PluginManagerTopMenuIT extends LightweightPluginDaemonTest {

  @Inject ListTopMenus topMenus;

  @Inject private RequestScopeOperations requestScopeOperations;

  @Test
  @GerritConfig(name = "plugins.allowRemoteAdmin", value = "true")
  public void showTopMenuForGerritAdministratorsWhenAllowRemoteAdmin() throws Exception {
    assertThat(pluginTopMenuEntries()).isNotEmpty();
  }

  @Test
  @GerritConfig(name = "plugins.allowRemoteAdmin", value = "true")
  public void topMenuContainsPluginsManagementItem() throws Exception {
    Optional<MenuEntry> topMenuEntry = pluginTopMenuEntries().stream().findFirst();
    assertThat(topMenuEntry.map(m -> m.name)).isEqualTo(Optional.of("Plugins"));

    Optional<MenuItem> pluginsMenuItem = topMenuEntry.flatMap(m -> m.items.stream().findFirst());
    assertThat(pluginsMenuItem.map(m -> m.name)).isEqualTo(Optional.of("Manage"));
  }

  @Test
  @GerritConfig(name = "plugins.allowRemoteAdmin", value = "true")
  public void hideTopMenuForRegularUsersEvenWhenAllowRemoteAdmin() throws Exception {
    requestScopeOperations.setApiUser(user.id());
    assertThat(pluginTopMenuEntries()).isEmpty();
  }

  @Test
  public void hideTopMenuByDefault() throws Exception {
    assertThat(pluginTopMenuEntries()).isEmpty();
  }

  private List<MenuEntry> pluginTopMenuEntries() throws Exception {
    List<MenuEntry> topMenuItems = topMenus.apply(new ConfigResource()).value();
    return topMenuItems;
  }
}
