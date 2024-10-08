/*
 * Copyright [2013-2021], Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.polardbx.executor.mpp.execution.buffer;

import com.alibaba.polardbx.common.exception.MemoryNotEnoughException;
import com.alibaba.polardbx.executor.mpp.execution.SystemMemoryUsageListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@ThreadSafe
public class OutputBufferMemoryManager {
    protected final long maxBufferedBytes;
    protected final AtomicLong bufferedBytes = new AtomicLong();

    @GuardedBy("this")
    protected SettableFuture<?> notFull;

    protected final AtomicBoolean blockOnFull = new AtomicBoolean(true);

    protected final SystemMemoryUsageListener systemMemoryUsageListener;
    protected final Executor notificationExecutor;

    public OutputBufferMemoryManager(long maxBufferedBytes, SystemMemoryUsageListener systemMemoryUsageListener,
                                     Executor notificationExecutor) {
        checkArgument(maxBufferedBytes > 0, "maxBufferedBytes must be > 0");
        this.maxBufferedBytes = maxBufferedBytes;
        this.systemMemoryUsageListener = requireNonNull(systemMemoryUsageListener, "systemMemoryUsageListener is null");
        this.notificationExecutor = requireNonNull(notificationExecutor, "notificationExecutor is null");

        notFull = SettableFuture.create();
        notFull.set(null);
    }

    public synchronized void updateMemoryUsage(long bytesAdded) {
        try {
            systemMemoryUsageListener.updateSystemMemoryUsage(bytesAdded);
            bufferedBytes.addAndGet(bytesAdded);
        } catch (MemoryNotEnoughException e) {
            throw e;
        }
        if (!isFull() && !notFull.isDone()) {
            // Complete future in a new thread to avoid making a callback on the caller thread.
            // This make is easier for callers to use this class since they can update the memory
            // usage while holding locks.
            SettableFuture<?> future = this.notFull;
            notificationExecutor.execute(() -> future.set(null));
        }
    }

    public synchronized ListenableFuture<?> getNotFullFuture() {
        if (isFull() && notFull.isDone()) {
            notFull = SettableFuture.create();
        }
        return notFull;
    }

    public synchronized void setNoBlockOnFull() {
        blockOnFull.set(false);
        // Complete future in a new thread to avoid making a callback on the caller thread.
        SettableFuture<?> future = notFull;
        notificationExecutor.execute(() -> future.set(null));
    }

    public long getBufferedBytes() {
        return bufferedBytes.get();
    }

    public double getUtilization() {
        return bufferedBytes.get() / (double) maxBufferedBytes;
    }

    public boolean isFull() {
        return bufferedBytes.get() > maxBufferedBytes && blockOnFull.get();
    }
}