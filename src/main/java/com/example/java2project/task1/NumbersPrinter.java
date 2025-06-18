package com.example.java2project.task1;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NumbersPrinter {

    private static final Object lock = new Object();
    private static int num = 0;
    private static final int MAX = 100;

    public static void main(String[] args) throws InterruptedException {
        Thread evenThread = new Thread(()-> {
            while (true) {
                synchronized (lock) {
                    while (num<= MAX && num % 2 != 0) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn(e.getMessage());
                            return;
                        }
                    }
                    if (num > MAX) {
                        lock.notify();
                        break;
                    }
                    System.out.println(num++);
                    lock.notify();
                }
            }
        });

        Thread oddThread = new Thread(()-> {
            while (true) {
                synchronized (lock) {
                    while (num<= MAX && num % 2 == 0) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn(e.getMessage());
                            return;
                        }
                    }
                    if (num > MAX) {
                        lock.notify();
                        break;
                    }
                    System.out.println(num++);
                    lock.notify();
                }
            }
        });

        evenThread.start();
        oddThread.start();
        evenThread.join();
        oddThread.join();
    }
}
