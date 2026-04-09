package os.thread.producerConsumer;

import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RxJava是事件驱动，对象发生了变化采取执行响应操作，而不是Thread循环检查sleep
 */
public class RxJavaProducerConsumerSimple {

    private static final PublishSubject<String> subject = PublishSubject.create();
    private static final CountDownLatch completionLatch = new CountDownLatch(1);
    private static final AtomicInteger produced = new AtomicInteger(0);
    private static final AtomicInteger consumed = new AtomicInteger(0);

    private static void produce() {
        System.out.println("📤 生产者开始生产消息...");

        try {
            for (int i = 0; i < 10; i++) {
                String message = "Message " + i;
                System.out.println("📨 发送: " + message + " (生产总数: " + produced.incrementAndGet() + ")");

                subject.onNext(message);
                Thread.sleep(50);
            }

            System.out.println("✅ 生产者完成所有消息发送");
            subject.onComplete();

        } catch (Exception e) {
            System.err.println("❌ 生产者发生异常: " + e.getMessage());
            subject.onError(e);
        }
    }

    private static Disposable consume() {
        System.out.println("📥 消费者开始订阅...");

        return subject
                .observeOn(Schedulers.io())
                .subscribe(
                        message -> {
                            System.out.println("📩 接收: " + message + " (消费总数: " + consumed.incrementAndGet() + ")");
                            try {
                                Thread.sleep(150);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        },
                        error -> {
                            System.err.println("💥 消费者遇到异常: " + error.getMessage());
                            completionLatch.countDown();
                        },
                        () -> {
                            System.out.println("🎉 消费者接收完成");
                            completionLatch.countDown();
                        }
                );
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("🚀 启动RxJava生产者消费者示例...");

        Disposable consumerDisposable = consume();
        Thread.sleep(100); // 确保订阅建立

        produce();

        completionLatch.await();
        Thread.sleep(1000); // 确保所有消息处理完成

        consumerDisposable.dispose();
        System.out.println("✅ 程序执行完成");
        System.out.println("📊 统计: 生产=" + produced.get() + ", 消费=" + consumed.get());
    }
}