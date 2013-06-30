/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.queues.SingleConsumerDoubleQueue;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class QueueDoubleChannel extends QueuePrimitiveChannel<Double> implements DoubleChannel {
    QueueDoubleChannel(Strand owner, SingleConsumerQueue<Double, ?> queue, OverflowPolicy policy) {
        super(owner, queue, policy);
    }

    @Override
    public double receiveDouble() throws SuspendExecution, InterruptedException {
        if (isClosed())
            throw new EOFException();
        final Object n = receiveNode();
        final double m = queue().doubleValue(n);
        queue.deq(n);
        signalSenders();
        return m;
    }

    @Override
    public double receiveDouble(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException, TimeoutException {
        if (isClosed())
            throw new EOFException();
        final Object n = receiveNode(timeout, unit);
        if (n == null)
            throw new TimeoutException();
        final double m = queue().doubleValue(n);
        queue.deq(n);
        signalSenders();
        return m;
    }

    @Override
    public void send(double message) {
        if (isSendClosed())
            return;
        queue().enq(message);
        signalReceivers();
    }

    @Override
    public boolean trySend(double message) {
        if (isSendClosed())
            return true;
        if (queue().enq(message)) {
            signalReceivers();
            return true;
        } else
            return false;
    }

    public void sendSync(double message) {
        if (isSendClosed())
            return;
        queue.enq(message);
        signalAndTryToExecNow();
    }

    private SingleConsumerDoubleQueue<Object> queue() {
        return (SingleConsumerDoubleQueue<Object>) queue;
    }
}
