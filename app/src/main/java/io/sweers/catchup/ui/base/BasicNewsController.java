package io.sweers.catchup.ui.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
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
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import io.sweers.catchup.R;
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

import static android.view.View.GONE;


public abstract class BasicNewsController<T> extends BaseController
    implements SwipeRefreshLayout.OnRefreshListener {

  @Bind(R.id.error_container) View errorView;
  @Bind(R.id.error_image) ImageView errorImage;
  @Bind(R.id.list) RecyclerView recyclerView;
  @Bind(R.id.progress) ProgressBar progress;
  @Bind(R.id.refresh) SwipeRefreshLayout swipeRefreshLayout;

  private Adapter adapter;

  public BasicNewsController() {
    this(null);
  }

  public BasicNewsController(Bundle args) {
    super(args);
  }

  protected abstract void performInjection();

  /**
   * View binding implementation to bind the given datum to the {@code holder}.
   *
   * @param holder The item ViewHolder instance.
   * @param view   The underlying view, for convenience.
   * @param t      The datum to back the view with.
   */
  protected abstract void bindItemView(
      @NonNull ViewHolder holder,
      @NonNull View view,
      @NonNull T t);

  /**
   * Main handler for row clicks.
   *
   * @param holder The item ViewHolder instance.
   * @param view   The underlying view, for convenience.
   * @param t      The datum backing the view.
   */
  protected abstract void onItemClick(
      @NonNull ViewHolder holder,
      @NonNull View view,
      @NonNull T t);

  /**
   * Optional comment click handling. If you don't override this, you probably should hide the
   * comments view in the item view.
   *
   * @param holder The item ViewHolder instance.
   * @param view   The underlying view, for convenience.
   * @param t      The datum backing the view.
   */
  protected void onCommentClick(
      @NonNull ViewHolder holder,
      @NonNull View view,
      @NonNull T t) {

  }

  /**
   * Optional item long click handling. If you don't override this, you should disable longclick on
   * the item view.
   *
   * @param holder The item ViewHolder instance.
   * @param view   The underlying view, for convenience.
   * @param t      The datum backing the view.
   * @return {@code true} if the click was handled, {@link false} if not.
   */
  protected boolean onItemLongClick(
      @NonNull ViewHolder holder,
      @NonNull View view,
      @NonNull T t) {
    return false;
  }

  @NonNull
  protected abstract Observable<List<T>> getDataObservable();

  @Override
  protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    return inflater.inflate(R.layout.controller_basic_news, container, false);
  }

  @Override protected void onViewBound(@NonNull View view) {
    super.onViewBound(view);
    // TODO There must be an earlier place than this
    performInjection();

    swipeRefreshLayout.setColorSchemeColors(getServiceThemeColor());

    LinearLayoutManager layoutManager
        = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    recyclerView.setLayoutManager(layoutManager);
    adapter = new Adapter();
    recyclerView.setAdapter(adapter);
    swipeRefreshLayout.setOnRefreshListener(this);

    FadeInUpAnimator itemAnimator = new FadeInUpAnimator(new OvershootInterpolator(1f));
    itemAnimator.setAddDuration(300);
    itemAnimator.setRemoveDuration(300);
    recyclerView.setItemAnimator(itemAnimator);
  }

  @OnClick(R.id.retry_button) void onRetry() {
    errorView.setVisibility(GONE);
    progress.setVisibility(View.VISIBLE);
    onRefresh();
  }

  @OnClick(R.id.error_image) void onErrorClick(ImageView imageView) {
    AnimatedVectorDrawableCompat avd = (AnimatedVectorDrawableCompat) imageView.getDrawable();
    avd.start();
  }

  @Override
  protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    swipeRefreshLayout.setEnabled(false);
    loadData();
  }

  @Override protected void onDestroyView(View view) {
    ButterKnife.unbind(this);
    super.onDestroyView(view);
  }

  private void loadData() {
    getDataObservable()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnUnsubscribe(() -> {
          swipeRefreshLayout.setEnabled(true);
          swipeRefreshLayout.setRefreshing(false);
        })
        .compose(this.<List<T>>bindToLifecycle())
        .subscribe(
            data -> {
              progress.setVisibility(GONE);
              errorView.setVisibility(GONE);
              swipeRefreshLayout.setVisibility(View.VISIBLE);
              adapter.setData(data);
            },
            e -> {
              if (e instanceof IOException) {
                AnimatedVectorDrawableCompat avd = AnimatedVectorDrawableCompat.create(
                    getActivity(),
                    R.drawable.avd_no_connection);
                errorImage.setImageDrawable(avd);
                progress.setVisibility(GONE);
                swipeRefreshLayout.setVisibility(GONE);
                errorView.setVisibility(View.VISIBLE);
                avd.start();
              } else if (e instanceof HttpException) {
                // TODO Show some sort of API error response.
                AnimatedVectorDrawableCompat avd = AnimatedVectorDrawableCompat.create(
                    getActivity(),
                    R.drawable.avd_no_connection);
                errorImage.setImageDrawable(avd);
                progress.setVisibility(GONE);
                swipeRefreshLayout.setVisibility(GONE);
                errorView.setVisibility(View.VISIBLE);
                avd.start();
              }
              Timber.e(e, "Update failed!");
              Toast.makeText(getActivity(), "Failed - " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
  }

  @Override
  public void onRefresh() {
    loadData();
  }

  private class Adapter extends RecyclerView.Adapter<ViewHolder> {

    private final List<T> data = new ArrayList<>();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_general, parent, false);
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      holder.bindViewInternal(data.get(position));
    }

    @Override
    public int getItemCount() {
      return data.size();
    }

    public void setData(List<T> newData) {
      boolean wasEmpty = data.isEmpty();
      if (!wasEmpty) {
        data.clear();
      }
      data.addAll(newData);
      if (wasEmpty) {
        notifyItemRangeInserted(0, data.size());
      } else {
        notifyDataSetChanged();
      }
    }
  }

  protected class ViewHolder extends RecyclerView.ViewHolder {

    @Bind(R.id.title) TextView title;
    @Bind(R.id.score) TextView score;
    @Bind(R.id.score_divider) TextView scoreDivider;
    @Bind(R.id.timestamp) TextView timestamp;
    @Bind(R.id.author) TextView author;
    @Bind(R.id.author_divider) TextView authorDivider;
    @Bind(R.id.source) TextView source;
    @Bind(R.id.comments) TextView comments;

    private T datum;

    public ViewHolder(@NonNull View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    void bindViewInternal(T datum) {
      this.datum = datum;
      bindItemView(this, itemView, datum);
    }

    @OnClick(R.id.container) void onItemClickInternal() {
      onItemClick(this, itemView, datum);
    }

    @OnLongClick(R.id.container) boolean onItemLongClickInternal() {
      return onItemLongClick(this, itemView, datum);
    }

    @OnClick(R.id.comments) void onCommentsClickInternal() {
      onCommentClick(this, itemView, datum);
    }

    public TextView score() {
      return score;
    }

    public void title(@NonNull CharSequence titleText) {
      title.setText(titleText);
    }

    public void score(@Nullable CharSequence scoreValue) {
      // TODO Shorten large numbers
      if (scoreValue == null) {
        score.setVisibility(GONE);
        scoreDivider.setVisibility(GONE);
      } else {
        scoreDivider.setVisibility(View.VISIBLE);
        score.setVisibility(View.VISIBLE);
        score.setText(scoreValue);
      }
    }

    public void timestamp(Date date) {
      timestamp.setText(DateUtils.getRelativeTimeSpanString(date.getTime(), System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
    }

    public void author(@NonNull CharSequence authorText) {
      author.setText(authorText);
    }

    public void source(@Nullable CharSequence sourceText) {
      if (sourceText == null) {
        source.setVisibility(GONE);
        authorDivider.setVisibility(GONE);
      } else {
        authorDivider.setVisibility(View.VISIBLE);
        source.setVisibility(View.VISIBLE);
        source.setText(sourceText);
      }
    }

    public void comments(int commentsCount) {
      // TODO Shorten large numbers
      comments.setText(String.format(Locale.getDefault(), "%d", commentsCount));
    }
  }
}
