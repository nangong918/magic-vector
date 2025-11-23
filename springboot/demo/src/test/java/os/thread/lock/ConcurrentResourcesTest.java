package os.thread.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class ConcurrentResourcesTest {

    private final static long NUM_ITEM = 10_000L;

    /**
     * 并发读写List
     */
    static class ConcurrentModificationDemo {

        private static final List<String> dataList = new ArrayList<>();

        public static void main(String[] args) {
            ConcurrentModificationDemo demo = new ConcurrentModificationDemo();
            demo.demoConcurrentModification();
        }

        /**
         * 在读的时候并发写
         * 18:25:25.882 [Thread-0] ERROR os.thread.lock.ConcurrentResourcesTest -- 捕获到并发修改异常:
         * java.util.ConcurrentModificationException: null
         * 	at java.base/java.util.ArrayList$Itr.checkForComodification(ArrayList.java:1095)
         * 	at java.base/java.util.ArrayList$Itr.next(ArrayList.java:1049)
         * 	at os.thread.lock.ConcurrentResourcesTest$ConcurrentModificationDemo.iterateList(ConcurrentResourcesTest.java:48)
         * 	at java.base/java.lang.Thread.run(Thread.java:1583)
         */
        public void demoConcurrentModification() {
            // 初始化数据
            for (int i = 0; i < NUM_ITEM; i++) {
                dataList.add("item-" + i);
            }

            // 创建两个线程并发操作
            Thread thread1 = new Thread(this::iterateList);
            Thread thread2 = new Thread(this::modifyList);

            thread1.start();
            thread2.start();

            try {
                thread1.join();
                thread2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("捕获到线程中断异常: ", e);
            }
        }

        // 线程1：使用增强for循环遍历
        private void iterateList() {
            try {
                for (String item : dataList) {
                    System.out.println("遍历: " + item);
                }
            } catch (ConcurrentModificationException e) {
                log.error("捕获到并发修改异常: ", e);
            }
        }

        // 线程2：修改List
        private void modifyList() {
            System.out.println("开始修改List...");
            dataList.clear(); // 这会引发ConcurrentModificationException
            dataList.add("new-item");
        }
    }

    /**
     * 并发读写HashMap
     */
    static class HashMapConcurrentDemo {
        private final static Map<Integer, String> dataMap = new HashMap<>();


        public static void main(String[] args) {
            HashMapConcurrentDemo demo = new HashMapConcurrentDemo();
            demo.demoHashMapConcurrentIssue();
        }

        /**
         * 修改HashMap...
         * 遍历: 0=value-0
         * 18:29:03.428 [Thread-0] ERROR os.thread.lock.ConcurrentResourcesTest -- HashMap并发修改异常:
         * java.util.ConcurrentModificationException: null
         * 	at java.base/java.util.HashMap$HashIterator.nextNode(HashMap.java:1605)
         * 	at java.base/java.util.HashMap$EntryIterator.next(HashMap.java:1638)
         * 	at java.base/java.util.HashMap$EntryIterator.next(HashMap.java:1636)
         * 	at os.thread.lock.ConcurrentResourcesTest$HashMapConcurrentDemo.iterateMap(ConcurrentResourcesTest.java:101)
         * 	at java.base/java.lang.Thread.run(Thread.java:1583)
         */
        public void demoHashMapConcurrentIssue() {
            // 初始化数据
            for (int i = 0; i < 10; i++) {
                dataMap.put(i, "value-" + i);
            }

            // 创建两个线程
            Thread thread1 = new Thread(this::iterateMap);
            Thread thread2 = new Thread(this::modifyMap);

            thread1.start();
            thread2.start();
        }

        private void iterateMap() {
            try {
                for (Map.Entry<Integer, String> entry : dataMap.entrySet()) {
                    System.out.println("遍历: " + entry.getKey() + "=" + entry.getValue());
                }
            } catch (ConcurrentModificationException e) {
                log.error("HashMap并发修改异常: ", e);
            }
        }

        private void modifyMap() {
            System.out.println("修改HashMap...");
            dataMap.put(100, "new-value");
            dataMap.remove(0);
        }
    }

    /**
     * 解决方案1：使用并发安全数据结构
     */
    static class ConcurrentCollectionSolution {
        private static final List<String> copyOnWriteList = new CopyOnWriteArrayList<>();

        public static void main(String[] args) {
            ConcurrentCollectionSolution demo = new ConcurrentCollectionSolution();
            demo.demoConcurrentList();
        }

        /**
         * 读取不稳定，明显需要锁。
         * 修改先完成的情况：读取到2个
         * 18:39:38.228 [main] INFO os.thread.lock.ConcurrentResourcesTest -- === 并发集合解决方案测试 ===
         * 18:39:38.231 [Thread-1] INFO os.thread.lock.ConcurrentResourcesTest -- 开始安全修改List...
         * 18:39:38.231 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 开始安全遍历List...
         * 18:39:38.232 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 安全遍历List: new-item-1
         * 18:39:38.232 [Thread-1] INFO os.thread.lock.ConcurrentResourcesTest -- List修改完成, list数据量：2
         * 18:39:38.234 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 安全遍历List: new-item-2
         * 18:39:38.234 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- List遍历完成，共2个元素
         * 18:39:38.234 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 并发集合解决方案测试结束，耗时: 159ms
         * <p>
         * 遍历先完成的情况：读取到10000个
         * 18:45:32.074 [main] INFO os.thread.lock.ConcurrentResourcesTest -- === 并发集合解决方案测试 ===
         * 18:45:32.077 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 开始安全遍历List...
         * 18:45:32.077 [Thread-1] INFO os.thread.lock.ConcurrentResourcesTest -- 开始安全修改List...
         * 18:45:32.077 [Thread-1] INFO os.thread.lock.ConcurrentResourcesTest -- List修改完成, list数据量：2
         * 18:45:32.077 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 安全遍历List: item-0
         * 18:45:32.080 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 安全遍历List: item-1
         * 18:45:32.080 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 安全遍历List: item-2
         * 18:45:32.080 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 安全遍历List: item-3
         * 18:45:32.080 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 安全遍历List: item-4
         * 18:45:32.082 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- List遍历完成，共10000个元素
         * 18:45:32.082 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 并发集合解决方案测试结束，耗时: 155ms
         */
        public void demoConcurrentList() {
            // 初始化数据
            for (int i = 0; i < NUM_ITEM; i++) {
                copyOnWriteList.add("item-" + i);
            }

            // List并发测试
            Thread listThread1 = new Thread(this::safeIterateList);
            Thread listThread2 = new Thread(this::safeModifyList);

            long startTime = System.currentTimeMillis();
            log.info("=== 并发集合解决方案测试 ===");
            listThread1.start();
            listThread2.start();

            try {
                listThread1.join();
                listThread2.join();

                log.info("并发集合解决方案测试结束，耗时: {}ms", System.currentTimeMillis() - startTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 安全遍历List - CopyOnWriteArrayList在遍历时不会受修改影响
        private void safeIterateList() {
            log.info("开始安全遍历List...");
            int count = 0;
            for (String item : copyOnWriteList) {
                count++;
                if (count <= 5) { // 只打印前5个避免日志过多
                    log.info("安全遍历List: {}", item);
                }
            }
            log.info("List遍历完成，共{}个元素", count);
        }

        // 安全修改List
        private void safeModifyList() {
            log.info("开始安全修改List...");
            copyOnWriteList.clear();
            copyOnWriteList.add("new-item-1");
            copyOnWriteList.add("new-item-2");
            log.info("List修改完成, list数据量：{}", copyOnWriteList.size());
        }
    }

    /**
     * 解决方案2：使用锁机制
     */
    static class LockSolution {
        private static final List<String> dataList = new ArrayList<>();
        private static final ReentrantLock listLock = new ReentrantLock();

        public static void main(String[] args) {
            LockSolution demo = new LockSolution();
            demo.demoLockSolution();
        }

        /**
         * 用锁不仅安全，还比ConCurrent快
         * 18:49:23.759 [main] INFO os.thread.lock.ConcurrentResourcesTest -- === 锁解决方案测试 ===
         * 18:49:23.762 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 开始加锁遍历List...
         * 18:49:23.762 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 加锁遍历List: item-0
         * 18:49:23.764 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 加锁遍历List: item-1
         * 18:49:23.764 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 加锁遍历List: item-2
         * 18:49:23.764 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 加锁遍历List: item-3
         * 18:49:23.764 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 加锁遍历List: item-4
         * 18:49:23.765 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 加锁List遍历完成，共10000个元素
         * 18:49:23.765 [Thread-1] INFO os.thread.lock.ConcurrentResourcesTest -- 开始加锁修改List...
         * 18:49:23.765 [Thread-1] INFO os.thread.lock.ConcurrentResourcesTest -- 加锁List修改完成，共2个元素
         * 18:49:23.766 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 锁解决方案测试结束，耗时: 5ms
         */
        public void demoLockSolution() {
            // 初始化数据
            for (int i = 0; i < NUM_ITEM; i++) {
                dataList.add("item-" + i);
            }


            Thread listThread1 = new Thread(this::lockIterateList);
            Thread listThread2 = new Thread(this::lockModifyList);

            log.info("=== 锁解决方案测试 ===");
            long startTime = System.currentTimeMillis();
            listThread1.start();
            listThread2.start();

            try {
                listThread1.join();
                listThread2.join();

                log.info("锁解决方案测试结束，耗时: {}ms", System.currentTimeMillis() - startTime);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }

        // 使用锁遍历List
        private void lockIterateList() {
            listLock.lock();
            try {
                log.info("开始加锁遍历List...");
                int count = 0;
                for (String item : dataList) {
                    count++;
                    if (count <= 5) {
                        log.info("加锁遍历List: {}", item);
                    }
                }
                log.info("加锁List遍历完成，共{}个元素", count);
            } finally {
                listLock.unlock();
            }
        }

        // 使用锁修改List
        private void lockModifyList() {
            listLock.lock();
            try {
                log.info("开始加锁修改List...");
                dataList.clear();
                dataList.add("locked-new-item-1");
                dataList.add("locked-new-item-2");
                log.info("加锁List修改完成，共{}个元素", dataList.size());
            } finally {
                listLock.unlock();
            }
        }
    }

    /**
     * 解决方案3：使用迭代器安全删除
     */
    static class IteratorSolution {
        private static final List<String> dataList = new ArrayList<>();

        public static void main(String[] args) {
            IteratorSolution demo = new IteratorSolution();
            demo.demoIteratorSolution();
        }

        /**
         * 迭代器安全删除元素
         * 18:52:37.142 [main] INFO os.thread.lock.ConcurrentResourcesTest -- === 迭代器解决方案测试 ===
         * 18:52:37.147 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 开始迭代器安全遍历List...
         * 18:52:37.147 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 迭代器遍历List: item-0
         * 18:52:37.150 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 迭代器遍历List: item-1
         * 18:52:37.150 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 迭代器遍历List: item-2
         * 18:52:37.150 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 迭代器遍历List: item-3
         * 18:52:37.150 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 迭代器遍历List: item-4
         * 18:52:37.151 [Thread-0] INFO os.thread.lock.ConcurrentResourcesTest -- 迭代器List遍历完成，共处理10000个元素
         * 18:52:37.151 [Thread-1] INFO os.thread.lock.ConcurrentResourcesTest -- 开始同步修改List...
         * 18:52:37.152 [Thread-1] INFO os.thread.lock.ConcurrentResourcesTest -- 同步List修改完成, 共处理2个元素
         * 18:52:37.152 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 迭代器解决方案测试结束，耗时: 5ms
         */
        public void demoIteratorSolution() {
            // 初始化数据
            for (int i = 0; i < NUM_ITEM; i++) {
                dataList.add("item-" + i);
            }

            Thread listThread1 = new Thread(this::iteratorSafeIterateList);
            Thread listThread2 = new Thread(this::iteratorSafeModifyList);

            log.info("=== 迭代器解决方案测试 ===");
            long startTime = System.currentTimeMillis();
            listThread1.start();
            listThread2.start();

            try {
                listThread1.join();
                listThread2.join();
                log.info("迭代器解决方案测试结束，耗时: {}ms", System.currentTimeMillis() - startTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 使用迭代器安全遍历和删除List
        private void iteratorSafeIterateList() {
            synchronized (dataList) {
                log.info("开始迭代器安全遍历List...");
                Iterator<String> iterator = dataList.iterator();
                int count = 0;
                while (iterator.hasNext()) {
                    String item = iterator.next();
                    count++;
                    if (count <= 5) {
                        log.info("迭代器遍历List: {}", item);
                    }
                }
                log.info("迭代器List遍历完成，共处理{}个元素", count);
            }
        }

        // 同步修改List
        private void iteratorSafeModifyList() {
            synchronized (dataList) {
                log.info("开始同步修改List...");
                dataList.clear();
                dataList.add("iterator-new-item-1");
                dataList.add("iterator-new-item-2");
                log.info("同步List修改完成, 共处理{}个元素", dataList.size());
            }
        }
    }

    private static final int THREAD_COUNT = 200;
    private static final int TASK_COUNT = 1000;

    // 共享资源
    private static final List<Integer> sharedResource = new ArrayList<>();

    /**
     * 模拟任务处理 - 访问共享资源
     */
    private static boolean processTask(int taskId, String source) {
        synchronized (sharedResource) {
            // 修改共享资源
            sharedResource.add(taskId);

            // 模拟偶尔地处理失败
            if (ThreadLocalRandom.current().nextInt(100) < 5) { // 5%失败率
                log.debug("{} - 任务{}处理失败", source, taskId);
                return false;
            }

            if (taskId % 100 == 0) {
                log.debug("{} - 任务{}处理成功", source, taskId);
            }
            return true;
        }
    }

    /**
     * 直接创建200个线程的测试
     */
    static class DirectThreadTest {
        private final CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger timeoutCount = new AtomicInteger(0);


        public static void main(String[] args) throws InterruptedException {
            DirectThreadTest demo = new DirectThreadTest();
            demo.testDirectThreads();
        }

        /**
         * 19:22:11.588 [main] INFO os.thread.lock.ConcurrentResourcesTest -- === 直接创建200个线程测试开始 ===
         * 19:22:11.617 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 直接创建线程测试结果:
         * 19:22:11.617 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 执行时间: 23ms
         * 19:22:11.617 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 成功任务: 186
         * 19:22:11.617 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 失败任务: 14
         * 19:22:11.617 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 超时任务: 0
         * 19:22:11.617 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 最终资源大小: 200
         * @throws InterruptedException
         */
        public void testDirectThreads() throws InterruptedException {
            log.info("=== 直接创建{}个线程测试开始 ===", THREAD_COUNT);
            long startTime = System.currentTimeMillis();

            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                Thread thread = new Thread(() -> {
                    try {
                        // 模拟任务处理
                        boolean success = processTask(threadId, "DirectThread");
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("线程{}执行异常: {}", threadId, e.getMessage());
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
                threads.add(thread);
            }

            // 启动所有线程
            for (Thread thread : threads) {
                thread.start();
            }

            // 等待所有线程完成，设置超时时间
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();

            if (!completed) {
                log.warn("!!! 线程执行超时，可能发生死锁或长时间等待 !!!");
                // 计算超时的线程数
                int remaining = (int) latch.getCount();
                timeoutCount.set(remaining);
                failureCount.addAndGet(remaining);
            }

            log.info("直接创建线程测试结果:");
            log.info("执行时间: {}ms", (endTime - startTime));
            log.info("成功任务: {}", successCount.get());
            log.info("失败任务: {}", failureCount.get());
            log.info("超时任务: {}", timeoutCount.get());
            log.info("最终资源大小: {}", sharedResource.size());
        }
    }

    /**
     * 线程池管理的测试
     */
    static class ThreadPoolTest {
        private final CountDownLatch latch = new CountDownLatch(TASK_COUNT);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger rejectedCount = new AtomicInteger(0);

        public static void main(String[] args) throws InterruptedException {
            ThreadPoolTest demo = new ThreadPoolTest();
            demo.testThreadPool();
        }

        /**
         * 19:25:20.264 [main] INFO os.thread.lock.ConcurrentResourcesTest -- === 线程池管理测试开始 ===
         * 19:25:20.282 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 线程池测试结果:
         * 19:25:20.282 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 执行时间: 14ms
         * 19:25:20.284 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 成功任务: 936
         * 19:25:20.284 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 失败任务: 64
         * 19:25:20.284 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 拒绝任务: 0
         * 19:25:20.285 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 池状态 - 核心线程: 10, 最大线程: 20, 队列大小: 0
         * 19:25:20.285 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 最终资源大小: 1000
         * @throws InterruptedException
         */
        public void testThreadPool() throws InterruptedException {
            log.info("=== 线程池管理测试开始 ===");
            long startTime = System.currentTimeMillis();

            // 创建有界线程池
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    10, // 核心线程数
                    20, // 最大线程数
                    60, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(50), // 有界队列
                    new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
            );

            for (int i = 0; i < TASK_COUNT; i++) {
                final int taskId = i;
                try {
                    executor.execute(() -> {
                        try {
                            boolean success = processTask(taskId, "ThreadPool");
                            if (success) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            log.error("任务{}执行异常: {}", taskId, e.getMessage());
                            failureCount.incrementAndGet();
                        } finally {
                            latch.countDown();
                        }
                    });
                } catch (Exception e) {
                    log.warn("任务{}被拒绝: {}", taskId, e.getMessage());
                    rejectedCount.incrementAndGet();
                    latch.countDown();
                }
            }

            // 等待所有任务完成
            latch.await();
            executor.shutdown();
            boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);

            long endTime = System.currentTimeMillis();

            log.info("线程池测试结果:");
            log.info("执行时间: {}ms", (endTime - startTime));
            log.info("成功任务: {}", successCount.get());
            log.info("失败任务: {}", failureCount.get());
            log.info("拒绝任务: {}", rejectedCount.get());
            log.info("池状态 - 核心线程: {}, 最大线程: {}, 队列大小: {}",
                    executor.getCorePoolSize(), executor.getMaximumPoolSize(),
                    executor.getQueue().size());
            log.info("最终资源大小: {}", sharedResource.size());

            if (!terminated) {
                log.warn("线程池未完全终止");
            }
        }
    }


    /**
     * 自定义队列测试
     */
    static class CustomQueueTest {
        /**
         * 多线程队列管理类
         */
        static class MultiThreadedQueue {
            private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
            private final List<WorkerThread> workers = new ArrayList<>();
            private final AtomicInteger activeCount = new AtomicInteger(0);
            private volatile boolean running = true;


            public MultiThreadedQueue(int workerCount) {
                // 创建工作线程
                for (int i = 0; i < workerCount; i++) {
                    WorkerThread worker = new WorkerThread("Worker-" + i);
                    workers.add(worker);
                    worker.start();
                }
            }

            public void submit(Runnable task) {
                if (running) {
                    try {
                        taskQueue.put(task);
                        activeCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("提交任务被中断", e);
                    }
                }
            }

            public void shutdown() {
                running = false;
                for (WorkerThread worker : workers) {
                    worker.interrupt();
                }
            }

            public int getQueueSize() {
                return taskQueue.size();
            }

            public int getActiveCount() {
                return activeCount.get();
            }

            private class WorkerThread extends Thread {
                public WorkerThread(String name) {
                    super(name);
                }

                @Override
                public void run() {
                    while (running && !isInterrupted()) {
                        try {
                            Runnable task = taskQueue.take();
                            try {
                                task.run();
                            } catch (Exception e) {
                                log.error("任务执行异常", e);
                            } finally {
                                activeCount.decrementAndGet();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }

        public static void main(String[] args) throws InterruptedException {
            CustomQueueTest test = new CustomQueueTest();
            test.testCustomQueue();
        }

        /**
         * 19:28:56.496 [main] INFO os.thread.lock.ConcurrentResourcesTest -- === 自定义多线程队列测试开始 ===
         * 19:28:56.510 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 自定义队列测试结果:
         * 19:28:56.511 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 执行时间: 10ms
         * 19:28:56.514 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 成功任务: 959
         * 19:28:56.514 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 失败任务: 41
         * 19:28:56.514 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 最终队列大小: 0
         * 19:28:56.514 [main] INFO os.thread.lock.ConcurrentResourcesTest -- 最终资源大小: 1000
         * @throws InterruptedException
         */
        public void testCustomQueue() throws InterruptedException {
            log.info("=== 自定义多线程队列测试开始 ===");
            long startTime = System.currentTimeMillis();

            MultiThreadedQueue queue = new MultiThreadedQueue(10); // 10个工作线程
            CountDownLatch latch = new CountDownLatch(TASK_COUNT);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            for (int i = 0; i < TASK_COUNT; i++) {
                final int taskId = i;
                queue.submit(() -> {
                    try {
                        boolean success = processTask(taskId, "CustomQueue");
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("任务{}执行异常: {}", taskId, e.getMessage());
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 等待所有任务完成
            latch.await();
            queue.shutdown();

            long endTime = System.currentTimeMillis();

            log.info("自定义队列测试结果:");
            log.info("执行时间: {}ms", (endTime - startTime));
            log.info("成功任务: {}", successCount.get());
            log.info("失败任务: {}", failureCount.get());
            log.info("最终队列大小: {}", queue.getQueueSize());
            log.info("最终资源大小: {}", sharedResource.size());
        }
    }
}
