package io.sweers.catchup.data.github.model;

public enum Order {
  ASC, DESC;

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
