// Copyright (C) 2023 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.manager.gson;

import static com.google.common.truth.OptionalSubject.optionals;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.truth.OptionalSubject;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class SmartJsonTest {

  private Gson gson;

  @Before
  public void setup() {
    gson = new Gson();
  }

  @Test
  public void shouldReturnEmptyForInexistentField() {
    Optional<String> fooField = parseAsSmartJson("{}").getOptionalString("foofield");
    assertThatOptional(fooField).isEmpty();
  }

  @Test
  public void shouldReturnPresentValueForField() {
    String fieldValue = "foovalue";
    Optional<String> fooField =
        parseAsSmartJson(String.format("{ \"foofield\": \"%s\"}", fieldValue))
            .getOptionalString("foofield");
    assertThatOptional(fooField).hasValue(fieldValue);
  }

  @Test
  public void shouldReturnEmptyForFieldWithNullValue() {
    Optional<String> fooField =
        parseAsSmartJson("{ \"foofield\": null }").getOptionalString("foofield");
    assertThatOptional(fooField).isEmpty();
  }

  private OptionalSubject assertThatOptional(Optional<String> field) {
    return assertWithMessage("Optional<field>").about(optionals()).that(field);
  }

  private SmartJson parseAsSmartJson(String jsonAsString) {
    return SmartJson.of(gson.fromJson(jsonAsString, JsonElement.class));
  }
}
