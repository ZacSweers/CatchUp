package io.sweers.catchup.ui.base;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.Unbinder;

public abstract class ButterKnifeController extends AutoDisposeController {

  private Unbinder unbinder;

  protected ButterKnifeController() {
    super();
  }

  protected ButterKnifeController(Bundle args) {
    super(args);
  }

  protected abstract View inflateView(@NonNull LayoutInflater inflater,
      @NonNull ViewGroup container);

  /**
   * Callback for wrapping context. Override for your own theme.
   */
  protected Context onThemeContext(@NonNull Context context) {
    return context;
  }

  @NonNull @Override
  protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    Context themedContext = onThemeContext(container.getContext());
    View view = inflateView(LayoutInflater.from(themedContext), container);
    unbinder = bind(view);
    onViewBound(view);
    return view;
  }

  protected abstract Unbinder bind(@NonNull View view);

  protected void onViewBound(@NonNull View view) {
  }

  @Override protected void onDestroyView(@NonNull View view) {
    if (unbinder != null) {
      unbinder.unbind();
    }
    super.onDestroyView(view);
  }
}
