package io.sweers.catchup.data;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.uber.autodispose.ScopeProvider;
import io.reactivex.Maybe;
import io.reactivex.subjects.MaybeSubject;

public abstract class RxViewHolder extends RecyclerView.ViewHolder implements ScopeProvider {

  private MaybeSubject<Object> unbindNotifier;

  public RxViewHolder(View itemView) {
    super(itemView);
  }

  private void onUnBind() {
    emitUnBindIfPresent();
  }

  private MaybeSubject<?> getOrInitNotifier() {
    if (unbindNotifier == null) {
      unbindNotifier = MaybeSubject.create();
    }
    return unbindNotifier;
  }

  private void emitUnBindIfPresent() {
    if (unbindNotifier != null && !unbindNotifier.hasComplete()) {
      unbindNotifier.onSuccess(new Object());
    }
  }

  @Override public Maybe<?> requestScope() {
    return getOrInitNotifier();
  }

  /**
   * Proxy for RecyclerView.Adapter's onViewRecycled method to unbind a holder if it's an
   * RxViewHolder instance.
   *
   * @param holder the holder to check
   */
  public static void onViewRecycled(RecyclerView.ViewHolder holder) {
    if (holder instanceof RxViewHolder) {
      ((RxViewHolder) holder).onUnBind();
    }
  }
}
