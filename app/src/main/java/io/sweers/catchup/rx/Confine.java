package io.sweers.catchup.rx;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.view.View;

import com.bluelinelabs.conductor.rxlifecycle.RxController;
import com.bluelinelabs.conductor.rxlifecycle.RxControllerLifecycle;
import com.trello.rxlifecycle.LifecycleTransformer;
import com.trello.rxlifecycle.android.RxLifecycleAndroid;

import org.reactivestreams.Publisher;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.CompletableTransformer;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.MaybeTransformer;
import io.reactivex.SingleTransformer;
import io.sweers.catchup.ui.base.BaseActivity;
import rx.Completable;
import rx.Observable;
import rx.Single;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV1Completable;
import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV1Observable;
import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV1Single;

public final class Confine {

  private Confine() {
    throw new InstantiationError();
  }

  @NonNull
  @CheckResult
  public static <T> LifecycleTransformer2<T> to(@NonNull BaseActivity activity) {
    return LifecycleTransformer2.create(RxLifecycleAndroid.bindActivity(activity.lifecycle()));
  }

  @NonNull
  @CheckResult
  public static <T> LifecycleTransformer2<T> to(@NonNull RxController controller) {
    return LifecycleTransformer2.create(
        RxControllerLifecycle.bindController(controller.lifecycle()));
  }

  @NonNull
  @CheckResult
  public static <T> LifecycleTransformer2<T> to(@NonNull View view) {
    return LifecycleTransformer2.create(RxLifecycleAndroid.bindView(view));
  }

  public static class LifecycleTransformer2<T> implements FlowableTransformer<T, T> {

    private LifecycleTransformer<T> delegate;

    private LifecycleTransformer2(@NonNull LifecycleTransformer<T> delegate) {
      this.delegate = delegate;
    }

    public static <T> LifecycleTransformer2<T> create(@NonNull LifecycleTransformer<T> delegate) {
      return new LifecycleTransformer2<>(delegate);
    }

    public <U> MaybeTransformer<U, U> forMaybe() {
      return source -> {
        Observable<U> o = toV1Observable(source.toFlowable());
        o = (Observable<U>) delegate.<U>call((Observable<T>) o);
        return RxJavaInterop.toV2Flowable(o).singleElement();
      };
    }

    public <U> SingleTransformer<U, U> forSingle() {
      return source1 -> {
        Single<U> o = toV1Single(source1);
        o = delegate.<U>forSingle().call(o);
        return RxJavaInterop.toV2Single(o);
      };
    }

    public CompletableTransformer forCompletable() {
      return source -> {
        Completable o = toV1Completable(source);
        o = delegate.forCompletable().call(o);
        return RxJavaInterop.toV2Completable(o);
      };
    }

    @Override
    public Publisher<T> apply(Flowable<T> source) {
      Observable<T> o = toV1Observable(source);
      o = delegate.call(o);
      return RxJavaInterop.toV2Flowable(o);
    }
  }
}
