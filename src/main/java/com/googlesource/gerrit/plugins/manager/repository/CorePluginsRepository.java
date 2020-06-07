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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Comparator.comparing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.common.Version;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class CorePluginsRepository implements PluginsRepository {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String GERRIT_VERSION = Version.getVersion();

  private final CorePluginsDescriptions pluginsDescriptions;
  private final String gerritWarUri;
  private final Path siteGerritWar;
  private static final char WINDOWS_FILE_SEPARATOR = '\\';
  private static final char UNIX_FILE_SEPARATOR = '/';

  @Inject
  public CorePluginsRepository(SitePaths site, CorePluginsDescriptions pd) {
    this(site.gerrit_war, site.gerrit_war.toString(), pd);
  }

  @VisibleForTesting
  public CorePluginsRepository(Path siteGerritWar, String gerritWar, CorePluginsDescriptions pd) {
    this.pluginsDescriptions = pd;
    final String normalizedWar = gerritWar.replace(WINDOWS_FILE_SEPARATOR, UNIX_FILE_SEPARATOR);
    this.gerritWarUri = Paths.get(normalizedWar).toUri().toString();
    this.siteGerritWar = siteGerritWar;
  }

  @Nullable
  private PluginInfo extractPluginInfoFromJarEntry(JarEntry entry) {
    try {
      Path entryName = Paths.get(entry.getName());
      URI pluginUrl = new URI("jar:" + gerritWarUri + "!/" + entry.getName());
      try (JarInputStream pluginJar = new JarInputStream(pluginUrl.toURL().openStream())) {
        return getManifestEntry(pluginJar)
            .map(
                m -> {
                  Attributes pluginAttributes = m.getMainAttributes();
                  String pluginName = pluginAttributes.getValue("Gerrit-PluginName");
                  return new PluginInfo(
                      pluginName,
                      pluginsDescriptions.get(pluginName).orElse(""),
                      pluginAttributes.getValue("Implementation-Version"),
                      "",
                      pluginUrl.toString());
                })
            .orElse(
                new PluginInfo(
                    dropSuffix(entryName.getFileName().toString(), ".jar"),
                    "",
                    "",
                    "",
                    pluginUrl.toString()));
      } catch (IOException e) {
        logger.atSevere().withCause(e).log("Unable to open plugin %s", pluginUrl);
        return null;
      }
    } catch (URISyntaxException e) {
      logger.atSevere().withCause(e).log("Invalid plugin filename");
      return null;
    }
  }

  private String dropSuffix(String string, String suffix) {
    return string.endsWith(suffix)
        ? string.substring(0, string.length() - suffix.length())
        : string;
  }

  @Nullable
  private static Optional<Manifest> getManifestEntry(JarInputStream pluginJar) throws IOException {
    for (JarEntry entry = pluginJar.getNextJarEntry();
        entry != null;
        entry = pluginJar.getNextJarEntry()) {
      if (entry.getName().equals(JarFile.MANIFEST_NAME)) {
        return Optional.of(new Manifest(pluginJar));
      }
    }
    return Optional.empty();
  }

  @Override
  public ImmutableList<PluginInfo> list(String gerritVersion) throws IOException {
    if (!gerritVersion.equals(GERRIT_VERSION)) {
      logger.atWarning().log(
          "No core plugins available for version %s which is different than "
              + "the current running Gerrit",
          gerritVersion);
      return ImmutableList.of();
    }

    if (siteGerritWar == null) {
      logger.atWarning().log("Core plugins not available in non-war Gerrit distributions");
      return ImmutableList.of();
    }

    try (JarFile gerritWar = new JarFile(siteGerritWar.toFile())) {
      return gerritWar.stream()
          .filter(e -> e.getName().startsWith("WEB-INF/plugins") && e.getName().endsWith(".jar"))
          .map(this::extractPluginInfoFromJarEntry)
          .filter(Objects::nonNull)
          .sorted(comparing(p -> p.name))
          .collect(toImmutableList());
    }
  }
}
