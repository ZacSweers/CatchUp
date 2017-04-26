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

package io.sweers.catchup.rx;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.sweers.catchup.RecordingObserver;
import rx.Subscription;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

import static com.google.common.truth.Truth.assertThat;

public class OperatorNormalizeTest {

  @Test
  public void basic() {
    TestScheduler scheduler = new TestScheduler();
    PublishSubject<Integer> subject = PublishSubject.create();
    RecordingObserver<Integer> o = new RecordingObserver<>();
    subject
        .lift(new OperatorNormalize<>(1, TimeUnit.SECONDS, scheduler))
        .subscribe(o);

    subject.onNext(0);
    o.takeNext();

    subject.onNext(0);
    o.assertNoMoreEvents();

    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
    o.takeNext();

    subject.onCompleted();
    o.assertOnCompleted();
  }

  @Test
  public void buffer() {
    TestScheduler scheduler = new TestScheduler();
    PublishSubject<Integer> subject = PublishSubject.create();
    RecordingObserver<Integer> o = new RecordingObserver<>();
    subject
        .lift(new OperatorNormalize<>(1, TimeUnit.SECONDS, scheduler))
        .subscribe(o);

    // First emits immediately
    subject.onNext(0);
    o.takeNext();

    subject.onNext(0);
    subject.onNext(0);
    subject.onNext(0);
    o.assertNoMoreEvents();

    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
    o.takeNext();
    o.assertNoMoreEvents();
    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
    o.takeNext();
    o.assertNoMoreEvents();
    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
    o.takeNext();
    o.assertNoMoreEvents();

    subject.onCompleted();
    o.assertOnCompleted();
  }

  @Test
  public void completion() {
    TestScheduler scheduler = new TestScheduler();
    PublishSubject<Integer> subject = PublishSubject.create();
    RecordingObserver<Integer> o = new RecordingObserver<>();
    subject
        .lift(new OperatorNormalize<>(1, TimeUnit.SECONDS, scheduler))
        .subscribe(o);

    // First emits immediately
    subject.onNext(0);
    o.takeNext();

    subject.onNext(0);
    subject.onCompleted();
    o.assertNoMoreEvents();

    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
    o.takeNext();
    o.assertOnCompleted();
  }

  @Test
  public void error() {
    TestScheduler scheduler = new TestScheduler();
    PublishSubject<Integer> subject = PublishSubject.create();
    RecordingObserver<Integer> o = new RecordingObserver<>();
    subject
        .lift(new OperatorNormalize<>(1, TimeUnit.SECONDS, scheduler))
        .subscribe(o);

    // First emits immediately
    subject.onNext(0);
    o.takeNext();

    subject.onNext(0);
    subject.onError(new RuntimeException("Blah"));
    o.assertNoMoreEvents();

    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
    o.takeNext();
    assertThat(o.takeError()).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void unsubscription() {
    TestScheduler scheduler = new TestScheduler();
    PublishSubject<Integer> subject = PublishSubject.create();
    RecordingObserver<Integer> o = new RecordingObserver<>();
    Subscription sub = subject
        .lift(new OperatorNormalize<>(1, TimeUnit.SECONDS, scheduler))
        .subscribe(o);

    // First emits immediately
    subject.onNext(0);
    o.takeNext();

    subject.onNext(0);
    o.assertNoMoreEvents();

    sub.unsubscribe();

    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
    o.assertNoMoreEvents();
  }

  @Test
  public void overDelay_shouldEmitImmediately() {
    TestScheduler scheduler = new TestScheduler();
    PublishSubject<Integer> subject = PublishSubject.create();
    RecordingObserver<Integer> o = new RecordingObserver<>();
    subject
        .lift(new OperatorNormalize<>(1, TimeUnit.SECONDS, scheduler))
        .subscribe(o);

    // First emits immediately
    subject.onNext(0);
    o.takeNext();

    scheduler.advanceTimeBy(2, TimeUnit.SECONDS);
    subject.onNext(0);
    o.takeNext();
  }

  @Test
  public void overDelay_withMultiple_emitsFirstAndStartsDrain() {
    TestScheduler scheduler = new TestScheduler();
    PublishSubject<Integer> subject = PublishSubject.create();
    RecordingObserver<Integer> o = new RecordingObserver<>();
    subject
        .lift(new OperatorNormalize<>(1, TimeUnit.SECONDS, scheduler))
        .subscribe(o);

    // First emits immediately
    subject.onNext(0);
    o.takeNext();

    scheduler.advanceTimeBy(2, TimeUnit.SECONDS);
    subject.onNext(0);
    subject.onNext(0);
    subject.onNext(0);

    // Only one emitted
    o.takeNext();
    o.assertNoMoreEvents();

    // Drain the rest
    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
    o.takeNext();
    o.assertNoMoreEvents();

    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
    o.takeNext();
    o.assertNoMoreEvents();
  }
}
