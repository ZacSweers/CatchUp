package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.ContextThemeWrapper;
import com.bluelinelabs.conductor.Controller;
import com.serjltt.moshi.adapters.WrappedJsonAdapter;
import com.squareup.moshi.Moshi;
import com.uber.autodispose.CompletableScoper;
import dagger.Binds;
import dagger.Lazy;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;
import io.reactivex.Single;
import io.sweers.catchup.R;
import io.sweers.catchup.data.ISO8601InstantAdapter;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.designernews.DesignerNewsService;
import io.sweers.catchup.data.designernews.model.Story;
import io.sweers.catchup.data.designernews.model.User;
import io.sweers.catchup.injection.ControllerKey;
import io.sweers.catchup.ui.base.BaseNewsController;
import io.sweers.catchup.ui.base.HasStableId;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Qualifier;
import okhttp3.OkHttpClient;
import org.threeten.bp.Instant;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public final class DesignerNewsController extends BaseNewsController<Story> {

  @Inject DesignerNewsService service;
  @Inject LinkManager linkManager;

  public DesignerNewsController() {
    super();
  }

  public DesignerNewsController(Bundle args) {
    super(args);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_DesignerNews);
  }

  @Override protected void bindItemView(@NonNull Story story, @NonNull ViewHolder holder) {
    //Story story = storyMeta.story;
    //User user = storyMeta.user;
    holder.title(story.title());

    holder.score(Pair.create("▲", story.voteCount()));
    holder.timestamp(story.createdAt());
    //holder.author(user.displayName());
    holder.author(null);

    holder.source(story.hostname());

    holder.comments(story.commentCount());
    holder.tag(story.badge());

    holder.itemClicks()
        .compose(transformUrlToMeta(story.url()))
        .flatMapCompletable(linkManager)
        .to(new CompletableScoper(holder))
        .subscribe();
    holder.itemCommentClicks()
        .compose(transformUrlToMeta(story.href()
            .replace("api.", "www.")
            .replace("api/v2/", "")))
        .flatMapCompletable(linkManager)
        .to(new CompletableScoper(holder))
        .subscribe();
  }

  @NonNull @Override protected Single<List<Story>> getDataSingle() {
    return service.getTopStories();
    // This won't do for now because /users endpoint sporadically barfs on specific user IDs
    //return service.getTopStories()
    //    .flatMap(stories -> Observable.zip(
    //        Observable.fromIterable(stories),
    //        Observable.fromIterable(stories)
    //            .map(story -> story.links()
    //                .user())
    //            .toList()
    //            .flatMap(ids -> service.getUsers(CommaJoinerList.from(ids)))
    //            .flattenAsObservable(users -> users),
    //        StoryAndUserHolder::new)
    //        .toList());
  }

  @Subcomponent
  public interface Component extends AndroidInjector<DesignerNewsController> {

    @Subcomponent.Builder
    abstract class Builder extends AndroidInjector.Builder<DesignerNewsController> {}
  }

  @dagger.Module(subcomponents = Component.class)
  public abstract static class Module {

    @Qualifier
    private @interface InternalApi {}

    @Binds @IntoMap @ControllerKey(DesignerNewsController.class)
    abstract AndroidInjector.Factory<? extends Controller> bindDesignerNewsControllerInjectorFactory(
        Component.Builder builder);

    @Provides @InternalApi static Moshi provideDesignerNewsMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Instant.class, new ISO8601InstantAdapter())
          .add(WrappedJsonAdapter.FACTORY)
          .build();
    }

    @Provides static DesignerNewsService provideDesignerNewsService(final Lazy<OkHttpClient> client,
        @InternalApi Moshi moshi,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
      Retrofit retrofit = new Retrofit.Builder().baseUrl(DesignerNewsService.ENDPOINT)
          .callFactory(request -> client.get()
              .newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build();
      return retrofit.create(DesignerNewsService.class);
    }
  }

  // TODO Eventually use this when we can safely query User ids
  static class StoryAndUserHolder implements HasStableId {

    final Story story;
    final User user;

    public StoryAndUserHolder(Story story, User user) {
      this.story = story;
      this.user = user;
    }

    @Override public long stableId() {
      return story.stableId();
    }
  }
}
