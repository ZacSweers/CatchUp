package io.sweers.catchup.ui.base;

import android.os.Bundle;
import android.util.Pair;

import rx.Observable;

public abstract class BaseController extends RefWatchingController {

  protected BaseController() {
  }

  protected BaseController(Bundle args) {
    super(args);
  }

  protected UrlTransformer transformUrl(String url) {
    return new UrlTransformer(url);
  }

  private class UrlTransformer implements Observable.Transformer<Object, Pair<String, Integer>> {

    private final String url;

    public UrlTransformer(String url) {
      this.url = url;
    }

    @Override
    public Observable<Pair<String, Integer>> call(Observable<Object> source) {
      return source.map(o -> Pair.create(url, getServiceThemeColor()));
    }
  }
}
