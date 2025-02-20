package net.sabafly.mailBox.executor;

import com.google.common.collect.Queues;
import net.sabafly.mailBox.MailBox;
import org.slf4j.Logger;

import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadedQueue <T extends Runnable> implements Runnable {
    private final Logger logger = MailBox.logger();

    private final Queue<T> jobs = Queues.newArrayDeque();
    private final Thread thread;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private volatile boolean killed = false;

    public ThreadedQueue(String threadName) {
        this.thread = new Thread(this, threadName);
        thread.start();
    }

    public void stop() {
        this.killed = true;

        lock.lock();
        try {
            condition.signal();
        } catch (Exception e) {
            logger.error("An unexpected error occurred while stopping ThreadedQueue {}", thread.getName(), e);
        } finally {
            lock.unlock();
        }
    }

    public void submit(T job) {
        lock.lock();
        try {
            jobs.offer(job);
            condition.signalAll();
        } catch (Exception e) {
            logger.error("An unexpected error occurred while submitting job to ThreadedQueue {}", thread.getName(), e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        while (!killed) {
            try {
                T job = next();
                if (job != null) {
                    job.run();
                }
            } catch (Exception e) {
                logger.error("An unexpected error occurred while running ThreadedQueue {}", thread.getName(), e);
            }
        }
    }

    public T next() {
        lock.lock();
        try {
            while (jobs.isEmpty() && !killed) {
                condition.await();
            }

            if (jobs.isEmpty()) {
                return null;
            }

            return jobs.remove();
        } catch (Exception e) {
            logger.error("An unexpected error occurred while getting next job from ThreadedQueue {}", thread.getName(), e);
            return null;
        } finally {
            lock.unlock();
        }
    }

}
