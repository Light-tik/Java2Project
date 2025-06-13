package com.example.java2project.task3;

import java.util.concurrent.atomic.AtomicInteger;

public class RingBufferTest {

    public static void main(String[] args) {
        RingBuffer<Integer> ringBuffer = new RingBuffer<>(5);

        AtomicInteger value = new AtomicInteger();

        Runnable producer = () -> {
            try {
                for (int i = 0; i < 20; i++) {
                    int val = value.incrementAndGet();
                    ringBuffer.put(val);
                    System.out.println("Производитель " + Thread.currentThread().getId() + " положил: " + val);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Runnable consumer = () -> {
            try {
                for (int i = 0; i < 15; i++) {
                    int val = ringBuffer.take();
                    System.out.println("Потребитель " + Thread.currentThread().getId() + " взял: " + val);
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        new Thread(producer).start();
        new Thread(producer).start();
        new Thread(consumer).start();
        new Thread(consumer).start();
    }
}
