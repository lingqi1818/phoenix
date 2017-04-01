package com.fangcloud.phoenix.server.buffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

/**
 * BufferPool
 * 
 * @author chenke
 * @date 2017年3月23日 上午10:18:42
 */
public final class BufferPool {
    public static final String                      LOCAL_BUF_THREAD_PREX = "$_";
    private final ThreadLocalBufferPool             localBufferPool;
    private static final Logger                     LOGGER                = Logger
            .getLogger(BufferPool.class);
    private final int                               chunkSize;
    private final ConcurrentLinkedQueue<ByteBuffer> items                 = new ConcurrentLinkedQueue<ByteBuffer>();
    private long                                    sharedOptsCount;
    private volatile int                            newCreated;
    private final long                              threadLocalCount;
    private final long                              capactiy;
    private long                                    totalBytes            = 0;
    private long                                    totalCounts           = 0;
    private static BufferPool                       bufferPool;

    public static BufferPool getInstance() {
        return bufferPool;
    }

    public static BufferPool createPool(long bufferSize, int chunkSize, int threadLocalPercent) {

        bufferPool = new BufferPool(bufferSize, chunkSize, threadLocalPercent);
        return bufferPool;
    }

    private BufferPool(long bufferSize, int chunkSize, int threadLocalPercent) {
        this.chunkSize = chunkSize;
        long size = bufferSize / chunkSize;
        size = (bufferSize % chunkSize == 0) ? size : size + 1;
        this.capactiy = size;
        threadLocalCount = threadLocalPercent * capactiy / 100;
        for (long i = 0; i < capactiy; i++) {
            items.offer(createDirectBuffer(chunkSize));
        }
        localBufferPool = new ThreadLocalBufferPool(threadLocalCount);
    }

    private static final boolean isLocalCacheThread() {
        final String thname = Thread.currentThread().getName();
        return (thname.length() < LOCAL_BUF_THREAD_PREX.length()) ? false
                : (thname.charAt(0) == '$' && thname.charAt(1) == '_');

    }

    public int getChunkSize() {
        return chunkSize;
    }

    public long getSharedOptsCount() {
        return sharedOptsCount;
    }

    public long size() {
        return this.items.size();
    }

    public long capacity() {
        return capactiy + newCreated;
    }

    public ByteBuffer allocate() {
        ByteBuffer node = null;
        if (isLocalCacheThread()) {
            // allocate from threadlocal
            node = localBufferPool.get().poll();
            if (node != null) {
                return node;
            }
        }
        node = items.poll();
        if (node == null) {
            // Allocate from heap if direct buffer OOM occurs
            try {
                node = this.createDirectBuffer(chunkSize);
                ++newCreated;
            } catch (final OutOfMemoryError oom) {
                LOGGER.warn("Direct buffer OOM occurs: so allocate from heap", oom);
                node = this.createTempBuffer(chunkSize);
            }
        }
        return node;
    }

    private boolean checkValidBuffer(ByteBuffer buffer) {
        // 拒绝回收null和容量大于chunkSize的缓存
        if (buffer == null || !buffer.isDirect()) {
            return false;
        } else if (buffer.capacity() > chunkSize) {
            LOGGER.warn(
                    "cant' recycle  a buffer large than my pool chunksize " + buffer.capacity());
            return false;
        }
        totalCounts++;
        totalBytes += buffer.limit();
        buffer.clear();
        return true;
    }

    public void recycle(ByteBuffer buffer) {
        if (!checkValidBuffer(buffer)) {
            return;
        }
        if (isLocalCacheThread()) {
            BufferQueue localQueue = localBufferPool.get();
            if (localQueue.snapshotSize() < threadLocalCount) {
                localQueue.put(buffer);
            } else {
                // recyle 3/4 thread local buffer
                items.addAll(localQueue.removeItems(threadLocalCount * 3 / 4));
                items.offer(buffer);
                sharedOptsCount++;
            }
        } else {
            sharedOptsCount++;
            items.offer(buffer);
        }

    }

    public int getAvgBufSize() {
        if (this.totalBytes < 0) {
            totalBytes = 0;
            this.totalCounts = 0;
            return 0;
        } else {
            return (int) (totalBytes / totalCounts);
        }
    }

    public boolean testIfDuplicate(ByteBuffer buffer) {
        for (ByteBuffer exists : items) {
            if (exists == buffer) {
                return true;
            }
        }
        return false;

    }

    private ByteBuffer createTempBuffer(int size) {
        return ByteBuffer.allocate(size);
    }

    private ByteBuffer createDirectBuffer(int size) {
        // for performance
        return ByteBuffer.allocateDirect(size);
    }

    public ByteBuffer allocate(int size) {
        if (size <= this.chunkSize) {
            return allocate();
        } else {
            LOGGER.warn("allocate buffer size large than default chunksize:" + this.chunkSize
                    + " he want " + size);
            return createTempBuffer(size);
        }
    }

    public int getNewCreated() {
        return this.newCreated;
    }

    public static void main(String[] args) {
        BufferPool pool = new BufferPool(2048, 1024, 2);
        long i = pool.capacity();
        List<ByteBuffer> all = new ArrayList<ByteBuffer>();
        for (int j = 0; j <= i; j++) {
            all.add(pool.allocate());
        }
        for (ByteBuffer buf : all) {
            pool.recycle(buf);
        }
        LOGGER.info(pool.size());
    }
}
