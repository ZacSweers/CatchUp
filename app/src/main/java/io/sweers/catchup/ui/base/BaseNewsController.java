package io.sweers.catchup.ui.base;

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
import com.jakewharton.rxbinding.view.RxView;
import com.uber.autodispose.AutoDispose;
import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiConsumer;
import io.sweers.catchup.R;
import io.sweers.catchup.data.RxViewHolder;
import io.sweers.catchup.ui.Scrollable;
import io.sweers.catchup.util.NumberUtil;
import io.sweers.catchup.util.Strings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator;
import org.threeten.bp.Instant;
import retrofit2.adapter.rxjava2.HttpException;
import rx.functions.Func1;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public abstract class BaseNewsController<T extends HasStableId> extends ServiceController
    implements SwipeRefreshLayout.OnRefreshListener, Scrollable {

  @BindView(R.id.error_container) View errorView;
  @BindView(R.id.error_message) TextView errorTextView;
  @BindView(R.id.error_image) ImageView errorImage;
  @BindView(R.id.list) RecyclerView recyclerView;
  @BindView(R.id.progress) ProgressBar progress;
  @BindView(R.id.refresh) SwipeRefreshLayout swipeRefreshLayout;

  private Adapter<T> adapter;

  public BaseNewsController() {
    super();
  }

  public BaseNewsController(Bundle args) {
    super(args);
  }

  protected abstract void performInjection();

  /**
   * View binding implementation to bind the given datum to the {@code holder}.
   *
   * @param t The datum to back the view with.
   * @param holder The item ViewHolder instance.
   */
  protected abstract void bindItemView(@NonNull T t, @NonNull ViewHolder holder);

  @NonNull protected abstract Single<List<T>> getDataSingle();

  @Override
  protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    return inflater.inflate(R.layout.controller_basic_news, container, false);
  }

  @Override protected Unbinder bind(@NonNull View view) {
    return new BaseNewsController_ViewBinding(this, view);
  }

  @Override protected void onViewBound(@NonNull View view) {
    super.onViewBound(view);
    // TODO There must be an earlier place than this
    performInjection();

    swipeRefreshLayout.setColorSchemeColors(getServiceThemeColor());

    LinearLayoutManager layoutManager =
        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    recyclerView.setLayoutManager(layoutManager);
    adapter = new Adapter<>(this::bindItemView);
    recyclerView.setAdapter(adapter);
    swipeRefreshLayout.setOnRefreshListener(this);

    FadeInUpAnimator itemAnimator = new FadeInUpAnimator(new OvershootInterpolator(1f));
    itemAnimator.setAddDuration(300);
    itemAnimator.setRemoveDuration(300);
    recyclerView.setItemAnimator(itemAnimator);
    loadData();
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
  }

  private void loadData() {
    AtomicLong timer = new AtomicLong();
    getDataSingle().observeOn(AndroidSchedulers.mainThread())
        .doOnEvent((result, t) -> {
          swipeRefreshLayout.setEnabled(true);
          swipeRefreshLayout.setRefreshing(false);
        })
        .doOnSubscribe(disposable -> timer.set(System.currentTimeMillis()))
        .doFinally(() -> System.out.println("Data load - "
            + getClass().getSimpleName()
            + " - took: "
            + (System.currentTimeMillis() - timer.get())))
        .subscribe(AutoDispose.single()
            .scopeWith(this)
            .around(data -> {
              progress.setVisibility(GONE);
              errorView.setVisibility(GONE);
              swipeRefreshLayout.setVisibility(VISIBLE);
              adapter.setData(data);
            }, e -> {
              if (e instanceof IOException) {
                AnimatedVectorDrawableCompat avd =
                    AnimatedVectorDrawableCompat.create(getActivity(),
                        R.drawable.avd_no_connection);
                errorImage.setImageDrawable(avd);
                progress.setVisibility(GONE);
                errorTextView.setText("Network Problem");
                swipeRefreshLayout.setVisibility(GONE);
                errorView.setVisibility(VISIBLE);
                avd.start();
              } else if (e instanceof HttpException) {
                // TODO Show some sort of API error response.
                AnimatedVectorDrawableCompat avd =
                    AnimatedVectorDrawableCompat.create(getActivity(),
                        R.drawable.avd_no_connection);
                errorImage.setImageDrawable(avd);
                progress.setVisibility(GONE);
                errorTextView.setText("API Problem");
                swipeRefreshLayout.setVisibility(GONE);
                errorView.setVisibility(VISIBLE);
                avd.start();
              } else {
                // TODO Show some sort of generic response error
                AnimatedVectorDrawableCompat avd =
                    AnimatedVectorDrawableCompat.create(getActivity(),
                        R.drawable.avd_no_connection);
                errorImage.setImageDrawable(avd);
                progress.setVisibility(GONE);
                swipeRefreshLayout.setVisibility(GONE);
                errorTextView.setText("Unknown Issue");
                errorView.setVisibility(VISIBLE);
                avd.start();
              }
              Timber.e(e, "Update failed!");
            }));
  }

  @Override public void onRefresh() {
    loadData();
  }

  @Override public void onRequestScrollToTop() {
    recyclerView.smoothScrollToPosition(0);
  }

  private static class Adapter<T extends HasStableId> extends RecyclerView.Adapter<ViewHolder> {

    private final List<T> data = new ArrayList<>();
    private final BiConsumer<T, ViewHolder> bindDelegate;

    public Adapter(@NonNull BiConsumer<T, ViewHolder> bindDelegate) {
      super();
      this.bindDelegate = bindDelegate;
      setHasStableIds(true);
    }

    @Override public long getItemId(int position) {
      return data.get(position)
          .stableId();
    }

    @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.list_item_general, parent, false);
      return new ViewHolder(view);
    }

    @Override public void onBindViewHolder(ViewHolder holder, int position) {
      try {
        bindDelegate.accept(data.get(position), holder);
      } catch (Exception e) {
        Timber.e(e, "Bind delegate failure!");
      }
    }

    @Override public int getItemCount() {
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

    @Override public void onViewRecycled(ViewHolder holder) {
      super.onViewRecycled(holder);
      RxViewHolder.onViewRecycled(holder);
    }
  }

  public static class ViewHolder extends RxViewHolder {

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

    public ViewHolder(@NonNull View itemView) {
      super(itemView);
      if (unbinder != null) {
        unbinder.unbind();
      }
      unbinder = new BaseNewsController$ViewHolder_ViewBinding(this, itemView);
    }

    private static final Object TEMP_OBJECT = new Object();
    private static final Func1<Void, Object> TEMP_FUNCTION = (Func1<Void, Object>) o -> TEMP_OBJECT;

    public Observable<Object> itemClicks() {
      return RxJavaInterop.toV2Observable(RxView.clicks(container)
          .map(TEMP_FUNCTION));
    }

    public Observable<Object> itemLongClicks() {
      return RxJavaInterop.toV2Observable(RxView.longClicks(container)
          .map(TEMP_FUNCTION));
    }

    public Observable<Object> itemCommentClicks() {
      return RxJavaInterop.toV2Observable(RxView.clicks(comments)
          .map(TEMP_FUNCTION));
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

    public void timestamp(Date date) {
      timestamp(date.getTime());
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

    public void author(@NonNull CharSequence authorText) {
      author.setText(authorText);
    }

    public void source(@Nullable CharSequence sourceText) {
      if (sourceText == null) {
        source.setVisibility(GONE);
        authorDivider.setVisibility(GONE);
      } else {
        authorDivider.setVisibility(VISIBLE);
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
