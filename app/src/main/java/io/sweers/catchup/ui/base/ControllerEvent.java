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

package io.sweers.catchup.ui.base;

import com.uber.autodispose.LifecycleEndedException;
import io.reactivex.functions.Function;

/**
 * Controller lifecycle events
 */
public enum ControllerEvent {
  CREATE, CREATE_VIEW, ATTACH, DETACH, DESTROY_VIEW, DESTROY;

  static final Function<ControllerEvent, ControllerEvent> LIFECYCLE = lastEvent -> {
    switch (lastEvent) {
      case CREATE:
        return DESTROY;
      case CREATE_VIEW:
        return DESTROY_VIEW;
      case ATTACH:
        return DETACH;
      case DETACH:
        return DESTROY_VIEW;
      case DESTROY_VIEW:
        return DESTROY;
      case DESTROY:
        throw new LifecycleEndedException(
            "Cannot bind to Controller lifecycle after it's been destroyed.");
    }
    throw new UnsupportedOperationException("Binding to " + lastEvent + " not yet implemented");
  };
}
