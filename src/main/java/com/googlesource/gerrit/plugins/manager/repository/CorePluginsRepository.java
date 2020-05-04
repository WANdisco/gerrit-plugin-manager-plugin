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

package com.googlesource.gerrit.plugins.manager.repository;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.io.Files;
import com.google.gerrit.common.Version;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorePluginsRepository implements PluginsRepository {
  private static final Logger log = LoggerFactory.getLogger(CorePluginsRepository.class);
  private static final String GERRIT_VERSION = Version.getVersion();

  private final SitePaths site;
  private final CorePluginsDescriptions pluginsDescriptions;

  @Inject
  public CorePluginsRepository(SitePaths site, CorePluginsDescriptions pd) {
    this.site = site;
    this.pluginsDescriptions = pd;
  }

  static class SelectPluginsFromJar implements Predicate<JarEntry> {
    @Override
    public boolean apply(JarEntry entry) {
      String entryName = entry.getName();
      return (entryName.startsWith("WEB-INF/plugins") && entryName.endsWith(".jar"));
    }
  }

  class ExtractPluginInfoFromJarEntry implements Function<JarEntry, PluginInfo> {
    private String gerritWarFilename;

    public ExtractPluginInfoFromJarEntry(String gerritWarFilename) {
      this.gerritWarFilename = gerritWarFilename;
    }

    @Override
    public PluginInfo apply(JarEntry entry) {
      try {
        Path entryName = Paths.get(entry.getName());
        URI pluginUrl = new URI("jar:file:" + gerritWarFilename + "!/" + entry.getName());
        try (JarInputStream pluginJar = new JarInputStream(pluginUrl.toURL().openStream())) {
          Manifest manifestJarEntry = getManifestEntry(pluginJar);
          if (manifestJarEntry != null) {
            Attributes pluginAttributes = manifestJarEntry.getMainAttributes();
            String pluginName = pluginAttributes.getValue("Gerrit-PluginName");
            return new PluginInfo(
                pluginName,
                pluginsDescriptions.get(pluginName).orElse(""),
                pluginAttributes.getValue("Implementation-Version"),
                "",
                pluginUrl.toString());
          }
          return new PluginInfo(
              dropFileExtension(entryName.getFileName().toString()),
              "",
              "",
              "",
              pluginUrl.toString());
        } catch (IOException e) {
          log.error("Unable to open plugin " + pluginUrl, e);
          return null;
        }
      } catch (URISyntaxException e) {
        log.error("Invalid plugin filename", e);
        return null;
      }
    }

    private String dropFileExtension(String fileName) {
      String extension = Files.getFileExtension(fileName);
      return fileName.substring(0, fileName.length() - extension.length() - 1);
    }

    private Manifest getManifestEntry(JarInputStream pluginJar) throws IOException {
      for (JarEntry entry = pluginJar.getNextJarEntry();
          entry != null;
          entry = pluginJar.getNextJarEntry()) {
        if (entry.getName().equals("META-INF/MANIFEST.MF")) {
          return new Manifest(pluginJar);
        }
      }
      return null;
    }
  }

  @Override
  public Collection<PluginInfo> list(String gerritVersion) throws IOException {
    if (!gerritVersion.equals(GERRIT_VERSION)) {
      log.warn(
          "No core plugins available for version {} which is different than "
              + "the current running Gerrit",
          gerritVersion);
      return Collections.emptyList();
    }

    final Path gerritWarPath = site.gerrit_war;
    if (gerritWarPath == null) {
      log.warn("Core plugins not available on non-war Gerrit distributions");
      return Collections.emptyList();
    }

    try (JarFile gerritWar = new JarFile(gerritWarPath.toFile())) {

      return FluentIterable.from(Collections.list(gerritWar.entries()))
          .filter(new SelectPluginsFromJar())
          .transform(new ExtractPluginInfoFromJarEntry(gerritWarPath.toString()))
          .filter(
              new Predicate<PluginInfo>() {
                @Override
                public boolean apply(PluginInfo pluginInfo) {
                  return pluginInfo != null;
                }
              })
          .toSortedList(
              new Comparator<PluginInfo>() {
                @Override
                public int compare(PluginInfo a, PluginInfo b) {
                  return a.name.compareTo(b.name);
                }
              });
    }
  }
}
