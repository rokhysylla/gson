/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson.internal;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * A map of comparable keys to values. Unlike {@code TreeMap}, this class uses insertion order for
 * iteration order. Comparison order is only used as an optimization for efficient insertion and
 * removal.
 *
 * <p>This implementation was derived from Android 4.1's TreeMap class.
 */
public final class LinkedTreeMap<K, V> extends AbstractMap<K, V> implements Serializable {

  private static final Comparator<Comparable> NATURAL_ORDER =
      new Comparator<Comparable>() {
        @Override
        public int compare(Comparable a, Comparable b) {
          return a.compareTo(b);
        }
      };
  private static final String NULL_KEY_MESSAGE = "key == null";
  private static final String NULL_VALUE_MESSAGE = "value == null";
  private final Comparator<? super K> comparator;
  private final boolean allowNullValues;
  Node<K, V> root;
  int size = 0;
  int modCount = 0;

  // Used to preserve iteration order
  final Node<K, V> header;

  /**
   * Create a natural order, empty tree map whose keys must be mutually comparable and non-null, and
   * whose values can be {@code null}.
   */
  @SuppressWarnings("unchecked") // unsafe! this assumes K is comparable
  public LinkedTreeMap() {
    this((Comparator<? super K>) NATURAL_ORDER, true);
  }

  /**
   * Create a natural order, empty tree map whose keys must be mutually comparable and non-null.
   *
   * @param allowNullValues whether {@code null} is allowed as entry value
   */
  @SuppressWarnings("unchecked") // unsafe! this assumes K is comparable
  public LinkedTreeMap(boolean allowNullValues) {
    this((Comparator<? super K>) NATURAL_ORDER, allowNullValues);
  }

  /**
   * Create a tree map ordered by {@code comparator}. This map's keys may only be null if {@code
   * comparator} permits.
   *
   * @param comparator the comparator to order elements with, or {@code null} to use the natural
   *     ordering.
   * @param allowNullValues whether {@code null} is allowed as entry value
   */
  // unsafe! if comparator is null, this assumes K is comparable
  @SuppressWarnings({"unchecked", "rawtypes"})
  public LinkedTreeMap(Comparator<? super K> comparator, boolean allowNullValues) {
    this.comparator = comparator != null ? comparator : (Comparator) NATURAL_ORDER;
    this.allowNullValues = allowNullValues;
    this.header = new Node<>(allowNullValues);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public V get(Object key) {
    Node<K, V> node = findByObject(key);
    return node != null ? node.value : null;
  }

  @Override
  public boolean containsKey(Object key) {
    return findByObject(key) != null;
  }

  @CanIgnoreReturnValue
  @Override
  public V put(K key, V value) {
    if (key == null) {
      throw new NullPointerException(NULL_KEY_MESSAGE);
    }
    if (value == null && !allowNullValues) {
      throw new NullPointerException(NULL_VALUE_MESSAGE);
    }
    Node<K, V> created = find(key, true);
    V result = created.value;
    created.value = value;
    return result;
  }

  @Override
  public void clear() {
    root = null;
    size = 0;
    modCount++;

    // Clear iteration order
    Node<K, V> header = this.header;
    header.next = header.prev = header;
  }

  @Override
  public V remove(Object key) {
    Node<K, V> node = removeInternalByKey(key);
    return node != null ? node.value : null;
  }

  /**
   * Returns the node at or adjacent to the given key, creating it if requested.
   *
   * @throws ClassCastException if {@code key} and the tree's keys aren't mutually comparable.
   */
  Node<K, V> find(K key, boolean create) {
    Comparator<? super K> comparator = this.comparator;
    Node<K, V> nearest = root;
    int comparison = 0;

    if (nearest != null) {
      // Micro-optimization: avoid polymorphic calls to Comparator.compare().
      @SuppressWarnings("unchecked") // Throws a ClassCastException below if there's trouble.
      Comparable<Object> comparableKey =
          (comparator == NATURAL_ORDER) ? (Comparable<Object>) key : null;

      while (true) {
        comparison =
            (comparableKey != null)
                ? comparableKey.compareTo(nearest.key)
                : comparator.compare(key, nearest.key);

        // We found the requested key.
        if (comparison == 0) {
          return nearest;
        }

        // If it exists, the key is in a subtree. Go deeper.
        Node<K, V> child = (comparison < 0) ? nearest.left : nearest.right;
        if (child == null) {
          break;
        }

        nearest = child;
      }
    }

    // The key doesn't exist in this tree.
    if (!create) {
      return null;
    }

    // Create the node and add it to the tree or the table.
    Node<K, V> header = this.header;
    Node<K, V> created;
    if (nearest == null) {
      // Check that the value is comparable if we didn't do any comparisons.
      if (comparator == NATURAL_ORDER && !(key instanceof Comparable)) {
        throw new ClassCastException(key.getClass().getName() + " is not Comparable");
      }
      created = new Node<>(allowNullValues, nearest, key, header, header.prev);
      root = created;
    } else {
      created = new Node<>(allowNullValues, nearest, key, header, header.prev);
      if (comparison < 0) { // nearest.key is higher
        nearest.left = created;
      } else { // comparison > 0, nearest.key is lower
        nearest.right = created;
      }
      rebalance(nearest, true);
    }
    size++;
    modCount++;

    return created;
  }

  @SuppressWarnings("unchecked")
  Node<K, V> findByObject(Object key) {
    try {
      return key != null ? find((K) key, false) : null;
    } catch (ClassCastException e) {
      return null;
    }
  }

  /**
   * Returns this map's entry that has the same key and value as {@code entry}, or null if this map
   * has no such entry.
   *
   * <p>This method uses the comparator for key equality rather than {@code equals}. If this map's
   * comparator isn't consistent with equals (such as {@code String.CASE_INSENSITIVE_ORDER}), then
   * {@code remove()} and {@code contains()} will violate the collections API.
   */
  Node<K, V> findByEntry(Entry<?, ?> entry) {
    Node<K, V> matchingNode = findByObject(entry.getKey());
    boolean valuesEqual = matchingNode != null && equal(matchingNode.value, entry.getValue());
    return valuesEqual ? matchingNode : null;
  }

  private static boolean equal(Object a, Object b) {
    return Objects.equals(a, b);
  }

  /**
   * Removes {@code node} from this tree, rearranging the tree's structure as necessary.
   *
   * @param unlink true to also unlink this node from the iteration linked list.
   */
  void removeInternal(Node<K, V> node, boolean unlink) {
    if (unlink) {
      node.prev.next = node.next;
      node.next.prev = node.prev;
    }

    Node<K, V> left = node.left;
    Node<K, V> right = node.right;
    Node<K, V> originalParent = node.parent;
    if (left != null && right != null) {

      /*
       * To remove a node with both left and right subtrees, move an
       * adjacent node from one of those subtrees into this node's place.
       *
       * Removing the adjacent node may change this node's subtrees. This
       * node may no longer have two subtrees once the adjacent node is
       * gone!
       */

      Node<K, V> adjacent = (left.height > right.height) ? left.last() : right.first();
      removeInternal(adjacent, false); // takes care of rebalance and size--

      int leftHeight = 0;
      left = node.left;
      if (left != null) {
        leftHeight = left.height;
        adjacent.left = left;
        left.parent = adjacent;
        node.left = null;
      }

      int rightHeight = 0;
      right = node.right;
      if (right != null) {
        rightHeight = right.height;
        adjacent.right = right;
        right.parent = adjacent;
        node.right = null;
      }

      adjacent.height = Math.max(leftHeight, rightHeight) + 1;
      replaceInParent(node, adjacent);
      return;
    } else if (left != null) {
      replaceInParent(node, left);
      node.left = null;
    } else if (right != null) {
      replaceInParent(node, right);
      node.right = null;
    } else {
      replaceInParent(node, null);
    }

    rebalance(originalParent, false);
    size--;
    modCount++;
  }

  Node<K, V> removeInternalByKey(Object key) {
    Node<K, V> node = findByObject(key);
    if (node != null) {
      removeInternal(node, true);
    }
    return node;
  }

  @SuppressWarnings("ReferenceEquality")
  private void replaceInParent(Node<K, V> node, Node<K, V> replacement) {
    Node<K, V> parent = node.parent;
    node.parent = null;
    if (replacement != null) {
      replacement.parent = parent;
    }

    if (parent != null) {
      if (parent.left == node) {
        parent.left = replacement;
      } else {
        assert parent.right == node;
        parent.right = replacement;
      }
    } else {
      root = replacement;
    }
  }

  /**
   * Rebalances the tree by performing any necessary AVL rotations
   * between the newly unbalanced node and the root.
   *
   * @param unbalanced the node from which rebalancing starts
   * @param insert {@code true} if the imbalance was caused by an insertion,
   *     {@code false} if it was caused by a removal
   */
  private void rebalance(Node<K, V> unbalanced, boolean insert) {
    for (Node<K, V> node = unbalanced; node != null; node = node.parent) {
      Node<K, V> left = node.left;
      Node<K, V> right = node.right;
      int leftHeight = height(left);
      int rightHeight = height(right);

      int delta = leftHeight - rightHeight;
      if (delta == -2) {
        rebalanceRightHeavy(node, insert);
        if (insert) {
          break;
        }
      } else if (delta == 2) {
        rebalanceLeftHeavy(node, insert);
        if (insert) {
          break;
        }
      } else if (delta == 0) {
        node.height = leftHeight + 1;
        if (insert) {
          break;
        }
      } else {
        assert (delta == -1 || delta == 1);
        node.height = Math.max(leftHeight, rightHeight) + 1;
        if (!insert) {
          break;
        }
      }
    }
  }

  /**
   * Returns the height of the given node, or {@code 0} if the node is {@code null}.
   *
   * @param node the node whose height is requested
   * @return the height of the node, or {@code 0} if null
   */
  private static <K, V> int height(Node<K, V> node) {
    return node != null ? node.height : 0;
  }

  /**
   * Rebalances a node that is heavier on the right side.
   *
   * @param node the unbalanced node
   * @param insert {@code true} if the imbalance was caused by an insertion
   */
  private void rebalanceRightHeavy(Node<K, V> node, boolean insert) {
    Node<K, V> right = node.right;
    Node<K, V> rightLeft = right.left;
    Node<K, V> rightRight = right.right;

    int rightDelta = height(rightLeft) - height(rightRight);
    if (rightDelta == -1 || (rightDelta == 0 && !insert)) {
      rotateLeft(node); // AVL right-right case
    } else {
      assert (rightDelta == 1);
      rotateRight(right); // AVL right-left case
      rotateLeft(node);
    }
  }

  /**
   * Rebalances a node that is heavier on the left side.
   *
   * @param node the unbalanced node
   * @param insert {@code true} if the imbalance was caused by an insertion
   */
  private void rebalanceLeftHeavy(Node<K, V> node, boolean insert) {
    Node<K, V> left = node.left;
    Node<K, V> leftLeft = left.left;
    Node<K, V> leftRight = left.right;

    int leftDelta = height(leftLeft) - height(leftRight);
    if (leftDelta == 1 || (leftDelta == 0 && !insert)) {
      rotateRight(node);
    } else {
      assert (leftDelta == -1);
      rotateLeft(left);
      rotateRight(node);
    }
  }

  /** Rotates the subtree so that its root's right child is the new root. */
  private void rotateLeft(Node<K, V> root) {
    Node<K, V> left = root.left;
    Node<K, V> pivot = root.right;
    Node<K, V> pivotLeft = pivot.left;
    Node<K, V> pivotRight = pivot.right;

    // move the pivot's left child to the root's right
    root.right = pivotLeft;
    if (pivotLeft != null) {
      pivotLeft.parent = root;
    }

    replaceInParent(root, pivot);

    // move the root to the pivot's left
    pivot.left = root;
    root.parent = pivot;

    // fix heights
    root.height =
        Math.max(left != null ? left.height : 0, pivotLeft != null ? pivotLeft.height : 0) + 1;
    pivot.height = Math.max(root.height, pivotRight != null ? pivotRight.height : 0) + 1;
  }

  /** Rotates the subtree so that its root's left child is the new root. */
  private void rotateRight(Node<K, V> root) {
    Node<K, V> pivot = root.left;
    Node<K, V> right = root.right;
    Node<K, V> pivotLeft = pivot.left;
    Node<K, V> pivotRight = pivot.right;

    // move the pivot's right child to the root's left
    root.left = pivotRight;
    if (pivotRight != null) {
      pivotRight.parent = root;
    }

    replaceInParent(root, pivot);

    // move the root to the pivot's right
    pivot.right = root;
    root.parent = pivot;

    // fixup heights
    root.height =
        Math.max(right != null ? right.height : 0, pivotRight != null ? pivotRight.height : 0) + 1;
    pivot.height = Math.max(root.height, pivotLeft != null ? pivotLeft.height : 0) + 1;
  }

  private EntrySet entrySet;
  private KeySet keySet;

  @Override
  public Set<Entry<K, V>> entrySet() {
    EntrySet result = entrySet;
    if (result == null) {
      result = entrySet = new EntrySet();
    }
    return result;
  }

  @Override
  public Set<K> keySet() {
    KeySet result = keySet;
    if (result == null) {
      result = keySet = new KeySet();
    }
    return result;
  }


  class EntrySet extends AbstractSet<Entry<K, V>> {
    @Override
    public int size() {
      return size;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new LinkedTreeMapIterator<Entry<K, V>>() {
        @Override
        public Entry<K, V> next() {
          return nextNode();
        }
      };
    }

    @Override
    public boolean contains(Object o) {
      return o instanceof Entry && findByEntry((Entry<?, ?>) o) != null;
    }

    @Override
    public boolean remove(Object o) {
      if (!(o instanceof Entry)) {
        return false;
      }

      Node<K, V> node = findByEntry((Entry<?, ?>) o);
      if (node == null) {
        return false;
      }
      removeInternal(node, true);
      return true;
    }

    @Override
    public void clear() {
      LinkedTreeMap.this.clear();
    }
  }

  final class KeySet extends AbstractSet<K> {
    @Override
    public int size() {
      return size;
    }

    @Override
    public Iterator<K> iterator() {
      return new LinkedTreeMapIterator<K>() {
        @Override
        public K next() {
          return nextNode().key;
        }
      };
    }

    @Override
    public boolean contains(Object o) {
      return containsKey(o);
    }

    @Override
    public boolean remove(Object key) {
      return removeInternalByKey(key) != null;
    }

    @Override
    public void clear() {
      LinkedTreeMap.this.clear();
    }
  }

  /**
   * If somebody is unlucky enough to have to serialize one of these, serialize it as a
   * LinkedHashMap so that they won't need Gson on the other side to deserialize it. Using
   * serialization defeats our DoS defence, so most apps shouldn't use it.
   */
  private Object writeReplace() throws ObjectStreamException {
    return new LinkedHashMap<>(this);
  }

  private void readObject(ObjectInputStream in) throws IOException {
    // Don't permit directly deserializing this class; writeReplace() should have written a
    // replacement
    throw new InvalidObjectException("Deserialization is unsupported");
  }


static final class Node<K, V> implements Entry<K, V> {
    Node<K, V> parent;
    Node<K, V> left;
    Node<K, V> right;
    Node<K, V> next;
    Node<K, V> prev;
    final K key;
    final boolean allowNullValue;
    V value;
    int height;

    /** Create the header entry */
    Node(boolean allowNullValue) {
      key = null;
      this.allowNullValue = allowNullValue;
      next = prev = this;
    }

    /** Create a regular entry */
    Node(boolean allowNullValue, Node<K, V> parent, K key, Node<K, V> next, Node<K, V> prev) {
      this.parent = parent;
      this.key = key;
      this.allowNullValue = allowNullValue;
      this.height = 1;
      this.next = next;
      this.prev = prev;
      prev.next = this;
      next.prev = this;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V value) {
      if (value == null && !allowNullValue) {
        throw new NullPointerException("value == null");
      }
      V oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Entry) {
        Entry<?, ?> other = (Entry<?, ?>) o;
        return (key == null ? other.getKey() == null : key.equals(other.getKey()))
            && (value == null ? other.getValue() == null : value.equals(other.getValue()));
      }
      return false;
    }

    @Override
    public int hashCode() {
      return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
      return key + "=" + value;
    }

    /** Returns the first node in this subtree. */
    public Node<K, V> first() {
      Node<K, V> node = this;
      Node<K, V> child = node.left;
      while (child != null) {
        node = child;
        child = node.left;
      }
      return node;
    }

    /** Returns the last node in this subtree. */
    public Node<K, V> last() {
      Node<K, V> node = this;
      Node<K, V> child = node.right;
      while (child != null) {
        node = child;
        child = node.right;
      }
      return node;
    }
  }

  private abstract class LinkedTreeMapIterator<T> implements Iterator<T> {
    Node<K, V> next = header.next;
    Node<K, V> lastReturned = null;
    int expectedModCount = modCount;

    LinkedTreeMapIterator() {}

    @Override
    @SuppressWarnings("ReferenceEquality")
    public final boolean hasNext() {
      return next != header;
    }

    @SuppressWarnings("ReferenceEquality")
    final Node<K, V> nextNode() {
      Node<K, V> nextNode = next;
      if (nextNode == header) {
        throw new NoSuchElementException();
      }
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
      next = nextNode.next;
      lastReturned = nextNode;
      return nextNode;
    }

    @Override
    public final void remove() {
      if (lastReturned == null) {
        throw new IllegalStateException();
      }
      removeInternal(lastReturned, true);
      lastReturned = null;
      expectedModCount = modCount;
    }
  }
}