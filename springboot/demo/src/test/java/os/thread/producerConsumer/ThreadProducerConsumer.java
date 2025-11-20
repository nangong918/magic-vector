package os.thread.producerConsumer;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author 13225
 * @date 2025/11/20 13:34
 */
public class ThreadProducerConsumer {

    // 定义一个ConcurrentLinkedQueue来存储Base64数据 (不用BlockingQueue，因为不确定数量)
    private static final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    // 生产者线程
    static class Producer implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < 20; i++) { // 假设生产20个Base64数据
                String base64Data = "data" + i; // 假设生成Base64数据
                queue.offer(base64Data); // 非阻塞方法
                System.out.println("Produced: " + base64Data);
                try {
                    Thread.sleep(100); // 模拟生产延迟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // 消费者线程
    static class Consumer implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < 20; i++) { // 消费20个Base64数据
                String base64Data;
                // 循环直到获取到数据
                while ((base64Data = queue.poll()) == null) {
                    // 休眠以避免忙等
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println("Consumed: " + base64Data);
            }
        }
    }

    public static void main(String[] args) {
        Thread producerThread = new Thread(new Producer());
        Thread consumerThread = new Thread(new Consumer());

        producerThread.start();
        consumerThread.start();

        try {
            producerThread.join();
            consumerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
