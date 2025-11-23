package os.thread.lock;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
}
