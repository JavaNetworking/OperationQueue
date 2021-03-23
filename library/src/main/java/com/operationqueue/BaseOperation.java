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

/**
 The base operation which can be sub-classed. All the basic operations
 attributes such as current running state, should be set in this class.
 */
public class BaseOperation implements Operation, Runnable {

    /**
     A {@link OperationState} value indicating the operations
     current operation state.
     */
    private OperationState state = null;

    /**
     Holds the operation error if any, or null if no error occurred.
     */
    private Throwable throwable;

    /**
     Basic constructor, sets the {@link OperationState} to {@link OperationState.Created}.
     */
    public BaseOperation() {
        setState(OperationState.Created);
    }

    /**
     Sets this operations state.

     @param state A {@link OperationState} value indicating the operation state to set.
     */
    public synchronized void setState(OperationState state) {
        this.state = state;
    }

    /**
     Gets this operations {@link OperationState}.

     @return A {@link OperationState} value indicating the current operation state.
     */
    public synchronized OperationState getState() {
        return state;
    }

    /**
     Set the current {@link Throwable} for this operation.
     */
    private synchronized void setThrowable(Throwable t) {
        this.throwable = t;
    }

    /**
     Gets the current {@link Throwable} if any.

     @return A {@link Throwable} with information about an error, or null if no error occurred.
     */
    public synchronized Throwable getThrowable() {
        return this.throwable;
    }

    /**
     The execution method where executing code should be placed.
     This method is called in a separate thread on the {@link OperationQueue} queue which
     holds this {@link Operation}.
     */
    public synchronized void execute() {
        setState(OperationState.Running);
    }

    /**
     The completion method which is called after the {@code execute()} method is called.
     */
    public synchronized void complete() {
        setState(OperationState.Finished);
    }

    /**
     The failure method is called when an exception occurs on working thread.
     */
    public synchronized void failure(Throwable t) {
        setState(OperationState.Failed);
        setThrowable(t);
    }

    @Override
    public void run() {
        try {
            this.setState(OperationState.Running);
            this.execute();
            this.setState(OperationState.Finished);
            this.complete();
        } catch (Throwable t) {
            this.failure(t);
            this.setState(OperationState.Cancelled);
        }
    }
}
