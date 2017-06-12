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

package android.support.v7.widget;

import android.view.View;
import com.uber.autodispose.ScopeProvider;
import io.reactivex.Maybe;
import io.reactivex.subjects.MaybeSubject;

public abstract class RxViewHolder extends RecyclerView.ViewHolder implements ScopeProvider {

  private static final Object NOTIFICATION = new Object();
  private MaybeSubject<Object> unbindNotifier;

  public RxViewHolder(View itemView) {
    super(itemView);
  }

  private void onUnBind() {
    emitUnBindIfPresent();
    unbindNotifier = null;
  }

  private MaybeSubject<?> getOrInitNotifier() {
    if (unbindNotifier == null) {
      unbindNotifier = MaybeSubject.create();
    }
    return unbindNotifier;
  }

  private void emitUnBindIfPresent() {
    if (unbindNotifier != null && !unbindNotifier.hasComplete()) {
      unbindNotifier.onSuccess(NOTIFICATION);
    }
  }

  @Override public Maybe<?> requestScope() {
    return getOrInitNotifier();
  }

  @Override void setFlags(int flags, int mask) {
    boolean wasBound = isBound();
    super.setFlags(flags, mask);
    if (wasBound && !isBound()) {
      onUnBind();
    }
  }

  @Override void addFlags(int flags) {
    boolean wasBound = isBound();
    super.addFlags(flags);
    if (wasBound && !isBound()) {
      onUnBind();
    }
  }

  @Override void clearPayload() {
    boolean wasBound = isBound();
    super.clearPayload();
    if (wasBound && !isBound()) {
      onUnBind();
    }
  }

  @Override void resetInternal() {
    boolean wasBound = isBound();
    super.resetInternal();
    if (wasBound && !isBound()) {
      onUnBind();
    }
  }
}
