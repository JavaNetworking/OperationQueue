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
 Public interface Operation defines the exection interface for {@link OperationQueue}.
 */
public interface Operation {
    /**
     The {@link Operation} states.
     */
    enum OperationState {
        Created,
        Rejected,
        Failed,
        InQueue,
        Running,
        Cancelled,
        Finished
    }

    /**
     State setting and getting.
     */
    void setState(OperationState state);
    OperationState getState();

    /**
     Execution methods. {@code execute()} is called and should include the executing code that
     is executes asynchronously on other thread. {@code complete()} is called once the
     {@code execute()} method finishes and another {@link Operation} can be executed.
     */
    void execute();
    void complete();
    void failure(Throwable t);
}
