/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * glassfish/bootstrap/legal/CDDLv1.0.txt or
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]

 * Copyright 2005 The Apache Software Foundation.
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
package kotterknife

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.HashSet

/**
 * A weak HashSet. An element stored in the WeakHashSet might be
 * garbage collected, if there is no strong reference to this element.
 */
class WeakHashSet<T> : MutableSet<T> {

  private val delegate = HashSet<WeakElement<T>>()

  /**
   * Helps to detect garbage collected values.
   */
  private val queue = ReferenceQueue<T>()

  override val size: Int = delegate.size

  override fun isEmpty() = delegate.isEmpty()

  /**
   * Returns an iterator over the elements in this set.  The elements
   * are returned in no particular order.
   *
   * @return an Iterator over the elements in this set.
   */
  override fun iterator(): MutableIterator<T> {
    // remove garbage collected elements
    processQueue()

    // get an iterator of the superclass WeakHashSet
    val i = delegate.iterator()

    return object : MutableIterator<T> {
      override fun hasNext(): Boolean {
        return i.hasNext()
      }

      override fun next(): T {
        // unwrap the element
        return i.next().get()!!
      }

      override fun remove() {
        // remove the element from the HashSet
        i.remove()
      }
    }
  }

  /**
   * Returns `true` if this set contains the specified element.
   *
   * @param element element whose presence in this set is to be tested.
   * @return `true` if this set contains the specified element.
   */
  override fun contains(element: T): Boolean {
    return delegate.contains(WeakElement(element))
  }

  /**
   * Adds the specified element to this set if it is not already
   * present.
   *
   * @param element element to be added to this set.
   * @return `true` if the set did not already contain the specified
   * element.
   */
  override fun add(element: T): Boolean {
    processQueue()
    return delegate.add(WeakElement(element, this.queue))
  }

  /**
   * Removes the given element from this set if it is present.
   *
   * @param element object to be removed from this set, if present.
   * @return `true` if the set contained the specified element.
   */
  override fun remove(element: T): Boolean {
    val ret = delegate.remove(WeakElement(element))
    processQueue()
    return ret
  }

  override fun containsAll(elements: Collection<T>): Boolean {
    for (o in elements) {
      if (!contains(o)) {
        return false
      }
    }
    return true
  }

  override fun addAll(elements: Collection<T>): Boolean {
    for (o in elements) {
      if (!add(o)) {
        return false
      }
    }
    return true
  }

  override fun retainAll(elements: Collection<T>): Boolean {
    val itemsToRemove = HashSet<T>(elements.size)
    for (t in this) {
      if (!elements.contains(t)) {
        itemsToRemove.add(t)
      }
    }
    return if (itemsToRemove.isEmpty()) {
      false
    } else {
      for (t in itemsToRemove) {
        remove(t)
      }
      true
    }
  }

  override fun removeAll(elements: Collection<T>): Boolean {
    var changed = false
    for (o in elements) {
      if (remove(o)) {
        changed = true
      }
    }
    return changed
  }

  override fun clear() {

  }

  /**
   * Removes all garbage collected values with their keys from the map.
   * Since we don't know how much the ReferenceQueue.poll() operation
   * costs, we should call it only in the add() method.
   */
  private fun processQueue() {
    var wv: WeakElement<out T>?

    while (true) {
      wv = this.queue.poll() as? WeakElement<out T>
      if (wv == null) break
      delegate.remove(wv)
    }
  }

  /**
   * A WeakHashSet stores objects of class WeakElement.
   * A WeakElement wraps the element that should be stored in the WeakHashSet.
   * WeakElement inherits from java.lang.ref.WeakReference.
   * It redefines equals and hashCode which delegate to the corresponding methods
   * of the wrapped element.
   */
  private class WeakElement<T> : WeakReference<T> {
    /**
     * Hashcode of key, stored here since the key
     * may be tossed by the GC
     */
    private val hash: Int

    constructor(o: T) : super(o) {
      hash = o?.hashCode() ?: 0
    }

    constructor(o: T, q: ReferenceQueue<T>) : super(o, q) {
      hash = o?.hashCode() ?: 0
    }

    /**
     * A WeakElement is equal to another WeakElement iff they both refer to objects
     * that are, in turn, equal according to their own equals methods
     */
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is WeakElement<*>) return false
      val t = this.get()
      val u = other.get()
      if (t === u) return true
      return if (t == null || u == null) false else t == u
    }

    override fun hashCode(): Int {
      return hash
    }
  }
}
