package os.thread.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * synchronized (内置锁)
 * JDK 1.6之前：重量级锁，直接向操作系统申请互斥量
 * JDK 1.6+：锁升级优化（无锁 → 偏向锁 → 轻量级锁 → 重量级锁）
 * 适用场景：低竞争、简单的同步需求
 */
public class LockTest {

    public static final long ADD_COUNT = 1000_000L;

    /**
     * count = 1149999
     * 耗时：8ms
     */
    public static class WithoutLock {

        public static int count = 0;

        public static void main(String[] args) {
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    count++;
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    count++;
                }
            });

            long start = System.currentTimeMillis();
            t1.start();
            t2.start();

            // 等待结束
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long end = System.currentTimeMillis();

            System.out.println("count = " + count);
            System.out.println("耗时：" + (end - start) + "ms");
        }
    }

    /**
     * count = 884984
     * 耗时：44ms
     */
    public static class VolatileWithoutLock {

        public static volatile int count = 0;
        public static void main(String[] args) {
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    count++;
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    count++;
                }
            });

            long start = System.currentTimeMillis();
            t1.start();
            t2.start();

            // 等待结束
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long end = System.currentTimeMillis();

            System.out.println("count = " + count);
            System.out.println("耗时：" + (end - start) + "ms");
        }
    }

    /**
     * count = 2000000
     * 耗时：30ms
     */
    public static class AtomicWithoutLock {
        public static AtomicInteger count = new AtomicInteger(0);

        public static void main(String[] args) {
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    count.addAndGet(1);
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    count.addAndGet(1);
                }
            });

            long start = System.currentTimeMillis();
            t1.start();
            t2.start();

            // 等待结束
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long end = System.currentTimeMillis();

            System.out.println("count = " + count);
            System.out.println("耗时：" + (end - start) + "ms");
        }
    }

    /**
     * count = 2000000
     * 耗时：40ms
     */
    public static class VolatileAtomicWithoutLock {
        public static volatile AtomicInteger count = new AtomicInteger(0);

        public static void main(String[] args) {
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    count.addAndGet(1);
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    count.addAndGet(1);
                }
            });

            long start = System.currentTimeMillis();
            t1.start();
            t2.start();

            // 等待结束
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long end = System.currentTimeMillis();

            System.out.println("count = " + count);
            System.out.println("耗时：" + (end - start) + "ms");

        }
    }

    /**
     * 方法锁
     * <p>
     * 不加volatile：
     * count = 2000000
     * 耗时：37ms
     * <p>
     * 加volatile:
     * count = 2000000
     * 耗时：55ms
     */
    public static class SynchronizedMethodLock {
        public volatile static int count = 0;

        public static synchronized void add() {
            count++;
        }

        public static void main(String[] args) {
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            });

            long start = System.currentTimeMillis();
            t1.start();
            t2.start();

            // 等待结束
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long end = System.currentTimeMillis();

            System.out.println("count = " + count);
            System.out.println("耗时：" + (end - start) + "ms");
        }
    }

    /**
     * 对象、块锁
     * <p>
     * count = 2000000
     * 耗时：56ms
     */
    public static class SynchronizedObjectLock {
        public volatile static int count = 0;

        public static void add() {
            synchronized (SynchronizedObjectLock.class) {
                count++;
            }
        }

        public static void main(String[] args) {
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            });

            long start = System.currentTimeMillis();
            t1.start();
            t2.start();

            // 等待结束
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long end = System.currentTimeMillis();

            System.out.println("count = " + count);
            System.out.println("耗时：" + (end - start) + "ms");
        }
    }

    /**
     * 2. ReentrantLock 可重入锁（非公平锁）
     * 性能与synchronized相当，但功能更丰富
     * ReentrantLock(非公平) - count = 2000000, 耗时：86ms
     */
    public static class ReentrantLockDemo {
        public static int count = 0;
        private static final ReentrantLock lock = new ReentrantLock(); // 默认非公平锁

        public static void add() {
            lock.lock();
            try {
                count++;
            } finally {
                lock.unlock();
            }
        }

        public static void main(String[] args) {
            count = 0;
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            });

            long start = System.currentTimeMillis();
            t1.start();
            t2.start();

            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long end = System.currentTimeMillis();
            System.out.println("ReentrantLock(非公平) - count = " + count + ", 耗时：" + (end - start) + "ms");
        }
    }

    /**
     * 3. ReentrantLock 公平锁
     * 保证线程按申请顺序获取锁，性能较低但公平
     * ReentrantLock(公平锁) - count = 2000000, 耗时：6664ms (tmd慢死了)
     */
    public static class ReentrantFairLockDemo {
        public static int count = 0;
        private static final ReentrantLock fairLock = new ReentrantLock(true); // 公平锁

        public static void add() {
            fairLock.lock();
            try {
                count++;
            } finally {
                fairLock.unlock();
            }
        }

        public static void main(String[] args) {
            count = 0;
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            });

            long start = System.currentTimeMillis();
            t1.start();
            t2.start();

            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long end = System.currentTimeMillis();
            System.out.println("ReentrantLock(公平锁) - count = " + count + ", 耗时：" + (end - start) + "ms");
        }
    }

    /**
     * 4. ReentrantReadWriteLock 读写锁
     * 在读多写少的场景下性能更好
     * ReadWriteLock - count = 2000000, 耗时：85ms
     */
    public static class ReadWriteLockDemo {
        public static int count = 0;
        private static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        private static final Lock readLock = rwLock.readLock();
        private static final Lock writeLock = rwLock.writeLock();

        public static void add() {
            writeLock.lock(); // 写操作需要排他锁
            try {
                count++;
            } finally {
                writeLock.unlock();
            }
        }

        // 读操作示例（虽然这里没有用到）
        public static int getCount() {
            readLock.lock(); // 读操作可以共享
            try {
                return count;
            } finally {
                readLock.unlock();
            }
        }

        public static void main(String[] args) {
            count = 0;
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            });

            long start = System.currentTimeMillis();
            t1.start();
            t2.start();

            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long end = System.currentTimeMillis();
            System.out.println("ReadWriteLock - count = " + count + ", 耗时：" + (end - start) + "ms");
        }
    }

    /**
     * 5. StampedLock 邮戳锁
     * JDK8+，性能最好的锁，支持乐观读
     * StampedLock - count = 2000000, 耗时：88ms
     */
    public static class StampedLockDemo {
        public static int count = 0;
        private static final StampedLock stampedLock = new StampedLock();

        public static void add() {
            long stamp = stampedLock.writeLock();
            try {
                count++;
            } finally {
                stampedLock.unlockWrite(stamp);
            }
        }

        // 乐观读示例
        public static int getCountOptimistic() {
            long stamp = stampedLock.tryOptimisticRead();
            int current = count;
            if (!stampedLock.validate(stamp)) {
                // 如果期间有写操作，升级为悲观读
                stamp = stampedLock.readLock();
                try {
                    current = count;
                } finally {
                    stampedLock.unlockRead(stamp);
                }
            }
            return current;
        }

        public static void main(String[] args) {
            count = 0;
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            });

            long start = System.currentTimeMillis();
            t1.start();
            t2.start();

            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long end = System.currentTimeMillis();
            System.out.println("StampedLock - count = " + count + ", 耗时：" + (end - start) + "ms");
        }
    }

    /**
     * 6. 尝试锁（TryLock）Demo
     * 避免死锁，支持超时
     * TryLock - count = 2000000, 耗时：108ms
     */
    public static class TryLockDemo {
        public static int count = 0;
        private static final ReentrantLock tryLock = new ReentrantLock();

        public static void add() {
            // 尝试获取锁，最多等待100ms
            try {
                if (tryLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                    try {
                        count++;
                    } finally {
                        tryLock.unlock();
                    }
                } else {
                    // 获取锁失败的处理
                    System.err.println(Thread.currentThread().getName() + " 获取锁失败");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public static void main(String[] args) {
            count = 0;
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            }, "Thread-1");
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT; i++) {
                    add();
                }
            }, "Thread-2");

            long start = System.currentTimeMillis();
            t1.start();
            t2.start();

            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long end = System.currentTimeMillis();
            System.out.println("TryLock - count = " + count + ", 耗时：" + (end - start) + "ms");
        }
    }

    /**
     * 7. 条件变量 Demo
     * 用于线程间的协调通信
     * Condition - 最终count = 10000, 耗时：262ms
     */
    public static class ConditionDemo {
        private static final ReentrantLock lock = new ReentrantLock();
        private static final Condition condition = lock.newCondition();
        public static int count = 0;
        private static boolean available = false;

        public static void produce() {
            lock.lock();
            try {
                while (available) {
                    condition.await(); // 等待消费
                }
                count++;
                available = true;
//                System.out.println("生产: " + count);
                condition.signal(); // 通知消费者
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }

        public static void consume() {
            lock.lock();
            try {
                while (!available) {
                    condition.await(); // 等待生产
                }
//                System.out.println("消费: " + count);
                available = false;
                condition.signal(); // 通知生产者
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }

        public static void main(String[] args) {
            count = 0;
            Thread producer = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT / 100; i++) { // 减少次数避免输出太多
                    produce();
                }
            });
            Thread consumer = new Thread(() -> {
                for (int i = 0; i < ADD_COUNT / 100; i++) {
                    consume();
                }
            });

            long start = System.currentTimeMillis();
            producer.start();
            consumer.start();

            try {
                producer.join();
                consumer.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long end = System.currentTimeMillis();
            System.out.println("Condition - 最终count = " + count + ", 耗时：" + (end - start) + "ms");
        }
    }
}
