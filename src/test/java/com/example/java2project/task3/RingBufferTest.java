package com.example.java2project.task3;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class RingBufferTest {

    @Test
    void testBasicPutTake() {
        try (RingBuffer<Integer> buffer = new RingBuffer<>(2)){
            buffer.put(1);
            buffer.put(2);

            assertEquals(1, buffer.take());
            assertEquals(2, buffer.take());
            assertTrue(buffer.isEmpty());
        } catch (InterruptedException | IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void testClear() {
        try (RingBuffer<Integer> buffer = new RingBuffer<>(3)){
            buffer.put(1);
            buffer.put(2);
            buffer.put(3);
            assertEquals(3, buffer.size());

            buffer.clear();
            assertEquals(0, buffer.size());
            buffer.put(5);
            assertEquals(5, buffer.take());
        } catch (InterruptedException | IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void testClose() {
        RingBuffer<Integer> buffer = new RingBuffer<>(3);
        buffer.close();
        assertThrows(IOException.class, () -> buffer.put(1));
        assertThrows(IOException.class, buffer::take);
    }

    @Test
    void testPutNull() {
        try (RingBuffer<String> buffer = new RingBuffer<>(2)){
            assertThrows(NullPointerException.class, () -> buffer.put(null));
        }
    }

    @Test
    void testPutMoreThanBuffer() {
        try (RingBuffer<Integer> buffer = new RingBuffer<>(2)) {
            buffer.put(1);
            buffer.put(2);
            assertThrows(IllegalStateException.class, () -> buffer.put(3));
        } catch (InterruptedException | IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void testMultiThread() {
        try (RingBuffer<Integer> ringBuffer = new RingBuffer<>(5)){
            ExecutorService executorService = Executors.newFixedThreadPool(4);
            AtomicInteger valueProd = new AtomicInteger();
            AtomicInteger valueCons = new AtomicInteger();

            Runnable producer = () -> {
                try {
                    for (int i = 0; i < 20; i++) {
                        ringBuffer.put(valueProd.incrementAndGet());
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    fail("Producer failed: " + e.getMessage());
                }
            };

            Runnable consumer = () -> {
                try {
                    for (int i = 0; i < 20; i++) {
                        Integer val = ringBuffer.take();
                        assertNotNull(val);
                        valueCons.incrementAndGet();
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    fail("Consumer failed: " + e.getMessage());
                }
            };

            executorService.submit(producer);
            executorService.submit(producer);
            executorService.submit(consumer);
            executorService.submit(consumer);

            executorService.shutdown();
            boolean terminated = executorService.awaitTermination(5, TimeUnit.SECONDS);
            assertTrue(terminated, "The streams did not complete on time");

            assertEquals(40, valueCons.get());
            assertTrue(ringBuffer.isEmpty());
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}
