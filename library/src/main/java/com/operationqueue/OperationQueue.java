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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.operationqueue.Operation.OperationState;

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
	private Map<String, BlockingQueue<Operation>> queues;

	/**
	 A {@link HashMap} instance which holds the {@link BlockingQueue} execution threads.

	 Every {@link BlockingQueue} gets its own thread with the same key identifier name which their operations
	 are executed on.
	 */
	private Map<String, Thread> queueThreads;

	/**
	 A {@link HashMap} instance which holds the worker thread statuses.

	 When a queue is set to false the worker thread exits after the next operation is finished.
	 */
	private Map<String, Boolean> runningStatuses;


	//-------------------------------------------------------
	// @name Public methods, {@link OperationQueue} interface
	//-------------------------------------------------------

	/**
	 Default constructor
	 */
	public OperationQueue() {}

	/**
	 Cancel all waiting operations and clear operations queue. This method sets the running
	 status to false, interrupts the current execution thread which throws an {@link InterruptedException}
	 and exits before finally clearing the main queue for waiting operations.
	 */
	public void cancelAllOperations() {

		Set<String> statusKeys = getRunningStatuses().keySet();
		for (String key : statusKeys) {
			setRunningStatus(key, false);
		}

		Set<String> threadKeys = getThreads().keySet();
		for (String key : threadKeys) {
			Thread t = getThreads().get(key);
			t.interrupt();
		}

		Set<String> queueKeys = getQueues().keySet();
		for (String key : queueKeys) {
			BlockingQueue<Operation> queue = getQueue(key);
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
	public void addOperation(Operation operation) {
		this.addOperationToQueueNamed(MAIN_QUEUE_KEY, operation);
	}

	/**
	 Adds an operation to a queue referenced by {@param key}.

	 If the queue which is referenced by the {@param key} does not exist. The queue
	 and a linked thread is created and the {@param operation} is added to the new
	 queue.

	 @param key A string value that is the key identifier name of a Queue.
	 @param operation The {@link Operation} instance which is added to the queue.
	 */
	public void addOperationToQueueNamed(String key, Operation operation) {

		// Offer the operation to the operation queue
		BlockingQueue<Operation> queue = getQueue(key);
		if (queue.offer(operation)) {
			operation.setState(OperationState.InQueue);
		} else {
			operation.setState(OperationState.Rejected);
		}

		// If current running status is false then start the working thread
		Thread thread = getThreads().get(key);
		if (getRunningStatus(key) != true && !thread.isAlive()) {
			thread.start();
		}
	}

	/**
	 Adds a {@link List} of instantiated {@link Operation} objects. The methods loops over the list
	 and calls the {@code addOperation(Operation operation)} method.

	 @param operations An instantiated {@link List} of instantiated {@link Operation} objects which is
	 		added to the {@link OperationQueue}s main queue.
	 */
	public void addOperations(List<Operation> operations) {
		for (Operation operation : operations) {
			this.addOperation(operation);
		}
	}

	/**
	 Check if the {@link OperationQueue}s main queue is empty.

	 @return A boolean value indicating if the {@link OperationQueue} is empty. If true the {@link OperationQueue}
	 		 has no waiting operations to execute.
	 */
	public boolean isEmpty() {
		return this.isEmpty(MAIN_QUEUE_KEY);
	}

	/**
	 Check if the {@link OperationQueue}s queue named {@param key} is empty.

	 @param key A string value representing a queue key identifier name.
	 @return A boolean value indicating if the {@link OperationQueue} is empty. If true the {@link OperationQueue}
	 		 has no waiting operations to execute.
	 */
	public boolean isEmpty(String key) {
		return getQueue(key).isEmpty();
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
	private synchronized BlockingQueue<Operation> getQueue(String key) {
		BlockingQueue<Operation> queue = getQueues().get(key);
		if (queue == null) {
			queue = newQueueForKey(key);
		}
		return queue;
	}

	/**
	 Gets the queues {@link HashMap} which holds all the operation queues.

	 If the queues reference is null a new {@link HashMap} is instantiated.

	 @return A {@link Map} instance which holds the operation queues of this class.
	 */
	private Map<String, BlockingQueue<Operation>> getQueues() {
		if (this.queues == null) {
			this.queues = new HashMap<String, BlockingQueue<Operation>>();
		}
		return this.queues;
	}

	/**
	 Adds a new queue and worker thread with key identifier name {@param key}.

	 @param key A string key identifier name which the queue and thread is identified by.

	 @return A {@link BlockingQueue} instance which can hold {@link Operation} instances.
	 */
	private BlockingQueue<Operation> newQueueForKey(String key) {

		BlockingQueue<Operation> queue = new LinkedBlockingQueue<Operation>();
		getQueues().put(key, queue);

		Thread t = newThreadForQueueKey(key);
		getThreads().put(key, t);

		return queue;
	}


	//----------------------------------------------
	// @name Thread and operation handling
	//----------------------------------------------

	/**
	 Gets the {@link OperationQueue}s thread {@link HashMap} that holds all the
	 worker threads.

	 If the threads map is null an new {@link HashMap} instance is created.

	 @return A {@link Map} instance that holds the worker threads.
	 */
	private Map<String, Thread> getThreads() {
		if (this.queueThreads == null) {
			this.queueThreads = new HashMap<String, Thread>();
		}
		return this.queueThreads;
	}

	/**
	 Creates a new worker thread for the {@param key} identifier name.

	 @param key A string value that identifies the {@link BlockingQueue} queue the
	 			thread should take its {@link Operation}s from.

	 @return A thread instance which executes {@link Operation} instances.
	 */
	private Thread newThreadForQueueKey(final String key) {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				setRunningStatus(key, true);

				BlockingQueue<Operation> queue = getQueue(key);

				while (getRunningStatus(key)) {
					try {
						Operation operation = null;
						try {
							operation = queue.take();
							operation.setState(OperationState.Running);
							operation.execute();
							operation.setState(OperationState.Finished);
						} catch (Throwable t) {
							operation.setState(OperationState.Cancelled);
						}
						operation.complete();
					} catch (Throwable t) {}


					if (queue.isEmpty()) {
						setRunningStatus(key, false);
					}
				}
			}
		});
	}


	//----------------------------------------------
	// @name Working thread status handling
	//----------------------------------------------

	/**
	 Get the {@link HashMap} which holds the running statuses.

	 @return A {@link HashMap} instance that holds the boolean values
	 */
	private Map<String, Boolean> getRunningStatuses() {
		if (this.runningStatuses == null) {
			this.runningStatuses = new HashMap<String, Boolean>();
		}
		return this.runningStatuses;
	}

	/**
	 Set the running status of execution threads.

	 @param key A string key identifier name representing operation queue name.
	 @param status A boolean value indicating if the running status should be true or false.
	 			   If false the execution threads will exit after the next operations finishes.
	 */
	private synchronized void setRunningStatus(String key, boolean status) {
		getRunningStatuses().put(key, status);
	}

	/**
	 Return the running status boolean value.

	 If the running status is set to false, the working thread may be waiting for a new operation
	 and not exit. It maybe waiting until it is interrupted/cancelled ({@see cancelAllOperations()}).

	 @param key A string key identifier name representing operation queue name.
	 @return A boolean value indicating the running status of the execution thread identified.
	 */
	private synchronized boolean getRunningStatus(String key) {
		Boolean status = getRunningStatuses().get(key);
		if (status == null) {
			return false;
		}

		return status;
	}
}
