package com.kamikazejam.test;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockExample {
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public static void main(String[] args) {
        Thread writer1 = new Thread(() -> {
            lock.writeLock().lock(); // Thread writer1 acquires the write lock
            try {
                System.out.println("Writer1 is writing...");
                Thread.sleep(2000); // Simulating some writing operation
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.writeLock().unlock(); // Thread writer1 releases the write lock
                System.out.println("Writer1 finished writing.");
            }
        });

        Thread writer2 = new Thread(() -> {
            lock.writeLock().lock(); // Thread writer2 attempts to acquire the write lock
            try {
                System.out.println("Writer2 is writing...");
                Thread.sleep(2000); // Simulating some writing operation
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.writeLock().unlock(); // Thread writer2 releases the write lock
                System.out.println("Writer2 finished writing.");
            }
        });

        writer1.start();
        try {
            Thread.sleep(1000); // Wait for writer1 to acquire and hold the write lock
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        writer2.start(); // Start writer2 while writer1 is holding the write lock
    }
}

