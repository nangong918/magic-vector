package designPattern.build_;

/**
 * @author 13225
 * @date 2025/11/6 12:50
 * 适配器模式，根据适配者的特性展示不同的效果。Android的RecyclerViewAdapter就会根据不同的View和ViewHolder进行不同类型的数据绑定并展现。
 */
public class AdapterPattern {

    // Target接口
    interface Target {
        void request();
    }

    // 适配者类
    static class Adaptee {
        public void specificRequest() {
            System.out.println("Called specificRequest() method from Adaptee");
        }
    }

    // 适配器类
    static class Adapter implements Target {
        private final Adaptee adaptee;

        public Adapter(Adaptee adaptee) {
            this.adaptee = adaptee;
        }

        @Override
        public void request() {
            // 调用适配者的方法
            adaptee.specificRequest();
        }
    }

    // 测试类
    public static class AdapterPatternDemo {
        public static void main(String[] args) {
            // 创建适配者对象
            Adaptee adaptee = new Adaptee();

            // 创建适配器对象
            Target adapter = new Adapter(adaptee);

            // 调用目标方法
            adapter.request();
        }
    }

}
