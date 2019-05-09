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

package com.googlesource.gerrit.plugins.manager;

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.manager.GerritVersionBranch.getBranch;

import org.junit.Test;

public class GerritVersionBranchTest {

  @Test
  public void getBranchReturnsCorrectBranchForTwoDigitsVersions() throws Exception {
    // Regular 2.x versions
    assertBranch("2.13", "stable-2.13");
    assertBranch("2.14", "stable-2.14");
    assertBranch("2.15", "stable-2.15");
    assertBranch("2.16", "stable-2.16");
  }

  @Test
  public void getBranchReturnsCorrectBranchForThreeDigitsVersions() throws Exception {
    // 2.x.y version
    assertBranch("2.16.10", "stable-2.16");

    // 3.0.0 version
    assertBranch("3.0.0", "stable-3.0");
  }

  @Test
  public void getBranchReturnsCorrectBranchForReleaseCandidates() throws Exception {
    // 2.x-rcx version
    assertBranch("2.16-rc1", "stable-2.16");

    // 3.0.0-rcx version
    assertBranch("3.0.0-rc3", "stable-3.0");
  }

  @Test
  public void getBranchReturnsCorrectBranchForDevelopmentOnStableBranches() throws Exception {
    assertBranch("2.16.8-17-gc8b633d5ce", "stable-2.16");
  }

  @Test
  public void getBranchReturnsCorrectBranchForDevelopmentOnMaster() throws Exception {
    assertBranch("3.0.0-rc2-237-gae0124c68e", "master");
  }

  @Test
  public void getBranchReturnsMasterForUnknownVersions() throws Exception {
    // Unknown versions
    assertBranch(null, "master");
    assertBranch("", "master");
    assertBranch("   ", "master");
    assertBranch("foo", "master");
  }

  private static void assertBranch(String version, String expectedBranch) throws Exception {
    assertThat(getBranch(version)).isEqualTo(expectedBranch);
  }
}
