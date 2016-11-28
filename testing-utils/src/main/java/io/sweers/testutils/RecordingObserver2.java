package io.sweers.testutils;

import android.util.Log;
import com.google.common.truth.Platform;
import io.reactivex.disposables.Disposable;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

public final class RecordingObserver2<T> implements io.reactivex.Observer<T> {
  private static final String TAG = "RecordingObserver2";

  private final BlockingDeque<Object> events = new LinkedBlockingDeque<>();

  @Override
  public void onError(Throwable e) {
    Log.v(TAG, "onError", e);
    events.addLast(new OnError(e));
  }

  @Override
  public void onComplete() {
    Log.v(TAG, "onCompleted");
    events.addLast(new OnCompleted());
  }

  @Override
  public void onSubscribe(Disposable d) {
    Log.v(TAG, "onSubscribe");
    events.addLast(new OnSubscribe(d));
  }

  @Override
  public void onNext(T t) {
    Log.v(TAG, "onNext " + t);
    events.addLast(new OnNext(t));
  }

  private <E> E takeEvent(Class<E> wanted) {
    Object event;
    try {
      event = events.pollFirst(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (event == null) {
      throw new NoSuchElementException(
          "No event found while waiting for " + wanted.getSimpleName());
    }
    assertThat(event).isInstanceOf(wanted);
    return wanted.cast(event);
  }

  public boolean hasNextEvent() {
    Object event;
    try {
      event = events.pollFirst(0, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return event != null && Platform.isInstanceOfType(event, OnNext.class);
  }

  public T takeNext() {
    OnNext event = takeEvent(OnNext.class);
    return event.value;
  }

  public Disposable takeDisposable() {
    return takeEvent(OnSubscribe.class).disposable;
  }

  public Throwable takeError() {
    return takeEvent(OnError.class).throwable;
  }

  public void assertOnCompleted() {
    takeEvent(OnCompleted.class);
  }

  public void assertNoMoreEvents() {
    try {
      Object event = takeEvent(Object.class);
      throw new IllegalStateException("Expected no more events but got " + event);
    } catch (NoSuchElementException ignored) {
    }
  }

  private final class OnNext {
    final T value;

    private OnNext(T value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "OnNext[" + value + "]";
    }
  }

  private final class OnCompleted {
    @Override
    public String toString() {
      return "OnCompleted";
    }
  }

  private final class OnError {
    private final Throwable throwable;

    private OnError(Throwable throwable) {
      this.throwable = throwable;
    }

    @Override
    public String toString() {
      return "OnError[" + throwable + "]";
    }
  }

  private final class OnSubscribe {
    private final Disposable disposable;

    private OnSubscribe(Disposable disposable) {
      this.disposable = disposable;
    }

    @Override
    public String toString() {
      return "OnSubscribe[" + disposable + "]";
    }
  }
}
