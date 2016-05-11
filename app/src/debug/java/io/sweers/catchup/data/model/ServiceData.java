package io.sweers.catchup.data.model;

import java.util.HashSet;
import java.util.Set;

public class ServiceData {
  public final String assetsPrefix;
  public final Set<String> supportedEndpoints;

  private ServiceData(String assetsPrefix, Set<String> supportedEndpoints) {
    this.assetsPrefix = assetsPrefix;
    this.supportedEndpoints = supportedEndpoints;
  }

  public boolean supports(String path) {
    return supportedEndpoints.contains(path);
  }

  public static class Builder {
    private final String prefix;
    private final Set<String> endpoints = new HashSet<>();

    public Builder(String prefix) {
      this.prefix = prefix;
    }

    public Builder addEndpoint(String endpoint) {
      endpoints.add(endpoint);
      return this;
    }

    public ServiceData build() {
      return new ServiceData(prefix, endpoints);
    }
  }
}
