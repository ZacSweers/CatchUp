package io.sweers.catchup.ui.base;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.bluelinelabs.conductor.Controller;
import io.sweers.catchup.R;
import io.sweers.catchup.util.UiUtil;

public abstract class ButterKnifeController extends Controller {

  @ColorInt private int serviceThemeColor = Color.BLACK;
  private Unbinder unbinder;

  protected ButterKnifeController() {
  }

  protected ButterKnifeController(Bundle args) {
    super(args);
  }

  protected abstract View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container);

  /**
   * Callback for wrapping context. Override for your own theme.
   */
  protected Context onThemeContext(@NonNull Context context) {
    return context;
  }

  @ColorInt
  protected int getServiceThemeColor() {
    return serviceThemeColor;
  }

  @NonNull
  @Override
  protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    Context parentContext = container.getContext();
    Context themedContext = onThemeContext(parentContext);

    if (parentContext != themedContext) {
      serviceThemeColor = UiUtil.resolveAttribute(themedContext, R.attr.colorAccent);
    }

    View view = inflateView(LayoutInflater.from(themedContext), container);
    unbinder = ButterKnife.bind(this, view);
    onViewBound(view);
    return view;
  }

  protected void onViewBound(@NonNull View view) {
  }

  @Override
  protected void onDestroyView(View view) {
    if (unbinder != null) {
      unbinder.unbind();
    }
    super.onDestroyView(view);
  }

}
