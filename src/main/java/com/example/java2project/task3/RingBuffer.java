package com.example.java2project.task3;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RingBuffer<T> implements Closeable {
    private final Object[] buffer;
    private int head = 0;
    private int tail = 0;
    private int count = 0;
    private volatile boolean closed = false;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    public RingBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("Size must be > 0");
        this.buffer = new Object[capacity];
    }

    public void put(T item) throws InterruptedException, IOException {
        if (item == null){
            throw new NullPointerException("You can't add null to a buffer");
        }
        lock.lock();
        try {
            if (count >= capacity()) {
                throw new IllegalStateException("Buffer full");
            }

            if (closed) throw new IOException("Buffer closed");
            buffer[head] = item;
            head = (head + 1) % capacity();
            count++;
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException, IOException {
        lock.lock();
        try {
            while (count == 0) {
                if (closed) throw new IOException("Buffer closed");
                notEmpty.await();
            }

            T item = (T) buffer[tail];
            buffer[tail] = null;
            tail = (tail + 1) % capacity();
            count--;

            notFull.signalAll();
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

    @Override
    public void close(){
        lock.lock();
        try {
            closed = true;
            notEmpty.signalAll();
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            for (int i = 0; i < capacity(); i++) {
                buffer[i] = null;
            }
            head = 0;
            tail = 0;
            count = 0;
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return count == 0;
        } finally {
            lock.unlock();
        }
    }
}
