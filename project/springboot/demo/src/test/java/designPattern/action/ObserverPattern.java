package designPattern.action;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/11/6 14:02
 * 观察者模式，参考Android的liveData，还要考虑更新在哪个线程执行
 */
public class ObserverPattern {


    // 自定义 LiveData 类
    static class LiveData<T> {
        private T data;
        private final List<Observer<T>> observers = new ArrayList<>();

        // 设置数据并通知观察者
        public void setValue(T value) {
            this.data = value;
            notifyObservers();
        }

        // 获取当前数据
        public T getValue() {
            return data;
        }

        // 添加观察者
        public void observe(Observer<T> observer) {
            observers.add(observer);
            // 立即通知观察者当前数据
            if (data != null) {
                observer.onChanged(data);
            }
        }

        // 通知所有观察者
        private void notifyObservers() {
            for (Observer<T> observer : observers) {
                observer.onChanged(data);
            }
        }
    }

    // 观察者接口
    interface Observer<T> {
        void onChanged(T data);
    }

    // 测试类
    public static class LiveDataDemo {
        public static void main(String[] args) {
            LiveData<String> liveData = new LiveData<>();

            // 创建观察者
            Observer<String> observer1 = data -> System.out.println("Observer 1: Data changed to: " + data);

            Observer<String> observer2 = data -> System.out.println("Observer 2: Data changed to: " + data);

            // 观察 LiveData
            liveData.observe(observer1);
            liveData.observe(observer2);

            // 更新数据
            liveData.setValue("Hello, World!");
            liveData.setValue("LiveData is awesome!");
        }
    }
}
