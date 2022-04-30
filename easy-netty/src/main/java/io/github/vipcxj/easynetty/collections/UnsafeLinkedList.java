package io.github.vipcxj.easynetty.collections;

import java.util.Iterator;

/**
 * At first, head and tail is null.
 * List is empty if and only if head and tail is null.
 * head is null if and only if tail is null.
 * tail is null if and only if head is null.
 * head equals to tail if and only if list has zero or one element.
 * @param <T>
 */
public class UnsafeLinkedList<T> {

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public UnsafeLinkedList() {
        this.head = this.tail = null;
        this.size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getSize() {
        return size;
    }

    public void addLast(T v) {
        if (this.tail == null) {
            this.head = this.tail = new Node<>(v);
        } else {
            this.tail.next = new Node<>(v);
            this.tail = this.tail.next;
        }
        ++size;
    }

    public void removeFirst() {
        if (this.head == null) {
            throw new IllegalStateException("The list is empty, unable to remove first.");
        }
        if (this.head == this.tail) {
            this.head = this.tail = null;
        } else {
            this.head = this.head.next;
        }
        --size;
    }

    public void clear() {
        this.head = this.tail = null;
        size = 0;
    }

    public Node<T> getHead() {
        return head;
    }

    public Node<T> getTail() {
        return tail;
    }

    public static class Node<T> {
        private final T data;
        private Node<T> next;

        Node(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }

        public Node<T> getNext() {
            return next;
        }
    }
}
