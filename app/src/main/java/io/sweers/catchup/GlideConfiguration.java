package io.sweers.catchup;

import android.app.ActivityManager;
import android.content.Context;
import android.support.v4.app.ActivityManagerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.module.GlideModule;

/**
 * Configure Glide to set desired image quality.
 */
public final class GlideConfiguration implements GlideModule {

  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    // Prefer higher quality images unless we're on a low RAM device
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    builder.setDecodeFormat(
        ActivityManagerCompat.isLowRamDevice(activityManager)
            ? DecodeFormat.PREFER_RGB_565
            : DecodeFormat.PREFER_ARGB_8888
    );
  }

  @Override
  public void registerComponents(Context context, Glide glide) {
  }
}
