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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.permissions.GlobalPermission;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class PluginManagerConfig {
  private static final String DEFAULT_GERRIT_CI_URL = "https://gerrit-ci.gerritforge.com";

  private final PluginConfig config;
  private final Provider<CurrentUser> currentUserProvider;
  private final PermissionBackend permissions;

  @Inject
  public PluginManagerConfig(
      PluginConfigFactory configFactory,
      @PluginName String pluginName,
      Provider<CurrentUser> currentUserProvider,
      PermissionBackend permissions) {
    this.config = configFactory.getFromGerritConfig(pluginName);
    this.currentUserProvider = currentUserProvider;
    this.permissions = permissions;
  }

  public String getJenkinsUrl() {
    return config.getString("jenkinsUrl", DEFAULT_GERRIT_CI_URL);
  }

  public boolean isCachePreloadEnabled() {
    return config.getBoolean("preload", true);
  }

  public boolean canAdministerPlugins() {
    try {
      permissions.user(currentUserProvider).check(GlobalPermission.ADMINISTRATE_SERVER);
      return true;
    } catch (AuthException | PermissionBackendException e) {
      return false;
    }
  }
}
