package io.sweers.catchup.ui.base;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.reactivex.ObservableTransformer;
import io.sweers.catchup.R;
import io.sweers.catchup.data.LinkManager.UrlMeta;
import io.sweers.catchup.injection.ConductorInjection;
import io.sweers.catchup.util.UiUtil;

/**
 * Controller base for different services.
 */
public abstract class ServiceController extends ButterKnifeController {

  @ColorInt private int serviceThemeColor = Color.BLACK;

  public ServiceController() {
    super();
  }

  public ServiceController(Bundle args) {
    super(args);
  }

  @NonNull @Override
  protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    View view = super.onCreateView(inflater, container);
    Context themedContext = view.getContext();
    if (container.getContext() != themedContext) {
      serviceThemeColor = UiUtil.resolveAttribute(themedContext, R.attr.colorAccent);
    }
    return view;
  }

  @Override protected void onAttach(@NonNull View view) {
    ConductorInjection.inject(this);
    super.onAttach(view);
  }

  protected <T> ObservableTransformer<T, UrlMeta> transformUrlToMeta(@Nullable final String url) {
    return upstream -> upstream.map(o -> new UrlMeta(url, getServiceThemeColor(), getActivity()));
  }

  @ColorInt public int getServiceThemeColor() {
    return serviceThemeColor;
  }
}
