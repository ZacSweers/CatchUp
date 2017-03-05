package io.sweers.catchup.injection;

import android.app.Activity;
import android.util.Log;
import com.bluelinelabs.conductor.Controller;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;

import static dagger.internal.Preconditions.checkNotNull;

/** Injects core Conductor types. */
public class ConductorInjection {
  private static final String TAG = "io.sweers.catchup.ConductorInjection";

  /**
   * Injects {@code controller} if an associated {@link AndroidInjector.Factory} implementation can be
   * found, otherwise throws an {@link IllegalArgumentException}.
   *
   * <p>Uses the following algorithm to find the appropriate {@code
   * DispatchingAndroidInjector<Controller>} to inject {@code controller}:
   *
   * <ol>
   * <li>Walks the parent-controller hierarchy to find the a controller that implements {@link
   * HasDispatchingControllerInjector}, and if none do
   * <li>Uses the {@code controller}'s {@link Controller#getActivity() activity} if it implements
   * {@link HasDispatchingControllerInjector}, and if not
   * <li>Uses the {@link android.app.Application} if it implements {@link
   * HasDispatchingControllerInjector}.
   * </ol>
   *
   * If none of them implement {@link HasDispatchingControllerInjector}, a {@link
   * IllegalArgumentException} is thrown.
   *
   * @throws IllegalArgumentException if no {@code AndroidInjector.Factory<Controller, ?>} is bound
   * for {@code controller}.
   */
  public static void inject(Controller controller) {
    checkNotNull(controller, "controller");
    HasDispatchingControllerInjector hasDispatchingControllerInjector =
        findHasControllerInjector(controller);
    Log.d(TAG, String.format(
        "An injector for %s was found in %s",
        controller.getClass()
            .getCanonicalName(),
        hasDispatchingControllerInjector.getClass()
            .getCanonicalName()));

    DispatchingAndroidInjector<Controller> controllerInjector =
        hasDispatchingControllerInjector.controllerInjector();
    checkNotNull(
        controllerInjector,
        "%s.controllerInjector() returned null",
        hasDispatchingControllerInjector.getClass()
            .getCanonicalName());

    controllerInjector.inject(controller);
  }

  private static HasDispatchingControllerInjector findHasControllerInjector(Controller controller) {
    Controller parentController = controller;
    while ((parentController = parentController.getParentController()) != null) {
      if (parentController instanceof HasDispatchingControllerInjector) {
        return (HasDispatchingControllerInjector) parentController;
      }
    }
    Activity activity = controller.getActivity();
    if (activity instanceof HasDispatchingControllerInjector) {
      return (HasDispatchingControllerInjector) activity;
    }
    if (activity.getApplication() instanceof HasDispatchingControllerInjector) {
      return (HasDispatchingControllerInjector) activity.getApplication();
    }
    throw new IllegalArgumentException(String.format(
        "No injector was found for %s",
        controller.getClass()
            .getCanonicalName()));
  }
}
