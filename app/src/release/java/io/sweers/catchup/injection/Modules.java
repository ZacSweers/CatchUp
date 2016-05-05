package io.sweers.catchup.injection;

import io.sweers.catchup.data.DataModule;

public final class Modules {

  public static DataModule dataModule() {
    return new DataModule();
  }

  private Modules() {
    throw new InstantiationError("No instances.");
  }
}
