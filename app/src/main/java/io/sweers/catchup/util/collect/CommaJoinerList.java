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

package io.sweers.catchup.util.collect;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A List implementation that emits toString() calls as comma separated values, useful for
 * something like multi-get requests in retrofit.
 *
 * @param <E> the element type
 */
public class CommaJoinerList<E> implements List<E> {

  public static <T> CommaJoinerList<T> from(List<T> source) {
    return new CommaJoinerList<>(source);
  }

  private final List<E> delegate;

  private CommaJoinerList(List<E> delegate) {
    this.delegate = delegate;
  }

  @Override public int size() {
    return delegate.size();
  }

  @Override public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override public boolean contains(Object o) {
    return delegate.contains(o);
  }

  @NonNull @Override public Iterator<E> iterator() {
    return delegate.iterator();
  }

  @NonNull @Override public Object[] toArray() {
    return delegate.toArray();
  }

  @NonNull @Override public <T> T[] toArray(@NonNull T[] a) {
    return delegate.toArray(a);
  }

  @Override public boolean add(E e) {
    return delegate.add(e);
  }

  @Override public boolean remove(Object o) {
    return delegate.remove(o);
  }

  @Override public boolean containsAll(@NonNull Collection<?> c) {
    return delegate.containsAll(c);
  }

  @Override public boolean addAll(@NonNull Collection<? extends E> c) {
    return delegate.addAll(c);
  }

  @Override public boolean addAll(int index, @NonNull Collection<? extends E> c) {
    return delegate.addAll(index, c);
  }

  @Override public boolean removeAll(@NonNull Collection<?> c) {
    return delegate.removeAll(c);
  }

  @Override public boolean retainAll(@NonNull Collection<?> c) {
    return delegate.retainAll(c);
  }

  @Override public void clear() {
    delegate.clear();
  }

  @Override public E get(int index) {
    return delegate.get(index);
  }

  @Override public E set(int index, E element) {
    return delegate.set(index, element);
  }

  @Override public void add(int index, E element) {
    delegate.add(index, element);
  }

  @Override public E remove(int index) {
    return delegate.remove(index);
  }

  @Override public int indexOf(Object o) {
    return delegate.indexOf(o);
  }

  @Override public int lastIndexOf(Object o) {
    return delegate.lastIndexOf(o);
  }

  @NonNull @Override public ListIterator<E> listIterator() {
    return delegate.listIterator();
  }

  @NonNull @Override public ListIterator<E> listIterator(int index) {
    return delegate.listIterator(index);
  }

  @NonNull @Override public List<E> subList(int fromIndex, int toIndex) {
    return delegate.subList(fromIndex, toIndex);
  }

  @Override public String toString() {
    return TextUtils.join(",", this);
  }
}
