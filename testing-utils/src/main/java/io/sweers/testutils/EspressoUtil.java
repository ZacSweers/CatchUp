package io.sweers.testutils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.PerformException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.espresso.util.HumanReadables;
import android.support.test.espresso.util.TreeIterables;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.view.View;
import android.widget.TextView;

import com.google.common.collect.Iterables;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static com.google.common.truth.Truth.assertThat;
import static org.hamcrest.Matchers.is;

public final class EspressoUtil {

    private EspressoUtil() {
        throw new AssertionError("No instances.");
    }

    public static Matcher<View> containsText(final CharSequence text) {
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("contains text: " + text);
            }

            @Override
            public boolean matchesSafely(TextView textView) {
                return textView.getText().toString().contains(text);
            }
        };
    }

    public static Matcher<View> doesNotContainText(final CharSequence text) {
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("does not contain text: " + text);
            }

            @Override
            public boolean matchesSafely(TextView textView) {
                return !textView.getText().toString().contains(text);
            }
        };
    }

    /**
     * Returns a matcher that matches {@link TextView} based on its text property value ignoring case. Note: View's
     * Sugar for withTextIgnoreCase(is("string")).
     *
     * @param text {@link String} with the text to match.
     * @return a {@link Matcher<View>} that matches the specified text ignoring case.
     */
    public static Matcher<View> withTextIgnoreCase(@NonNull String text) {
        return withTextIgnoreCase(is(text.toLowerCase()));
    }

    /**
     * Returns a matcher that matches {@link TextView}s based on text property value ignoring case. Note: View's
     * text property is never {@code null}. If you {@code setText(null)} it will still be "". Do not use a null matcher.
     *
     * @param stringMatcher <a href="http://hamcrest.org/JavaHamcrest/javadoc/1.3/org/hamcrest/Matcher.html">
     * <code>Matcher</code></a> of {@link String} with text to match.
     * @return a {@link Matcher<View>} that matches the specified text ignoring case.
     */
    public static Matcher<View> withTextIgnoreCase(@NonNull final Matcher<String> stringMatcher) {
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("with text ignoring case: ");
                stringMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(TextView textView) {
                return stringMatcher.matches(textView.getText().toString().toLowerCase());
            }
        };
    }

    @CheckResult
    public static ViewAction focus() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "focusing ";
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.requestFocus();
            }
        };
    }

    /**
     * Returns a matcher that matches {@link View}s that are enabled.
     */
    public static Matcher<View> isDisabled() {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("is disabled");
            }

            @Override
            public boolean matchesSafely(View view) {
                return !view.isEnabled();
            }
        };
    }

    /**
     * Gets the current foreground activity
     *
     * @return the current foreground {@link Activity}.
     */
    @NonNull
    public static Activity getCurrentActivity() {
        return getFromUIThread(new GetterFunc<Activity>() {
            @Override
            public Activity get() {
                Collection<Activity> activities
                        = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
                if (activities == null || activities.isEmpty()) {
                    throw new RuntimeException("No active activities found!");
                } else {
                    return Iterables.getOnlyElement(activities);
                }
            }
        });
    }

    /**
     * Threadsafe means of retrieving a value from logic that must be run on the UI thread.
     *
     * @param getter a {@link GetterFunc} implementation.
     * @param <T> The type of the object you're retrieving.
     * @return The retrieved object.
     */
    public static <T> T getFromUIThread(final GetterFunc<T> getter) {
        if (Thread.currentThread() != InstrumentationRegistry.getTargetContext().getMainLooper().getThread()) {
            final ParametrizedHolder<T> holder = new ParametrizedHolder<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    holder.heldObject = getFromUIThread(getter);
                }
            });
            return holder.heldObject;
        } else {
            return getter.get();
        }
    }

    public static void assertInActivity(Class<? extends Activity> activityClass) {
        Activity currentActivity = getCurrentActivity();
        assertThat(currentActivity).isNotNull();
        //noinspection ConstantConditions
        assertThat(activityClass).isInstanceOf(currentActivity.getClass());
    }

    /**
     * Presses back until a certain activity is reached.
     *
     * @param activityClass The desired activity to go back to.
     */
    public static void pressBackUntilActivity(@NonNull final Class<? extends Activity> activityClass) {
        pressBackUntilCondition(new ConditionCheck() {
            @Override
            public boolean check() {
                return getCurrentActivity().getClass().isAssignableFrom(activityClass);
            }
        });
    }

    /**
     * Presses back until we meet the specified condition.
     *
     * @param conditionCheck the condition to meet.
     */
    public static void pressBackUntilCondition(ConditionCheck conditionCheck) {
        while (!conditionCheck.check()) {
            pressBack();
        }
    }

    public static void takeScreenshot(@Nullable Activity activity) {
        if (activity == null) {
            return;
        }
        String directoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/testingscreenshots/";
        String fileName = "fail.png";

        View srcView = activity.getWindow().getDecorView().getRootView();
        srcView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(srcView.getDrawingCache());
        srcView.setDrawingCacheEnabled(false);

        OutputStream out = null;
        File dir = new File(directoryPath);
        dir.mkdirs();
        File imageFile = new File(dir, fileName);

        // java.io.madness
        try {
            boolean success = imageFile.createNewFile();
            if (success) {
                out = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                out.flush();
            }
        } catch (IOException ignored) {

        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception ignored) { }
        }
    }

    /**
     * Perform action of waiting for a specific view condition
     *
     * Based on http://stackoverflow.com/a/22563297
     */
    @CheckResult
    public static ViewAction waitForViewMatch(final Matcher<View> viewMatcher, final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for a specific view that matches <viewmatcher> during " + millis + " millis.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + millis;

                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        // found view with required ID
                        if (viewMatcher.matches(child)) {
                            return;
                        }
                    }

                    uiController.loopMainThreadForAtLeast(50);
                }
                while (System.currentTimeMillis() < endTime);

                // timeout happens
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    public static ViewAction waitForCondition(@NonNull final ConditionCheck conditionCheck,
                                              final long millis) {
        return new ViewAction() {

            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for a specific value that matches <matcher> during " + millis + " millis.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();
                final long startTime = SystemClock.elapsedRealtime();
                final long endTime = startTime + millis;

                do {
                    if (conditionCheck.check()) {
                        return;
                    }
                    uiController.loopMainThreadForAtLeast(50);
                }
                while (SystemClock.elapsedRealtime() <= endTime);

                // timeout happens
                throw new PerformException.Builder()
                        .withActionDescription(getDescription())
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    /**
     * Simple parametrized holder, useful for when you need a final reference to a generic type that you need to mutate
     * later.
     *
     * @param <T> The type to hold.
     */
    private static class ParametrizedHolder<T> {
        private T heldObject = null;
    }

    /**
     * An interface that runs logic returning an object of type {@linkplain <T>}. Similar to {@link Runnable}.
     * @param <T> The type to return.
     */
    public interface GetterFunc<T> {

        /**
         * Called to run the logic to retrieve the requested object.
         *
         * @return The requested object.
         */
        T get();
    }

    public interface ConditionCheck {
        boolean check();
    }
}
