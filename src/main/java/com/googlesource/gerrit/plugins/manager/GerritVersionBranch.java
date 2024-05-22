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

import com.google.common.base.Splitter;
import java.util.List;

public class GerritVersionBranch {
  private static final Splitter VERSION_SPLITTER = Splitter.on(".");
  private static final Splitter MINOR_VERSION_SPLITTER = Splitter.on("-");

  public static String getBranch(String gerritVersion) {
    if (gerritVersion == null
        || gerritVersion.trim().isEmpty()
        || !Character.isDigit(gerritVersion.trim().charAt(0))) {
      return "master";
    }
    List<String> versionNumbers = VERSION_SPLITTER.splitToList(gerritVersion);
    String major = versionNumbers.get(0);
    String minor = versionNumbers.get(1);

    if (minor.contains("-")) {
      minor = MINOR_VERSION_SPLITTER.splitToList(minor).get(0);
    }

    if (versionNumbers.size() > 2) {
      String fixVersionNumber = versionNumbers.get(2);
      if (fixVersionNumber.contains("-") && !fixVersionNumber.contains("-rc") && !fixVersionNumber.contains("-RP-")) {
        return "master";
      }
    }

    return "stable-" + major + "." + minor;
  }
}
