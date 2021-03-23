package com.operationqueue;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;


public class OperationQueueTest {

    @Test
    public void testOperationQueue() {
        final CountDownLatch signal = new CountDownLatch(1);

        // Setup OperationQueue with one base operation
        OperationQueue operationQueue = new OperationQueue();
        operationQueue.addOperation(new BaseOperation() {
            @Override
            public synchronized void execute() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public synchronized void complete() {
                signal.countDown();
            }
        });

        // Test if empty
        assertFalse(operationQueue.isEmpty());

        // Wait for signal
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(operationQueue.isEmpty());
    }

    @Test
    public void testOperationQueueNamed() {
        final CountDownLatch signal = new CountDownLatch(1);

        // Setup OperationQueue with one base operation
        OperationQueue operationQueue = new OperationQueue("another_queue");
        operationQueue.addOperation(new BaseOperation() {
            @Override
            public synchronized void execute() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public synchronized void complete() {
                signal.countDown();
            }
        });

        // Test if empty
        assertFalse(operationQueue.isEmpty());

        // Wait for signal
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(operationQueue.isEmpty());
    }
}