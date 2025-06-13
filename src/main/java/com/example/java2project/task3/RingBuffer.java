package com.example.java2project.task3;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RingBuffer<T> {
    private final Object[] buffer;
    private int head = 0;
    private int tail = 0;
    private int count = 0;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    public RingBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("Размер должен быть > 0");
        this.buffer = new Object[capacity];
    }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (count == capacity()) {
                notFull.await();
            }

            buffer[head] = item;
            head = (head + 1) % capacity();
            count++;

            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await();
            }

            T item = (T) buffer[tail];
            tail = (tail + 1) % capacity();
            count--;

            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    public int capacity() {
        return buffer.length;
    }
}
