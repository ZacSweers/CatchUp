package io.sweers.catchup.injection;

import io.sweers.catchup.data.DataModule;
import io.sweers.catchup.data.DebugDataModule;

public final class Modules {

  public static DataModule dataModule() {
    return new DebugDataModule();
  }

  private Modules() {
    throw new InstantiationError("No instances.");
  }
}
