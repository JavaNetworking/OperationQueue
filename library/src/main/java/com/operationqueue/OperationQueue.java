// Copyright (c) 2014 JavaNetworking (https://github.com/JavaNetworking/OperationQueue)
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.operationqueue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import com.operationqueue.Operation.OperationState;
import java.util.ArrayList;

/**
 The {@link OperationsQueue} class handles the execution of {@link Operation} instances
 through queues and executes them one by one in a separate working thread.
 */
public class OperationQueue {

    /**
     A string value that holds the main queue key identifier name.
     */
    private final String MAIN_QUEUE_KEY = "main_queue";

    /**
     A {@link HashMap} instance which holds the operation queues for current {@link OperationQueue} instance.
     */
    private Map<String, BlockingQueue<BaseOperation>> queues;

    /**
     A {@link HashMap} instance which holds the {@link BlockingQueue} execution thread pools.

     Every {@link BlockingQueue} gets its own thread pool with the same key identifier name which their operations
     are executed on.
     */
    private Map<String, ExecutorService> queueThreadPools;

    /**
     A {@link HashMap} instance which holds the {@link Future}s.

     When all futures have finished executing, the thread pool return true on isDone.
     */
    private Map<String, List<Future>> queueFutures;

    //-------------------------------------------------------
    // @name Public methods, {@link OperationQueue} interface
    //-------------------------------------------------------

    private String currentQueue;

    /**
     Default constructor
     */
    public OperationQueue() {
        this.currentQueue = MAIN_QUEUE_KEY;
    }

    public OperationQueue(String queueName) {
        this.currentQueue = queueName;
    }

    /**
     Cancel all waiting operations and clear operations queue. This method sets the running
     status to false, interrupts the current execution thread which throws an {@link InterruptedException}
     and exits before finally clearing the main queue for waiting operations.
     */
    public void cancelAllOperations() {
        Set<String> futureQueueKeys = queueFutures.keySet();
        for (String key : futureQueueKeys) {
            List<Future> fs = queueFutures.get(key);
            for (Future f : fs) {
                f.cancel(true);
            }
        }

        Set<String> queueKeys = getQueues().keySet();
        for (String key : queueKeys) {
            BlockingQueue<BaseOperation> queue = getQueue(key);
            queue.clear();
        }
    }

    /**
     Add {@link Operation} instance to the {@link OperationQueue} instance main queue. The operations
     is offered to the main queue but can be rejected and not added to the queue. When
     rejected the {@link Operation} has the state of {@link OperationState.Rejected}. If the {@link Operation}
     is added to the queue the state is set to {@link OperationState.InQueue}.

     It the {@link OperationQueue}s running status is false. The execution thread is started and the running
     status is updated to true.

     @param operation The {@link Operation} instance which is added to the queue.
     */
    public void addOperation(BaseOperation operation) {
        this.addOperationToQueueNamed(currentQueue, operation);
    }

    /**
     Adds an operation to a queue referenced by {@param key}.

     If the queue which is referenced by the {@param key} does not exist. The queue
     and a linked thread is created and the {@param operation} is added to the new
     queue.

     @param key A string value that is the key identifier name of a Queue.
     @param operation The {@link Operation} instance which is added to the queue.
     */
    private void addOperationToQueueNamed(String key, BaseOperation operation) {

        // Offer the operation to the operation queue
        BlockingQueue<BaseOperation> queue = getQueue(key);
        if (queue.offer(operation)) {
            operation.setState(OperationState.InQueue);

            runThreadsInThreadPool(key);
        } else {
            operation.setState(OperationState.Rejected);
        }
    }

    /**
     Adds a {@link List} of instantiated {@link Operation} objects. The methods loops over the list
     and calls the {@code addOperation(Operation operation)} method.

     @param operations An instantiated {@link List} of instantiated {@link Operation} objects which is
            added to the {@link OperationQueue}s main queue.
     */
    public void addOperations(List<BaseOperation> operations) {
        for (BaseOperation operation : operations) {
            this.addOperation(operation);
        }
    }

    /**
     Check if the {@link OperationQueue}s main queue is empty.

     @return A boolean value indicating if the {@link OperationQueue} is empty. If true the {@link OperationQueue}
             has no waiting operations to execute.
     */
    public boolean isEmpty() {
        return this.isEmpty(currentQueue);
    }

    /**
     Check if the {@link OperationQueue}s queue named {@param key} is empty.

     @param key A string value representing a queue key identifier name.
     @return A boolean value indicating if the {@link OperationQueue} is empty. If true the {@link OperationQueue}
             has no waiting operations to execute.
     */
    public boolean isEmpty(String key) {
        return isDone(key);
    }

    public boolean isDone() {
        return isDone(currentQueue);
    }

    public boolean isDone(String key) {
        boolean status = true;

        List<Future> flist = queueFutures.get(key);
        for (Future f : flist) {
            if (!f.isDone()) {
                status = false;
            }
        }
        return status;
    }

    //----------------------------------------------
    // @name Queue handling
    //----------------------------------------------

    /**
     Get a {@link BlockingQueue} by the key identifier name.

     If the queue references by {@param key} is null a new queue is instantiated.

     @param key A string value that is the key identifier name of the queue.

     @return A {@link BlockingQueue} instance which holds {@link Operation} instances.
     */
    private synchronized BlockingQueue<BaseOperation> getQueue(String key) {
        BlockingQueue<BaseOperation> queue = getQueues().get(key);
        if (queue == null) {
            queue = newQueueForKey(key);
            queueFutures = new HashMap<String, List<Future>>();
            queueFutures.put(key, new ArrayList<Future>());
        }
        return queue;
    }

    /**
     Gets the queues {@link HashMap} which holds all the operation queues.

     If the queues reference is null a new {@link HashMap} is instantiated.

     @return A {@link Map} instance which holds the operation queues of this class.
     */
    private Map<String, BlockingQueue<BaseOperation>> getQueues() {
        if (this.queues == null) {
            this.queues = new HashMap<String, BlockingQueue<BaseOperation>>();
        }
        return this.queues;
    }

    /**
     Adds a new queue and worker thread with key identifier name {@param key}.

     @param key A string key identifier name which the queue and thread is identified by.

     @return A {@link BlockingQueue} instance which can hold {@link Operation} instances.
     */
    private BlockingQueue<BaseOperation> newQueueForKey(String key) {

        BlockingQueue<BaseOperation> queue = new LinkedBlockingQueue<BaseOperation>();
        getQueues().put(key, queue);

        return queue;
    }


    //----------------------------------------------
    // @name Thread pool and operation handling
    //----------------------------------------------

    /**
     Gets the {@link OperationQueue}s thread {@link HashMap} that holds all the
     worker threads.

     If the thread pools map is null an new {@link HashMap} instance is created.

     @return A {@link Map} instance that holds the worker threads.
     */
    private Map<String, ExecutorService> getThreadPools() {
        if (this.queueThreadPools == null) {
            this.queueThreadPools = new HashMap<String, ExecutorService>();
        }
        return this.queueThreadPools;
    }

    /**
     Put thread into thread {@link HashMap}.

     @param key String value representing the {@link HashMap} key to use.
     @param tp ExecutorService to store for key.
     */
    private void putThreadPoolForKey(String key, ExecutorService tp) {
        getThreadPools().put(key, tp);
    }

    /**
     Get thread for key from {@link HashMap}.

     If thread not found a new thread is created for key.

     @param key String value representing the {@link HashMap} key to use.

     @return A thread pool instance from key.
     */
    private ExecutorService getThreadPoolForKey(String key) {
        ExecutorService t = getThreadPools().get(key);
        if (t == null) {
            t = Executors.newFixedThreadPool(10); //newThreadPoolForQueueKey(key);
            putThreadPoolForKey(key, t);
        }

        return t;
    }

    private void runThreadsInThreadPool(final String key) {
        ExecutorService tp = getThreadPoolForKey(key);

        try {
            while (!getQueue(key).isEmpty()) {
                Future f = tp.submit(getQueue(key).take());

                queueFutures.get(key).add(f);
            }
        } catch (InterruptedException ex) {
            // tp.shutdown();
        }
    }

    /**
     Removes thread pool from thread {@link HashMap}.

     @param key String value representing the {@link HashMap} key to use.
     */
//    private void removeThreadForKey(String key) throws InterruptedException {
//        ExecutorService tp = getThreadPools().remove(key);
//        if (tp != null) {
//            tp.shutdown();
//            tp.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//        }
//    }

    /**
     Creates a new worker thread pool for the {@param key} identifier name.

     @param key A string value that identifies the {@link BlockingQueue} queue the
                thread pool should take its {@link Operation}s from.

     @return A {@link ExecutorService} instance which executes {@link Operation} instances.
     */
//    private ExecutorService newThreadPoolForQueueKey(final String key) {
//        return Executors.newFixedThreadPool(10);
//    }

}
