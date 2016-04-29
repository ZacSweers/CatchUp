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
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import io.sweers.catchup.R;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import static android.view.View.GONE;


public abstract class BasicNewsController<T> extends BaseController
    implements SwipeRefreshLayout.OnRefreshListener {

  @Bind(R.id.error_container) View errorView;
  @Bind(R.id.error_image) ImageView errorImage;
  @Bind(R.id.refresh) SwipeRefreshLayout swipeRefreshLayout;
  @Bind(R.id.list) RecyclerView recyclerView;

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
   * @param view The underlying view, for convenience.
   * @param t The datum to back the view with.
   */
  protected abstract void bindItemView(
      @NonNull ViewHolder holder,
      @NonNull View view,
      @NonNull T t);

  /**
   * Main handler for row clicks.
   *
   * @param holder The item ViewHolder instance.
   * @param view The underlying view, for convenience.
   * @param t The datum backing the view.
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
   * @param view The underlying view, for convenience.
   * @param t The datum backing the view.
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
   * @param view The underlying view, for convenience.
   * @param t The datum backing the view.
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

    LinearLayoutManager layoutManager
        = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    recyclerView.setLayoutManager(layoutManager);
    adapter = new Adapter();
    recyclerView.setAdapter(adapter);
    swipeRefreshLayout.setOnRefreshListener(this);

    // TODO Use RecyclerView animators from wasabeef
//    FadeInUpAnimator itemAnimator = new FadeInUpAnimator(new OvershootInterpolator(1f));
//    itemAnimator.setAddDuration(300);
//    itemAnimator.setRemoveDuration(300);
//    recyclerView.setItemAnimator(itemAnimator);
  }

  @OnClick(R.id.retry_button) void onRetry() {
    errorView.setVisibility(GONE);
    swipeRefreshLayout.setRefreshing(true);
    onRefresh();
  }

  @Override
  protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    swipeRefreshLayout.setRefreshing(true);
    loadData();
  }

  @Override protected void onDestroyView(View view) {
    ButterKnife.unbind(this);
    super.onDestroyView(view);
  }

  private void loadData() {
    getDataObservable()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnUnsubscribe(() -> swipeRefreshLayout.setRefreshing(false))
        .compose(this.<List<T>>bindToLifecycle())
        .subscribe(
            data -> {
              adapter.setData(data);
            },
            e -> {
              if (e instanceof HttpException) {
                errorImage.setImageDrawable(
                    AnimatedVectorDrawableCompat.create(
                        getActivity(),
                        R.drawable.avd_no_connection));
                errorView.setVisibility(View.VISIBLE);
              }
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
      data.clear();
      data.addAll(newData);
      notifyDataSetChanged();
    }
  }

  protected class ViewHolder extends RecyclerView.ViewHolder {

    @Bind(R.id.title) TextView title;
    @Bind(R.id.score) TextView score;
    @Bind(R.id.timestamp) TextView timestamp;
    @Bind(R.id.author) TextView author;
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

    public void title(@NonNull CharSequence titleText) {
      title.setText(titleText);
    }

    public void score(@Nullable CharSequence scoreValue) {
      // TODO Shorten large numbers
      setOrHideView(score, scoreValue);
    }

    public void timestamp(long utcTime) {
      timestamp.setText(DateUtils.getRelativeTimeSpanString(utcTime * 1000, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
    }

    public void author(@NonNull CharSequence authorText) {
      author.setText(authorText);
    }

    public void source(@Nullable CharSequence sourceText) {
      setOrHideView(source, sourceText);
    }

    public void comments(int commentsCount) {
      // TODO Shorten large numbers
      comments.setText(String.format(Locale.getDefault(), "%d", commentsCount));
    }

    private void setOrHideView(@NonNull TextView textView, @Nullable CharSequence value) {
      if (value == null) {
        textView.setVisibility(GONE);
      } else {
        textView.setVisibility(View.VISIBLE);
        textView.setText(value);
      }
    }
  }
}
