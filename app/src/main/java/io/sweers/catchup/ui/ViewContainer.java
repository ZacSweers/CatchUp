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

package io.sweers.catchup.ui;

import android.app.Activity;
import android.view.ViewGroup;

import static butterknife.ButterKnife.findById;

/**
 * An indirection which allows controlling the root container used for each activity.
 */
public interface ViewContainer {
  /**
   * An {@link ViewContainer} which returns the normal activity content view.
   */
  ViewContainer DEFAULT = activity -> findById(activity, android.R.id.content);

  /**
   * The root {@link ViewGroup} into which the activity should place its contents.
   */
  ViewGroup forActivity(Activity activity);
}
