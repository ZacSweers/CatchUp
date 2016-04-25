package io.sweers.catchup.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.moshi.Moshi;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.Lazy;
import dagger.Provides;
import io.sweers.catchup.R;
import io.sweers.catchup.injection.PerController;
import io.sweers.catchup.model.HackerNewsStory;
import io.sweers.catchup.network.HackerNewsService;
import io.sweers.catchup.rx.OptimisticObserver;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseController;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import static rx.schedulers.Schedulers.io;


public final class HackerNewsController extends BaseController
    implements SwipeRefreshLayout.OnRefreshListener {

  @Inject HackerNewsService service;
  @Inject CustomTabActivityHelper customTab;

  @Bind(R.id.refresh) SwipeRefreshLayout swipeRefreshLayout;
  @Bind(R.id.list) RecyclerView recyclerView;

  private Adapter adapter;

  public HackerNewsController() {
    this(null);
  }

  public HackerNewsController(Bundle args) {
    super(args);
  }

  protected Component createComponent() {
    return DaggerHackerNewsController_Component
        .builder()
        .module(new Module())
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build();
  }

  @Override
  protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    return inflater.inflate(R.layout.controller_hacker_news, container, false);
  }

  @Override protected void onViewBound(@NonNull View view) {
    super.onViewBound(view);
    // TODO There must be an earlier place than this
    createComponent().inject(this);

    LinearLayoutManager layoutManager
        = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    recyclerView.setLayoutManager(layoutManager);
    adapter = new Adapter();
    recyclerView.setAdapter(adapter);
    swipeRefreshLayout.setOnRefreshListener(this);
  }

  @Override
  protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    loadStories();
  }

  private void loadStories() {
    loadStoriesObservable()
        .subscribe(new OptimisticObserver<List<HackerNewsStory>>("Blah") {
          @Override
          public void onNext(List<HackerNewsStory> hackerNewsStories) {
            adapter.setStories(hackerNewsStories);
          }
        });
  }

  private Observable<List<HackerNewsStory>> loadStoriesObservable() {
    return service.topStories()
        .concatMapIterable(strings -> strings)
        .take(25)
        .concatMap(id -> service.getItem(id))
        .toList()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe(() -> swipeRefreshLayout.setRefreshing(true))
        .doOnUnsubscribe(() -> swipeRefreshLayout.setRefreshing(false))
        .compose(this.<List<HackerNewsStory>>bindToLifecycle());
  }

  @Override
  public void onRefresh() {
    loadStories();
  }

  @PerController
  @dagger.Component(
      modules = Module.class,
      dependencies = ActivityComponent.class
  )
  public interface Component {
    void inject(HackerNewsController controller);
  }

  @dagger.Module
  public static class Module {

    @Provides
    @PerController
    HackerNewsService provideHackerNewsService(final Lazy<OkHttpClient> client, Moshi moshi) {
      Retrofit retrofit = new Retrofit.Builder()
          .baseUrl("https://hacker-news.firebaseio.com/v0/")
          .callFactory(request -> client.get().newCall(request))
          .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(io()))
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build();
      return retrofit.create(HackerNewsService.class);
    }
  }

  private class Adapter extends RecyclerView.Adapter<ViewHolder> {

    private final List<HackerNewsStory> stories = new ArrayList<>();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_general, parent, false);
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      holder.bindView(stories.get(position));
    }

    @Override
    public int getItemCount() {
      return stories.size();
    }

    public void setStories(List<HackerNewsStory> newStories) {
      stories.clear();
      stories.addAll(newStories);
      notifyDataSetChanged();
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {

    @Bind(R.id.title) TextView title;
    @Bind(R.id.score) TextView score;
    @Bind(R.id.timestamp) TextView timestamp;
    @Bind(R.id.author) TextView author;
    @Bind(R.id.source) TextView source;
    @Bind(R.id.comments) TextView comments;

    private HackerNewsStory story;

    public ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    void bindView(HackerNewsStory story) {
      this.story = story;
      title.setText(story.title());
      score.setText("+ " + story.score());
      timestamp.setText(DateUtils.getRelativeTimeSpanString(story.time() * 1000, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
      author.setText(story.by());
      source.setText(HttpUrl.parse(story.url()).host());

      String commentsCount;
      if (story.kids() != null) {
        commentsCount = Integer.toString(story.kids().size());
      } else {
        commentsCount = "0";
      }
      comments.setText(commentsCount);
    }

    @OnClick(R.id.container) void onItemClick() {
      // TODO Check supported media types, otherwise Chrome Custom Tabs
      customTab.openCustomTab(customTab.getCustomTabIntent().build(),
          Uri.parse(story.url()));
    }

    @OnClick(R.id.comments) void onCommentsClick() {
      // TODO Make the app choice a pref
      customTab.openCustomTab(customTab.getCustomTabIntent().build(),
          Uri.parse("https://news.ycombinator.com/item?id=" + story.id()));
    }
  }
}
