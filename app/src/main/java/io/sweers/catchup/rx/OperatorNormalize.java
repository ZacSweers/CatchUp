package io.sweers.catchup.rx;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

/**
 * An operator that normalizes the flow of data through an {@link Observable} stream in such a way
 * that it will only emit a maximum of once per specified time window. If the previous emission was
 * longer before than the specified window, it will emit immediately.
 * <p>
 * This is not a solution for backpressure and I do not know if it supports it. The intended usage
 * is to effectively normalize a flow that you don't want emitting too quickly, such as when dealing
 * with APIs that have rate limiting.
 *
 * @param <T> the data stream type
 */
public class OperatorNormalize<T> implements Observable.Operator<T, T> {

  final long window;
  final Scheduler scheduler;
  final Deque<T> queue = new ConcurrentLinkedDeque<>();

  public OperatorNormalize(long window, TimeUnit unit, Scheduler scheduler) {
    this.window = unit.toMillis(window);
    this.scheduler = scheduler;
  }

  @Override
  public Subscriber<? super T> call(final Subscriber<? super T> child) {
    final Scheduler.Worker worker = scheduler.createWorker();
    child.add(worker);
    return new Subscriber<T>(child) {

      boolean done;
      // The beginning time is the time when the observer subscribes.
      private volatile long lastTimestamp = scheduler.now();
      private volatile long nextTimestamp = lastTimestamp;
      private AtomicBoolean isDraining = new AtomicBoolean();

      private boolean completionPending = false;
      private Throwable pendingError = null;

      @Override
      public void onCompleted() {
        if (!done) {
          done = true;
          completionPending = true;
        }
        if (queue.isEmpty() && !isDraining.get()) {
          child.onCompleted();
        }
      }

      @Override
      public void onError(final Throwable e) {
        if (!done) {
          done = true;
          pendingError = e;
        }
        if (queue.isEmpty() && !isDraining.get()) {
          child.onError(e);
          worker.unsubscribe();
        }
      }

      @Override
      public void onNext(final T t) {
        long timeNow = scheduler.now();
        if (queue.isEmpty() && timeNow >= nextTimestamp) {
          // Can emit immediately
          child.onNext(t);
          lastTimestamp = timeNow;
          nextTimestamp = lastTimestamp + window;
        } else {
          // Start a drain
          // Any future items will be added to the queue here too
          queue.add(t);
          if (!isDraining.getAndSet(true)) {
            drain();
          }
        }
      }

      private void finishDrain() {
        isDraining.set(false);
        if (done) {
          if (completionPending) {
            onCompleted();
          } else {
            onError(pendingError);
          }
        }
      }

      private synchronized void drain() {
        worker.schedule(() -> {
          child.onNext(queue.remove());
          lastTimestamp = scheduler.now();
          nextTimestamp = lastTimestamp + window;
          if (queue.isEmpty()) {
            finishDrain();
          } else {
            drain();
          }
        }, zeroIfNegative(nextTimestamp - scheduler.now()), TimeUnit.MILLISECONDS);
      }

      private long zeroIfNegative(long l) {
        return l < 0 ? 0 : l;
      }
    };
  }
}
