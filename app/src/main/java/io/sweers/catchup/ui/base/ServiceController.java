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

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import io.reactivex.ObservableTransformer;
import io.sweers.catchup.R;
import io.sweers.catchup.data.LinkManager.UrlMeta;
import io.sweers.catchup.injection.ConductorInjection;
import io.sweers.catchup.util.UiUtil;

/**
 * Controller base for different services.
 */
public abstract class ServiceController extends ButterKnifeController {

  protected static final int TYPE_ITEM = 0;
  protected static final int TYPE_LOADING_MORE = -1;
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

  @Override protected void onViewBound(@NonNull View view) {
    ConductorInjection.inject(this);
    super.onViewBound(view);
  }

  protected <T> ObservableTransformer<T, UrlMeta> transformUrlToMeta(@Nullable final String url) {
    return upstream -> upstream.map(o -> new UrlMeta(url, getServiceThemeColor(), getActivity()));
  }

  @ColorInt public int getServiceThemeColor() {
    return serviceThemeColor;
  }

  public static class LoadingMoreHolder extends RecyclerView.ViewHolder {

    public final ProgressBar progress;

    public LoadingMoreHolder(View itemView) {
      super(itemView);
      progress = (ProgressBar) itemView;
    }
  }
}
