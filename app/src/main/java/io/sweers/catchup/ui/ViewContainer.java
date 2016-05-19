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
