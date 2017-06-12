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

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.google.auto.value.AutoValue;
import com.jakewharton.rxbinding2.view.RxView;
import com.uber.autodispose.SingleScoper;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiConsumer;
import io.sweers.catchup.R;
import android.support.v7.widget.RxViewHolder;
import io.sweers.catchup.ui.InfiniteScrollListener;
import io.sweers.catchup.ui.Scrollable;
import io.sweers.catchup.util.Iterables;
import io.sweers.catchup.util.NumberUtil;
import io.sweers.catchup.util.Strings;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator;
import org.threeten.bp.Instant;
import retrofit2.HttpException;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public abstract class BaseNewsController<T extends HasStableId> extends ServiceController
    implements SwipeRefreshLayout.OnRefreshListener, Scrollable, DataLoadingSubject {

  @BindView(R.id.error_container) View errorView;
  @BindView(R.id.error_message) TextView errorTextView;
  @BindView(R.id.error_image) ImageView errorImage;
  @BindView(R.id.list) RecyclerView recyclerView;
  @BindView(R.id.progress) ProgressBar progress;
  @BindView(R.id.refresh) SwipeRefreshLayout swipeRefreshLayout;

  private Adapter<T> adapter;
  private int page = 0;
  private boolean fromSaveInstanceState = false;
  private boolean moreDataAvailable = true;
  private boolean isDataLoading = false;

  public BaseNewsController() {
    super();
  }

  public BaseNewsController(Bundle args) {
    super(args);
  }

  /**
   * View binding implementation to bind the given datum to the {@code holder}.
   *
   * @param t The datum to back the view with.
   * @param holder The item ViewHolder instance.
   */
  protected abstract void bindItemView(@NonNull T t, @NonNull NewsItemViewHolder holder);

  @NonNull protected abstract Single<List<T>> getDataSingle(DataRequest request);

  @Override
  protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    return inflater.inflate(R.layout.controller_basic_news, container, false);
  }

  @Override protected Unbinder bind(@NonNull View view) {
    return new BaseNewsController_ViewBinding(this, view);
  }

  @Override protected void onViewBound(@NonNull View view) {
    super.onViewBound(view);

    swipeRefreshLayout.setColorSchemeColors(getServiceThemeColor());

    LinearLayoutManager layoutManager =
        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addOnScrollListener(new InfiniteScrollListener(layoutManager, this) {
      @Override public void onLoadMore() {
        loadData();
      }
    });
    adapter = new Adapter<>(this::bindItemView);
    recyclerView.setAdapter(adapter);
    swipeRefreshLayout.setOnRefreshListener(this);

    FadeInUpAnimator itemAnimator = new FadeInUpAnimator(new OvershootInterpolator(1f));
    itemAnimator.setAddDuration(300);
    itemAnimator.setRemoveDuration(300);

    // This blows up adding item ranges >_>
    //recyclerView.setItemAnimator(itemAnimator);
  }

  @OnClick(R.id.retry_button) void onRetry() {
    errorView.setVisibility(GONE);
    progress.setVisibility(VISIBLE);
    onRefresh();
  }

  @OnClick(R.id.error_image) void onErrorClick(ImageView imageView) {
    AnimatedVectorDrawableCompat avd = (AnimatedVectorDrawableCompat) imageView.getDrawable();
    avd.start();
  }

  @Override protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    swipeRefreshLayout.setEnabled(false);
    loadData();
  }

  @Override protected void onDetach(@NonNull View view) {
    page = 0;
    moreDataAvailable = true;
    super.onDetach(view);
  }

  @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
    // TODO Check when these are called in conductor, restore seems to be after attach.
    //outState.putInt("pageNumber", page);
    //outState.putBoolean("savedInstance", true);
    super.onSaveInstanceState(outState);
  }

  @Override protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    //page = savedInstanceState.getInt("pageNumber");
    //fromSaveInstanceState = savedInstanceState.getBoolean("savedInstance", false);
  }

  @Override public boolean isDataLoading() {
    return isDataLoading;
  }

  protected void setMoreDataAvailable(boolean moreDataAvailable) {
    this.moreDataAvailable = moreDataAvailable;
  }

  private void loadData() {
    loadData(false);
  }

  private void loadData(final boolean fromRefresh) {
    if (fromRefresh) {
      moreDataAvailable = true;
      page = 0;
    }
    if (!moreDataAvailable) {
      return;
    }
    final int pageToRequest = page++;
    isDataLoading = true;
    if (adapter.getItemCount() != 0) {
      recyclerView.post(() -> adapter.dataStartedLoading());
    }
    AtomicLong timer = new AtomicLong();
    getDataSingle(DataRequest.create(
        fromRefresh,
        fromSaveInstanceState && page != 0,
        pageToRequest)).observeOn(AndroidSchedulers.mainThread())
        .doOnEvent((result, t) -> {
          swipeRefreshLayout.setEnabled(true);
          swipeRefreshLayout.setRefreshing(false);
        })
        .doOnSubscribe(disposable -> timer.set(System.currentTimeMillis()))
        .doFinally(() -> {
          Timber.d("Data load - %s - took: %dms",
              getClass().getSimpleName(),
              System.currentTimeMillis() - timer.get());
          isDataLoading = false;
          recyclerView.post(() -> adapter.dataFinishedLoading());
        })
        .to(new SingleScoper<>(this))
        .subscribe(data -> {
          progress.setVisibility(GONE);
          errorView.setVisibility(GONE);
          swipeRefreshLayout.setVisibility(VISIBLE);
          recyclerView.post(() -> {
            if (fromRefresh) {
              adapter.setData(data);
            } else {
              adapter.addData(data);
            }
          });
        }, e -> {
          Activity activity = getActivity();
          if (pageToRequest == 0 && activity != null) {
            if (e instanceof IOException) {
              AnimatedVectorDrawableCompat avd =
                  AnimatedVectorDrawableCompat.create(activity, R.drawable.avd_no_connection);
              errorImage.setImageDrawable(avd);
              progress.setVisibility(GONE);
              errorTextView.setText("Network Problem");
              swipeRefreshLayout.setVisibility(GONE);
              errorView.setVisibility(VISIBLE);
              avd.start();
            } else if (e instanceof HttpException) {
              // TODO Show some sort of API error response.
              AnimatedVectorDrawableCompat avd =
                  AnimatedVectorDrawableCompat.create(activity, R.drawable.avd_no_connection);
              errorImage.setImageDrawable(avd);
              progress.setVisibility(GONE);
              errorTextView.setText("API Problem");
              swipeRefreshLayout.setVisibility(GONE);
              errorView.setVisibility(VISIBLE);
              avd.start();
            } else {
              // TODO Show some sort of generic response error
              AnimatedVectorDrawableCompat avd =
                  AnimatedVectorDrawableCompat.create(activity, R.drawable.avd_no_connection);
              errorImage.setImageDrawable(avd);
              progress.setVisibility(GONE);
              swipeRefreshLayout.setVisibility(GONE);
              errorTextView.setText("Unknown issue.");
              errorView.setVisibility(VISIBLE);
              avd.start();
              Timber.e(e, "Unknown issue.");
            }
          } else {
            page--;
          }
        });
  }

  @Override public void onRefresh() {
    loadData(true);
  }

  @Override public void onRequestScrollToTop() {
    if (adapter.getItemCount() > 50) {
      recyclerView.scrollToPosition(0);
    } else {
      recyclerView.smoothScrollToPosition(0);
    }
  }

  private static class Adapter<T extends HasStableId>
      extends RecyclerView.Adapter<RecyclerView.ViewHolder>
      implements DataLoadingSubject.DataLoadingCallbacks {

    private final LinkedHashSet<T> data = new LinkedHashSet<>();
    private final BiConsumer<T, NewsItemViewHolder> bindDelegate;
    private boolean showLoadingMore = false;

    public Adapter(@NonNull BiConsumer<T, NewsItemViewHolder> bindDelegate) {
      super();
      this.bindDelegate = bindDelegate;
      setHasStableIds(true);
    }

    @Override public long getItemId(int position) {
      if (getItemViewType(position) == TYPE_LOADING_MORE) {
        return RecyclerView.NO_ID;
      }
      return Iterables.get(data, position)
          .stableId();
    }

    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
      switch (viewType) {
        case TYPE_ITEM:
          return new NewsItemViewHolder(layoutInflater.inflate(R.layout.list_item_general,
              parent,
              false));
        case TYPE_LOADING_MORE:
          return new LoadingMoreHolder(layoutInflater.inflate(R.layout.infinite_loading,
              parent,
              false));
      }
      throw new InvalidParameterException("Unrecognized view type - " + viewType);
    }

    @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      switch (getItemViewType(position)) {
        case TYPE_ITEM:
          try {
            bindDelegate.accept(Iterables.get(data, position), (NewsItemViewHolder) holder);
          } catch (Exception e) {
            Timber.e(e, "Bind delegate failure!");
          }
          break;
        case TYPE_LOADING_MORE:
          ((LoadingMoreHolder) holder).progress.setVisibility(
              (position > 0) ? View.VISIBLE : View.INVISIBLE);
          break;
      }
    }

    @Override public int getItemCount() {
      return getDataItemCount() + (showLoadingMore ? 1 : 0);
    }

    public int getDataItemCount() {
      return data.size();
    }

    private int getLoadingMoreItemPosition() {
      return showLoadingMore ? getItemCount() - 1 : RecyclerView.NO_POSITION;
    }

    @Override public int getItemViewType(int position) {
      if (position < getDataItemCount() && getDataItemCount() > 0) {
        return TYPE_ITEM;
      }
      return TYPE_LOADING_MORE;
    }

    @Override public void dataStartedLoading() {
      if (showLoadingMore) {
        return;
      }
      showLoadingMore = true;
      notifyItemInserted(getLoadingMoreItemPosition());
    }

    @Override public void dataFinishedLoading() {
      if (!showLoadingMore) {
        return;
      }
      final int loadingPos = getLoadingMoreItemPosition();
      showLoadingMore = false;
      notifyItemRemoved(loadingPos);
    }

    public void addData(List<T> newData) {
      int prevSize = data.size();
      data.addAll(newData);
      notifyItemRangeInserted(prevSize, data.size() - prevSize);
    }

    public void setData(List<T> newData) {
      data.clear();
      data.addAll(newData);
      notifyDataSetChanged();
    }
  }

  @AutoValue
  public abstract static class DataRequest {

    public abstract boolean fromRefresh();

    public abstract boolean multipage();

    public abstract int page();

    static DataRequest create(boolean fromRefresh, boolean multipage, int page) {
      return new AutoValue_BaseNewsController_DataRequest(fromRefresh, multipage, page);
    }
  }

  public static class NewsItemViewHolder extends RxViewHolder {

    @BindView(R.id.container) View container;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.score) TextView score;
    @BindView(R.id.score_divider) TextView scoreDivider;
    @BindView(R.id.timestamp) TextView timestamp;
    @BindView(R.id.author) TextView author;
    @BindView(R.id.author_divider) TextView authorDivider;
    @BindView(R.id.source) TextView source;
    @BindView(R.id.comments) TextView comments;
    @BindView(R.id.tag) TextView tag;
    @BindView(R.id.tag_divider) View tagDivider;
    private Unbinder unbinder;

    public NewsItemViewHolder(@NonNull View itemView) {
      super(itemView);
      if (unbinder != null) {
        unbinder.unbind();
      }
      unbinder = new BaseNewsController$NewsItemViewHolder_ViewBinding(this, itemView);
    }

    public Observable<Object> itemClicks() {
      return RxView.clicks(container);
    }

    public Observable<Object> itemLongClicks() {
      return RxView.longClicks(container);
    }

    public Observable<Object> itemCommentClicks() {
      return RxView.clicks(comments);
    }

    public void title(@NonNull CharSequence titleText) {
      title.setText(titleText);
    }

    public void score(@Nullable Pair<String, Integer> scoreValue) {
      if (scoreValue == null) {
        score.setVisibility(GONE);
        scoreDivider.setVisibility(GONE);
      } else {
        scoreDivider.setVisibility(VISIBLE);
        score.setVisibility(VISIBLE);
        score.setText(String.format("%s %s",
            scoreValue.first,
            NumberUtil.format(scoreValue.second)));
      }
    }

    public void tag(@Nullable String text) {
      if (text == null) {
        tag.setVisibility(GONE);
        tagDivider.setVisibility(GONE);
      } else {
        tag.setVisibility(VISIBLE);
        tagDivider.setVisibility(VISIBLE);
        tag.setText(Strings.capitalize(text));
      }
    }

    public void timestamp(Instant instant) {
      timestamp(instant.toEpochMilli());
    }

    private void timestamp(long date) {
      timestamp.setText(DateUtils.getRelativeTimeSpanString(date,
          System.currentTimeMillis(),
          0L,
          DateUtils.FORMAT_ABBREV_ALL));
    }

    public void author(@Nullable CharSequence authorText) {
      if (authorText == null) {
        author.setVisibility(GONE);
        authorDivider.setVisibility(GONE);
      } else {
        authorDivider.setVisibility(VISIBLE);
        author.setVisibility(VISIBLE);
        author.setText(authorText);
      }
    }

    public void source(@Nullable CharSequence sourceText) {
      if (sourceText == null) {
        source.setVisibility(GONE);
        authorDivider.setVisibility(GONE);
      } else {
        if (author.getVisibility() == VISIBLE) {
          authorDivider.setVisibility(VISIBLE);
        }
        source.setVisibility(VISIBLE);
        source.setText(sourceText);
      }
    }

    public void comments(int commentsCount) {
      comments.setText(NumberUtil.format(commentsCount));
    }

    public void hideComments() {
      comments.setVisibility(GONE);
    }
  }
}
