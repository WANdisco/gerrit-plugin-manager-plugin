// Copyright (C) 2017 The Android Open Source Project
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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Optional;

@Singleton
public class CorePluginsDescriptions {
  private final HashMap<String, String> pluginsDescriptions;

  @Inject
  public CorePluginsDescriptions() {
    pluginsDescriptions = new HashMap<>();
    pluginsDescriptions.put("codemirror-editor", "CodeMirror plugin for polygerrit");
    pluginsDescriptions.put(
        "commit-message-length-validator",
        "Plugin to validate that commit messages conform to length limits");
    pluginsDescriptions.put("delete-project", "Provides the ability to delete a project");
    pluginsDescriptions.put("download-commands", "Adds the standard download schemes and commands");
    pluginsDescriptions.put("gitiles", "Plugin running Gitiles alongside a Gerrit server");
    pluginsDescriptions.put("hooks", "Old-style fork+exec hooks");
    pluginsDescriptions.put(
        "plugin-manager", "Adds support for discovering and installing other plugins");
    pluginsDescriptions.put("replication", "Copies to other servers using the Git protocol");
    pluginsDescriptions.put(
        "reviewnotes", "Annotates merged commits using notes on refs/notes/review");
    pluginsDescriptions.put(
        "singleusergroup", "GroupBackend enabling users to be directly added to access rules");
    pluginsDescriptions.put(
        "webhooks", "Allows to propagate Gerrit events to remote http endpoints");

    // Additional Replicated Core Plugins
    pluginsDescriptions.put("lfs",
            "Enables Git Large File Storage integration for Gerrit projects.");
    pluginsDescriptions.put("its-base",
            "Base functionality for interaction with Issue Tracking Systems");
    pluginsDescriptions.put("its-jira",
            "Enables links to Jira ITS in comments and publishing Gerrit events as ticket comments.");
  }

  public Optional<String> get(String plugin) {
    return Optional.ofNullable(pluginsDescriptions.get(plugin));
  }
}
