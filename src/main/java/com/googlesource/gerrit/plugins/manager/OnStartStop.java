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

import com.google.gerrit.common.Version;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.manager.repository.PluginInfo;
import com.googlesource.gerrit.plugins.manager.repository.PluginsRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

public class OnStartStop implements LifecycleListener {
  private static final Logger log = LoggerFactory.getLogger(OnStartStop.class);

  private final PluginsRepository pluginsRepo;

  private final String pluginName;

  private final PluginManagerConfig config;

  @Inject
  public OnStartStop(PluginsRepository pluginsRepo,
      @PluginName String pluginName, PluginManagerConfig config) {
    this.pluginsRepo = pluginsRepo;
    this.pluginName = pluginName;
    this.config = config;
  }

  @Override
  public void start() {
    if (config.isCachePreloadEnabled()) {
      Thread preloader = new Thread(new Runnable() {

        @Override
        public void run() {
          log.info("Start-up: pre-loading list of plugins from registry");
          try {
            Collection<PluginInfo> plugins =
                pluginsRepo.list(Version.getVersion());
            log.info("{} plugins successfully pre-loaded", plugins.size());
          } catch (IOException e) {
            log.error("Cannot access plugins list at this time", e);
          }
        }
      });
      preloader.setName(pluginName + "-preloader");
      preloader.start();
    }
  }

  @Override
  public void stop() {
  }
}
