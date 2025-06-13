package com.example.java2project.task1;

import java.util.concurrent.Semaphore;

public class NumbersPrinter {

    private static final Semaphore evenSemaphore = new Semaphore(1);
    private static final Semaphore oddSemaphore = new Semaphore(0);

    public static void main(String[] args) {
        new Thread(() -> {
            for (int i = 0; i <= 100; i += 2) {
                try {
                    evenSemaphore.acquire();
                    System.out.println(i);
                    oddSemaphore.release();
                } catch (InterruptedException e) {}
            }
        }).start();

        new Thread(() -> {
            for (int i = 1; i <= 99; i += 2) {
                try {
                    oddSemaphore.acquire();
                    System.out.println(i);
                    evenSemaphore.release();
                } catch (InterruptedException e) {}
            }
        }).start();
    }
}
