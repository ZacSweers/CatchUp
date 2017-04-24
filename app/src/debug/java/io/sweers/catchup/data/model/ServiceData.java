/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.data.model;

import java.util.HashSet;
import java.util.Set;

public class ServiceData {
  public final String assetsPrefix;
  public final String fileType;
  public final Set<String> supportedEndpoints;

  private ServiceData(String assetsPrefix, String fileType, Set<String> supportedEndpoints) {
    this.assetsPrefix = assetsPrefix;
    this.fileType = fileType;
    this.supportedEndpoints = supportedEndpoints;
  }

  public boolean supports(String path) {
    return supportedEndpoints.contains(path);
  }

  public static class Builder {
    private final String prefix;
    private final Set<String> endpoints = new HashSet<>();
    private String fileType = "json"; // Default

    public Builder(String prefix) {
      this.prefix = prefix;
    }

    public Builder addEndpoint(String endpoint) {
      endpoints.add(endpoint);
      return this;
    }

    public Builder fileType(String fileType) {
      this.fileType = fileType;
      return this;
    }

    public ServiceData build() {
      return new ServiceData(prefix, fileType, endpoints);
    }
  }
}
