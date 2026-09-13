package com.codegraph.bridge.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
public class SingleInstanceService {

    private static final Path LOCK_FILE = Path.of(
            System.getProperty("java.io.tmpdir"),
            "codegraph-agent.lock"
    );

    private FileChannel channel;
    private FileLock lock;

    /**
     * Normal startup.
     */
    public boolean acquire() {
        try {
            Files.createDirectories(LOCK_FILE.getParent());

            channel = FileChannel.open(
                    LOCK_FILE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
            );

            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException e) {
                lock = null;
            }

            if (lock == null) {
                closeChannel();
                return false;
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Restart startup.
     *
     * Waits for the previous process to release the lock.
     */
    public boolean acquireWithRetry(long timeoutMs) {

        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeoutMs) {

            if (acquire()) {
                return true;
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    public void release() {

        try {
            if (lock != null && lock.isValid()) {
                lock.release();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock = null;
            closeChannel();
        }
    }

    private void closeChannel() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            channel = null;
        }
    }
}