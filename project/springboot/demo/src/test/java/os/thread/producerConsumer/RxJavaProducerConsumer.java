package os.thread.producerConsumer;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.NonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class RxJavaProducerConsumer {

    private static final CountDownLatch completionLatch = new CountDownLatch(1);
    private static final AtomicInteger produced = new AtomicInteger(0);
    private static final AtomicInteger consumed = new AtomicInteger(0);

    @NonNull
    private static Observable<String> createObservable() {
        return Observable.create(emitter -> {
            System.out.println("📤 生产者开始生产消息...");

            try {
                for (int i = 0; i < 10; i++) {
                    if (emitter.isDisposed()) {
                        break;
                    }

                    String message = "Message " + i;
                    System.out.println("📨 发送: " + message + " (生产总数: " + produced.incrementAndGet() + ")");

                    emitter.onNext(message);
                    Thread.sleep(50);
                }

                if (!emitter.isDisposed()) {
                    System.out.println("✅ 生产者完成所有消息发送");
                    emitter.onComplete();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!emitter.isDisposed()) {
                    System.err.println("❌ 生产者被中断");
                    emitter.onError(e);
                }
            } catch (Exception e) {
                if (!emitter.isDisposed()) {
                    System.err.println("❌ 生产者发生异常: " + e.getMessage());
                    emitter.onError(e);
                }
            }
        });
    }

    private static Disposable consume(@NonNull Observable<String> observable) {
        System.out.println("📥 消费者开始订阅...");

        return observable
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
                            System.out.println("🎉 消费者接收完成，所有消息处理完毕");
                            completionLatch.countDown();
                        }
                );
    }

    public static void main(String[] args) {
        System.out.println("🚀 启动RxJava生产者消费者示例...");

        Observable<String> observable = createObservable();
        // 启动消费者
        Disposable consumerDisposable = consume(observable);

        // 等待处理完成
        try {
            completionLatch.await();
            Thread.sleep(1000); // 额外等待
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        consumerDisposable.dispose();
        System.out.println("✅ 程序执行完成");
        System.out.println("📊 统计: 生产=" + produced.get() + ", 消费=" + consumed.get());
    }
}