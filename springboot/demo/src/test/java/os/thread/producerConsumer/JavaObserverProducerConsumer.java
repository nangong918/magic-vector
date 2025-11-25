package os.thread.producerConsumer;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 使用Java内置Observer接口的实现
 */
public class JavaObserverProducerConsumer {

    // 消息事件类
    static class MessageEvent {
        final String message;
        final boolean isComplete;
        final Exception error;

        MessageEvent(String message) {
            this.message = message;
            this.isComplete = false;
            this.error = null;
        }

        MessageEvent(boolean isComplete) {
            this.message = null;
            this.isComplete = isComplete;
            this.error = null;
        }

        MessageEvent(Exception error) {
            this.message = null;
            this.isComplete = false;
            this.error = error;
        }
    }

    // 生产者（被观察者）
    static class MessageProducer extends Observable {
        private final AtomicInteger producedCount = new AtomicInteger(0);

        public void startProduce() {
            System.out.println("📤 生产者开始生产消息...");

            new Thread(() -> {
                try {
                    for (int i = 0; i < 10; i++) {
                        String message = "Message " + i;
                        System.out.println("📨 发送: " + message + " (生产总数: " + producedCount.incrementAndGet() + ")");

                        /// 通知观察者
                        setChanged();
                        notifyObservers(new MessageEvent(message));

                        // 模拟生产速度
                        Thread.sleep(50);
                    }

                    // 生产完成
                    setChanged();
                    notifyObservers(new MessageEvent(true));
                    System.out.println("✅ 生产者完成所有消息发送");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    setChanged();
                    notifyObservers(new MessageEvent(e));
                } catch (Exception e) {
                    setChanged();
                    notifyObservers(new MessageEvent(e));
                }
            }).start();
        }
    }

    // 消费者（观察者）
    static class MessageConsumer implements Observer {
        private final AtomicInteger consumedCount = new AtomicInteger(0);
        private final CountDownLatch completionLatch;

        public MessageConsumer(CountDownLatch completionLatch) {
            this.completionLatch = completionLatch;
        }

        @Override
        public void update(Observable o, Object arg) {
            if (!(arg instanceof MessageEvent event)) return;

            if (event.message != null) {
                // 处理消息
                new Thread(() -> {
                    System.out.println("📩 接收: " + event.message + " (消费总数: " + consumedCount.incrementAndGet() + ")");
                    // 模拟消费速度
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } else if (event.isComplete) {
                System.out.println("🎉 消费者接收完成");
                completionLatch.countDown();
            } else if (event.error != null) {
                System.err.println("💥 消费者遇到异常: " + event.error.getMessage());
                completionLatch.countDown();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("🚀 启动Java内置观察者模式示例...");

        CountDownLatch completionLatch = new CountDownLatch(1);

        MessageProducer producer = new MessageProducer();
        MessageConsumer consumer = new MessageConsumer(completionLatch);

        producer.addObserver(consumer);
        producer.startProduce();

        completionLatch.await();
        System.out.println("✅ 程序执行完成");
    }
}
