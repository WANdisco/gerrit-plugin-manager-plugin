// Copyright (C) 2016 The Android Open Source Project
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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.httpd.WebLoginListener;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.plugins.PluginLoader;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FirstWebLoginListener implements WebLoginListener {
  private final Path pluginData;
  private final PluginLoader pluginLoader;
  private final PluginManagerConfig config;
  private final String pluginUrlPath;

  @Inject
  public FirstWebLoginListener(
      PluginLoader pluginLoader,
      @PluginData Path pluginData,
      @PluginCanonicalWebUrlPath String pluginUrlPath,
      PluginManagerConfig config) {
    this.pluginData = pluginData;
    this.pluginLoader = pluginLoader;
    this.pluginUrlPath = pluginUrlPath;
    this.config = config;
  }

  @Override
  public void onLogin(IdentifiedUser user, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (pluginLoader.isRemoteAdminEnabled() && config.canAdministerPlugins()) {
      Path firstLoginFile = pluginData.resolve("firstLogin." + user.getAccountId().get());
      if (!firstLoginFile.toFile().exists()) {
        response.sendRedirect(pluginUrlPath + "static/intro.html");

        String timestamp =
            DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
        Files.write(firstLoginFile, timestamp.getBytes(UTF_8), StandardOpenOption.CREATE);
      }
    }
  }

  @Override
  public void onLogout(
      IdentifiedUser user, HttpServletRequest request, HttpServletResponse response) {}
}
