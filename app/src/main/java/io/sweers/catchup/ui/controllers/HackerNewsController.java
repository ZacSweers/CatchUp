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

package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import com.bluelinelabs.conductor.Controller;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.uber.autodispose.CompletableScoper;
import com.uber.autodispose.ObservableScoper;
import dagger.Binds;
import dagger.Lazy;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.sweers.catchup.R;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.hackernews.model.HackerNewsStory;
import io.sweers.catchup.injection.ControllerKey;
import io.sweers.catchup.ui.base.BaseNewsController;
import java.util.List;
import javax.inject.Inject;
import okhttp3.HttpUrl;
import timber.log.Timber;

import static io.sweers.catchup.data.RemoteConfigKeys.SMMRY_ENABLED;

public final class HackerNewsController extends BaseNewsController<HackerNewsStory> {

  @Inject LinkManager linkManager;
  @Inject FirebaseRemoteConfig remoteConfig;
  @Inject Lazy<FirebaseDatabase> database;

  public HackerNewsController() {
    super();
  }

  public HackerNewsController(Bundle args) {
    super(args);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_HackerNews);
  }

  @Override
  protected void bindItemView(@NonNull HackerNewsStory story, @NonNull NewsItemViewHolder holder) {
    holder.title(story.title());
    holder.score(Pair.create("+", story.score()));
    holder.timestamp(story.time());
    holder.author(story.by());

    String url = story.url();
    if (url == null) {
      holder.source(null);
    } else {
      holder.source(HttpUrl.parse(url)
          .host());
    }

    int commentsCount = 0;
    // TODO Adapter to coerce this to Collections.emptyList()?
    List<Long> kids = story.kids();
    if (kids != null) {
      commentsCount = kids.size();
    }
    holder.comments(commentsCount);
    holder.tag(null);

    if (remoteConfig.getBoolean(SMMRY_ENABLED) && !TextUtils.isEmpty(url)) {
      holder.itemLongClicks()
          .to(new ObservableScoper<>(holder))
          .subscribe(SmmryController.showFor(this, url));
    }

    holder.itemClicks()
        .compose(transformUrlToMeta(url))
        .flatMapCompletable(linkManager)
        .to(new CompletableScoper(holder))
        .subscribe();

    holder.itemCommentClicks()
        .compose(transformUrlToMeta("https://news.ycombinator.com/item?id=" + story.id()))
        .flatMapCompletable(linkManager)
        .to(new CompletableScoper(holder))
        .subscribe();
  }

  @NonNull @Override
  protected Single<List<HackerNewsStory>> getDataSingle(DataRequest request) {
    int itemsPerPage = 25; // TODO Pref this
    return Single.create((SingleEmitter<DataSnapshot> emitter) -> {
      ValueEventListener listener = new ValueEventListener() {
        @Override public void onDataChange(DataSnapshot dataSnapshot) {
          emitter.onSuccess(dataSnapshot);
        }

        @Override public void onCancelled(DatabaseError firebaseError) {
          Timber.d("%d", firebaseError.getCode());
          emitter.onError(firebaseError.toException());
        }
      };

      DatabaseReference ref = database.get()
          .getReference("v0/topstories");
      emitter.setCancellable(() -> ref.removeEventListener(listener));
      ref.addValueEventListener(listener);
    })
        .flattenAsObservable(DataSnapshot::getChildren)
        .skip(((request.page() + 1) * itemsPerPage) - itemsPerPage)
        .take(itemsPerPage)
        .map(d -> (Long) d.getValue())
        .concatMapEager(id -> Observable.create((ObservableOnSubscribe<DataSnapshot>) emitter -> {
          DatabaseReference ref = database.get()
              .getReference("v0/item/" + id);
          ValueEventListener listener = new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot dataSnapshot) {
              emitter.onNext(dataSnapshot);
              emitter.onComplete();
            }

            @Override public void onCancelled(DatabaseError firebaseError) {
              Timber.d("%d", firebaseError.getCode());
              emitter.onError(firebaseError.toException());
            }
          };
          emitter.setCancellable(() -> ref.removeEventListener(listener));
          ref.addValueEventListener(listener);
        }))
        .map(HackerNewsStory::create)
        .toList();
  }

  @Subcomponent
  public interface Component extends AndroidInjector<HackerNewsController> {

    @Subcomponent.Builder
    abstract class Builder extends AndroidInjector.Builder<HackerNewsController> {}
  }

  @dagger.Module(subcomponents = Component.class)
  public abstract static class Module {

    @Binds @IntoMap @ControllerKey(HackerNewsController.class)
    abstract AndroidInjector.Factory<? extends Controller> bindHackerNewsControllerInjectorFactory(
        Component.Builder builder);

    @Provides static FirebaseDatabase provideDataBase() {
      return FirebaseDatabase.getInstance("https://hacker-news.firebaseio.com/");
    }
  }
}
