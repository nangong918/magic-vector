package os.thread.lock;

/**
 * 死锁的条件：
 * 1. 互斥条件：两个进程对同一资源，只能有一个进程占用；
 * 2. 请求与保持条件：进程已经占用了资源，又请求同一资源，导致无法继续执行
 * 3. 不可抢占条件：进程已占用资源，其他进程无法抢占该资源
 * 4. 循环等待条件：进程A请求资源B，进程B请求资源A，两个进程互相等待，造成死锁
 * <p>
 * 死锁的解决方法：
 * 1. 避免死锁：避免两个进程互相请求资源；
 * 2. 检测死锁：通过死锁检测，找出死锁的进程；
 * 3. 恢复死锁：通过死锁恢复，将死锁的进程唤醒
 */
public class Deadlock {

    // 两个互斥资源
    private static final Object resourceA = new Object();
    private static final Object resourceB = new Object();

    public static void main(String[] args) {
        System.out.println("🚨 死锁演示开始...");
        System.out.println("资源A: " + resourceA);
        System.out.println("资源B: " + resourceB);
        System.out.println();

        // 线程1：先获取resourceA，再请求resourceB
        Thread thread1 = new Thread(() -> {
            System.out.println("线程1: 启动，准备获取资源A");

            // 🔒 条件1：互斥条件 - synchronized保证同一时间只有一个线程能占用资源
            synchronized (resourceA) {
                System.out.println("线程1: ✅ 成功获取资源A");

                try {
                    // 模拟一些处理时间
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("线程1: 等待获取资源B...");

                // 🔒 条件2：请求与保持条件 - 线程1已经持有resourceA，又请求resourceB
                // 🔒 条件4：循环等待条件 - 线程1等待resourceB，而resourceB被线程2持有
                // 🔒 条件3：不可抢占条件 - 线程1已占用resourceA，线程2无法抢占resourceA
                synchronized (resourceB) {
                    System.out.println("线程1: ✅ 成功获取资源B");
                    // 执行业务逻辑
                    System.out.println("线程1: 执行业务逻辑...");
                }
            }
            System.out.println("线程1: 完成并释放所有资源");
        }, "Thread-1");

        // 线程2：先获取resourceB，再请求resourceA
        Thread thread2 = new Thread(() -> {
            System.out.println("线程2: 启动，准备获取资源B");

            // 🔒 条件1：互斥条件 - synchronized保证同一时间只有一个线程能占用资源
            synchronized (resourceB) {
                System.out.println("线程2: ✅ 成功获取资源B");

                try {
                    // 模拟一些处理时间
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("线程2: 等待获取资源A...");

                // 🔒 条件2：请求与保持条件 - 线程2已经持有resourceB，又请求resourceA
                // 🔒 条件4：循环等待条件 - 线程2等待resourceA，而resourceA被线程1持有
                // 🔒 条件3：不可抢占条件 - 线程2已占用resourceB，线程1无法抢占resourceB
                synchronized (resourceA) {
                    System.out.println("线程2: ✅ 成功获取资源A");
                    // 执行业务逻辑
                    System.out.println("线程2: 执行业务逻辑...");
                }
            }
            System.out.println("线程2: 完成并释放所有资源");
        }, "Thread-2");

        // 启动线程
        thread1.start();
        thread2.start();

        // 监控线程状态
        Thread monitor = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    System.out.println("监控: 线程1状态=" + thread1.getState() +
                            ", 线程2状态=" + thread2.getState());
                    Thread.sleep(1000);

                    // 5秒后检测到死锁
                    if (i == 4) {
                        System.out.println("\n🚨 检测到死锁！两个线程都处于BLOCKED状态");
                        System.out.println("💡 死锁原因分析：");
                        System.out.println("   1. 互斥条件: ✅ 两个资源都被synchronized保护");
                        System.out.println("   2. 请求与保持: ✅ 每个线程都持有资源并请求另一个");
                        System.out.println("   3. 不可抢占: ✅ synchronized锁不可被抢占");
                        System.out.println("   4. 循环等待: ✅ 线程1等B(被2持有)，线程2等A(被1持有)");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Monitor-Thread");

        monitor.start();

        // 等待一段时间后强制结束
        try {
            Thread.sleep(10000);
            System.out.println("\n⏰ 10秒后强制结束程序...");
            System.exit(0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}