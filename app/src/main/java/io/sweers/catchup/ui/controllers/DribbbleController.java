package io.sweers.catchup.ui.controllers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ArrayRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.jakewharton.rxbinding.view.RxView;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Rfc3339DateJsonAdapter;
import com.uber.autodispose.AutoDispose;
import dagger.Lazy;
import dagger.Provides;
import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.R;
import io.sweers.catchup.data.AuthInterceptor;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.RxViewHolder;
import io.sweers.catchup.data.dribbble.DribbbleService;
import io.sweers.catchup.data.dribbble.model.Shot;
import io.sweers.catchup.injection.qualifiers.ForApi;
import io.sweers.catchup.injection.scopes.PerController;
import io.sweers.catchup.ui.Scrollable;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.ServiceController;
import io.sweers.catchup.ui.widget.BadgedFourThreeImageView;
import io.sweers.catchup.util.ObservableColorMatrix;
import io.sweers.catchup.util.UiUtil;
import io.sweers.catchup.util.glide.DribbbleTarget;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import okhttp3.OkHttpClient;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.functions.Action2;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class DribbbleController extends ServiceController
    implements SwipeRefreshLayout.OnRefreshListener, Scrollable {

  @ColorInt private static final int INITIAL_GIF_BADGE_COLOR = 0x40ffffff;
  @Inject DribbbleService service;
  @Inject LinkManager linkManager;
  @BindView(R.id.error_container) View errorView;
  @BindView(R.id.error_image) ImageView errorImage;
  @BindView(R.id.error_message) TextView errorTextView;
  @BindView(R.id.list) RecyclerView recyclerView;
  @BindView(R.id.progress) ProgressBar progress;
  @BindView(R.id.refresh) SwipeRefreshLayout swipeRefreshLayout;
  private Adapter adapter;

  public DribbbleController() {
    super();
  }

  public DribbbleController(Bundle args) {
    super(args);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_Dribbble);
  }

  @Override
  protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    return inflater.inflate(R.layout.controller_basic_news, container, false);
  }

  @Override protected Unbinder bind(@NonNull View view) {
    return new DribbbleController_ViewBinding(this, view);
  }

  @Override protected void onViewBound(@NonNull View view) {
    super.onViewBound(view);
    // TODO There must be an earlier place than this
    DaggerDribbbleController_Component.builder()
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);

    swipeRefreshLayout.setColorSchemeColors(getServiceThemeColor());

    GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
    recyclerView.setLayoutManager(layoutManager);
    adapter = new Adapter(view.getContext(),
        (shot, viewHolder) -> RxJavaInterop.toV2Observable(RxView.clicks(viewHolder.itemView)
            .map(o -> new Object()))
            .compose(transformUrlToMeta(shot.htmlUrl()))
            .flatMapCompletable(linkManager)
            .subscribe(AutoDispose.completable()
                .scopeWith(viewHolder)
                .empty()));
    recyclerView.setAdapter(adapter);
    swipeRefreshLayout.setOnRefreshListener(this);
  }

  @Override protected void onAttach(@NonNull View view) {
    super.onAttach(view);
    swipeRefreshLayout.setEnabled(false);
    loadData();
  }

  private void loadData() {
    service.getPopular(1, 50)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnEvent((result, t) -> {
          swipeRefreshLayout.setEnabled(true);
          swipeRefreshLayout.setRefreshing(false);
        })
        .subscribe(AutoDispose.single()
            .scopeWith(this)
            .around(shots -> {
              progress.setVisibility(GONE);
              errorView.setVisibility(GONE);
              swipeRefreshLayout.setVisibility(VISIBLE);
              adapter.setShots(shots);
            }, e -> {
              if (e instanceof IOException) {
                AnimatedVectorDrawableCompat avd =
                    AnimatedVectorDrawableCompat.create(getActivity(),
                        R.drawable.avd_no_connection);
                errorImage.setImageDrawable(avd);
                errorTextView.setText("Network Problem");
                progress.setVisibility(GONE);
                swipeRefreshLayout.setVisibility(GONE);
                errorView.setVisibility(VISIBLE);
                avd.start();
              } else if (e instanceof HttpException) {
                // TODO Show some sort of API error response.
                AnimatedVectorDrawableCompat avd =
                    AnimatedVectorDrawableCompat.create(getActivity(),
                        R.drawable.avd_no_connection);
                errorImage.setImageDrawable(avd);
                errorTextView.setText("API Problem");
                progress.setVisibility(GONE);
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

  @OnClick(R.id.retry_button) void onRetry() {
    errorView.setVisibility(GONE);
    progress.setVisibility(VISIBLE);
    onRefresh();
  }

  @OnClick(R.id.error_image) void onErrorClick(ImageView imageView) {
    AnimatedVectorDrawableCompat avd = (AnimatedVectorDrawableCompat) imageView.getDrawable();
    avd.start();
  }

  @Override public void onRefresh() {
    loadData();
  }

  @Override public void onRequestScrollToTop() {
    recyclerView.smoothScrollToPosition(0);
  }

  @PerController
  @dagger.Component(modules = DribbbleController.Module.class,
                    dependencies = ActivityComponent.class)
  public interface Component {
    void inject(DribbbleController controller);
  }

  private static class Adapter extends RecyclerView.Adapter<Adapter.DribbbleShotHolder> {

    private final List<Shot> shots = new ArrayList<>();
    private final ColorDrawable[] shotLoadingPlaceholders;
    @NonNull private final Action2<Shot, DribbbleShotHolder> bindDelegate;

    public Adapter(Context context, @NonNull Action2<Shot, DribbbleShotHolder> bindDelegate) {
      this.bindDelegate = bindDelegate;
      setHasStableIds(true);
      @ArrayRes int loadingColorArrayId;
      if (UiUtil.isInNightMode(context)) {
        loadingColorArrayId = R.array.loading_placeholders_dark;
      } else {
        loadingColorArrayId = R.array.loading_placeholders_light;
      }
      int[] placeholderColors = context.getResources()
          .getIntArray(loadingColorArrayId);
      shotLoadingPlaceholders = new ColorDrawable[placeholderColors.length];
      for (int i = 0; i < placeholderColors.length; i++) {
        shotLoadingPlaceholders[i] = new ColorDrawable(placeholderColors[i]);
      }
    }

    public void setShots(List<Shot> newShots) {
      boolean wasEmpty = shots.isEmpty();
      if (!wasEmpty) {
        shots.clear();
      }
      shots.addAll(newShots);
      if (wasEmpty) {
        notifyItemRangeInserted(0, shots.size());
      } else {
        notifyDataSetChanged();
      }
    }

    @Override public long getItemId(int position) {
      return shots.get(position)
          .stableId();
    }

    @TargetApi(Build.VERSION_CODES.M) @Override
    public DribbbleShotHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      final DribbbleShotHolder holder =
          new DribbbleShotHolder(LayoutInflater.from(parent.getContext())
              .inflate(R.layout.dribbble_shot_item, parent, false));
      holder.image.setBadgeColor(INITIAL_GIF_BADGE_COLOR);
      holder.image.setForeground(UiUtil.createColorSelector(0x40808080, null));
      // play animated GIFs whilst touched
      holder.image.setOnTouchListener((v, event) -> {
        // check if it's an event we care about, else bail fast
        final int action = event.getAction();
        if (!(action == MotionEvent.ACTION_DOWN
            || action == MotionEvent.ACTION_UP
            || action == MotionEvent.ACTION_CANCEL)) {
          return false;
        }

        // get the image and check if it's an animated GIF
        final Drawable drawable = holder.image.getDrawable();
        if (drawable == null) return false;
        GifDrawable gif = null;
        if (drawable instanceof GifDrawable) {
          gif = (GifDrawable) drawable;
        } else if (drawable instanceof TransitionDrawable) {
          // we fade in images on load which uses a TransitionDrawable; check its layers
          TransitionDrawable fadingIn = (TransitionDrawable) drawable;
          for (int i = 0; i < fadingIn.getNumberOfLayers(); i++) {
            if (fadingIn.getDrawable(i) instanceof GifDrawable) {
              gif = (GifDrawable) fadingIn.getDrawable(i);
              break;
            }
          }
        }
        if (gif == null) return false;
        // GIF found, start/stop it on press/lift
        switch (action) {
          case MotionEvent.ACTION_DOWN:
            gif.start();
            break;
          case MotionEvent.ACTION_UP:
          case MotionEvent.ACTION_CANCEL:
            gif.stop();
            break;
        }
        return false;
      });
      return holder;
    }

    @Override public void onBindViewHolder(DribbbleShotHolder holder, int position) {
      holder.bindView(shots.get(position));
      bindDelegate.call(shots.get(position), holder);
    }

    @Override public int getItemCount() {
      return shots.size();
    }

    @Override @SuppressLint("NewApi") public void onViewRecycled(DribbbleShotHolder holder) {
      // reset the badge & ripple which are dynamically determined
      holder.image.setBadgeColor(INITIAL_GIF_BADGE_COLOR);
      holder.image.showBadge(false);
      holder.image.setForeground(UiUtil.createColorSelector(0x40808080, null));
      RxViewHolder.onViewRecycled(holder);
    }

    class DribbbleShotHolder extends RxViewHolder {

      private BadgedFourThreeImageView image;

      public DribbbleShotHolder(View itemView) {
        super(itemView);
        image = (BadgedFourThreeImageView) itemView;
      }

      public void bindView(Shot shot) {
        final int[] imageSize = shot.images()
            .bestSize();
        Glide.with(itemView.getContext())
            .load(shot.images()
                .best())
            .listener(new RequestListener<String, GlideDrawable>() {
              @Override public boolean onResourceReady(GlideDrawable resource,
                  String model,
                  Target<GlideDrawable> target,
                  boolean isFromMemoryCache,
                  boolean isFirstResource) {
                if (!shot.hasFadedIn) {
                  image.setHasTransientState(true);
                  final ObservableColorMatrix cm = new ObservableColorMatrix();
                  final ObjectAnimator saturation =
                      ObjectAnimator.ofFloat(cm, ObservableColorMatrix.SATURATION, 0f, 1f);
                  saturation.addUpdateListener(valueAnimator -> {
                    // just animating the color matrix does not invalidate the
                    // drawable so need this update listener.  Also have to create a
                    // new CMCF as the matrix is immutable :(
                    image.setColorFilter(new ColorMatrixColorFilter(cm));
                  });
                  saturation.setDuration(2000L);
                  saturation.setInterpolator(new FastOutSlowInInterpolator());
                  saturation.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) {
                      image.clearColorFilter();
                      image.setHasTransientState(false);
                    }
                  });
                  saturation.start();
                  shot.hasFadedIn = true;
                }
                return false;
              }

              @Override public boolean onException(Exception e,
                  String model,
                  Target<GlideDrawable> target,
                  boolean isFirstResource) {
                return false;
              }
            })
            .placeholder(shotLoadingPlaceholders[getAdapterPosition()
                % shotLoadingPlaceholders.length])
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .fitCenter()
            .override(imageSize[0], imageSize[1])
            .into(new DribbbleTarget(image, false));
        // need both placeholder & background to prevent seeing through shot as it fades in
        image.setBackground(shotLoadingPlaceholders[getAdapterPosition()
            % shotLoadingPlaceholders.length]);
        image.showBadge(shot.animated());
      }
    }
  }

  @dagger.Module
  public abstract static class Module {

    @Provides @PerController @ForApi
    static OkHttpClient provideDribbbleOkHttpClient(OkHttpClient client) {
      return client.newBuilder()
          .addInterceptor(AuthInterceptor.create("Bearer",
              BuildConfig.DRIBBBLE_CLIENT_ACCESS_TOKEN))
          .build();
    }

    @Provides @PerController @ForApi static Moshi provideDribbbleMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Date.class, new Rfc3339DateJsonAdapter())
          .build();
    }

    @Provides @PerController
    static DribbbleService provideDribbbleService(@ForApi final Lazy<OkHttpClient> client,
        @ForApi Moshi moshi,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
      return new Retrofit.Builder().baseUrl(DribbbleService.ENDPOINT)
          .callFactory(request -> client.get()
              .newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()
          .create(DribbbleService.class);
    }
  }
}
