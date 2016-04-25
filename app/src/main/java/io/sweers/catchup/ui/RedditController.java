package io.sweers.catchup.ui;

import android.content.Intent;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
import io.sweers.catchup.model.reddit.RedditLink;
import io.sweers.catchup.model.reddit.RedditListing;
import io.sweers.catchup.model.reddit.RedditObject;
import io.sweers.catchup.model.reddit.RedditObjectDeserializer;
import io.sweers.catchup.model.reddit.RedditResponse;
import io.sweers.catchup.network.RedditService;
import io.sweers.catchup.rx.OptimisticObserver;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseController;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import static rx.schedulers.Schedulers.io;


public final class RedditController extends BaseController
    implements SwipeRefreshLayout.OnRefreshListener {

  @Inject RedditService service;
  @Inject CustomTabActivityHelper customTab;

  @Bind(R.id.refresh) SwipeRefreshLayout swipeRefreshLayout;
  @Bind(R.id.list) RecyclerView recyclerView;

  private Adapter adapter;

  public RedditController() {
    this(null);
  }

  public RedditController(Bundle args) {
    super(args);
  }

  protected Component createComponent() {
    return DaggerRedditController_Component
        .builder()
        .module(new Module())
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build();
  }

  @Override
  protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    return inflater.inflate(R.layout.controller_reddit, container, false);
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

  @Override protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    loadPosts();
  }

  private void loadPosts() {
    loadPostsObservable()
        .subscribe(new OptimisticObserver<RedditListing>("Blah") {
          @Override
          public void onNext(RedditListing redditListing) {
            adapter.setPosts((List<RedditLink>) redditListing.getChildren());
          }
        });
  }

  private Observable<RedditListing> loadPostsObservable() {
    return service.top(null, 25)
        .map(RedditResponse::getData)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe(() -> swipeRefreshLayout.setRefreshing(true))
        .doOnUnsubscribe(() -> swipeRefreshLayout.setRefreshing(false))
        .compose(this.<RedditListing>bindToLifecycle());
  }

  @Override
  public void onRefresh() {
    loadPosts();
  }

  @PerController
  @dagger.Component(
      modules = Module.class,
      dependencies = ActivityComponent.class
  )
  public interface Component {
    void inject(RedditController controller);
  }

  @dagger.Module
  public static class Module {

    @Provides
    @PerController Gson provideGson() {
      return new GsonBuilder()
          .registerTypeAdapter(RedditObject.class, new RedditObjectDeserializer())
          .create();
    }

    @Provides
    @PerController RedditService provideRedditService(final Lazy<OkHttpClient> client, Gson gson) {
      Retrofit retrofit = new Retrofit.Builder()
          .baseUrl("https://www.reddit.com/")
          .callFactory(request -> client.get().newCall(request))
          .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(io()))
          .addConverterFactory(GsonConverterFactory.create(gson))
          .build();
      return retrofit.create(RedditService.class);
    }
  }

  private class Adapter extends RecyclerView.Adapter<ViewHolder> {

    private final List<RedditLink> posts = new ArrayList<>();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_general, parent, false);
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      holder.bindView(posts.get(position));
    }

    @Override
    public int getItemCount() {
      return posts.size();
    }

    public void setPosts(List<RedditLink> newPosts) {
      posts.clear();
      posts.addAll(newPosts);
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

    private RedditLink redditLink;

    public ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    void bindView(RedditLink link) {
      this.redditLink = link;
      title.setText(link.getTitle());
      score.setText("+ " + link.getScore());
      timestamp.setText(DateUtils.getRelativeTimeSpanString(link.getCreatedUtc() * 1000, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
      author.setText("/u/" + link.getAuthor());

      if (link.getDomain() != null) {
        source.setText(link.getDomain());
      } else {
        source.setText("self");
      }

      comments.setText(Integer.toString(link.getNumComments()));
    }

    @OnClick(R.id.container) void onItemClick() {
      // TODO Check supported media types, otherwise Chrome Custom Tabs
      customTab.openCustomTab(customTab.getCustomTabIntent().build(),
          Uri.parse(redditLink.getUrl()));
    }

    @OnClick(R.id.comments) void onCommentsClick() {
      // TODO Make the app choice a pref
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://reddit.com/comments/" + redditLink.getId()));
      startActivity(intent);

      // TODO This should be how it's actually done
//      customTab.openCustomTab(customTab.getCustomTabIntent().build(),
//          Uri.parse("https://reddit.com/comments/" + redditLink.getId()));
    }
  }
}
