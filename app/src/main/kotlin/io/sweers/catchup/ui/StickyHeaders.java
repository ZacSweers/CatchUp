/*
 * Copyright (C) 2019. Uber Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sweers.catchup.ui;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adds sticky headers capabilities to the {@link RecyclerView.Adapter}. Should return {@code true}
 * for all positions that represent sticky headers.
 *
 * <p>Adapted from https://github.com/Doist/RecyclerViewExtensions
 */
public interface StickyHeaders {
  boolean isStickyHeader(int position);

  interface ViewSetup {
    /**
     * Adjusts any necessary properties of the {@code holder} that is being used as a sticky header.
     *
     * <p>{@link #teardownStickyHeaderView(View)} will be called sometime after this method and
     * before any other calls to this method go through.
     */
    void setupStickyHeaderView(View stickyHeader);

    /**
     * Reverts any properties changed in {@link #setupStickyHeaderView(View)}.
     *
     * <p>Called after {@link #setupStickyHeaderView(View)}.
     */
    void teardownStickyHeaderView(View stickyHeader);
  }
}
