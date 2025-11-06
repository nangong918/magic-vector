package designPattern;

/**
 * @author 13225
 * @date 2025/11/6 11:21
 * Spring种的Service和ServiceImpl的关系就是抽象工厂模式
 * 例子：PayService需要对应不同的国家：JP，CN，US。然后不同韩国的实现：JPPayImpl，CNPayImpl，USPayImpl；这就是一个抽象工厂模式
 */
public class AbstractFactoryPattern {
    // AbstractFactoryDemo.java

    // 产品接口
    interface Product {
        void use();
    }

    // 具体产品A
    static class ConcreteProductA implements Product {
        @Override
        public void use() {
            System.out.println("Using Concrete Product A");
        }
    }

    // 具体产品B
    static class ConcreteProductB implements Product {
        @Override
        public void use() {
            System.out.println("Using Concrete Product B");
        }
    }

    // 工厂接口
    interface Factory {
        Product createProduct();
    }

    // 具体工厂A
    static class ConcreteFactoryA implements Factory {
        @Override
        public Product createProduct() {
            return new ConcreteProductA();
        }
    }

    // 具体工厂B
    static class ConcreteFactoryB implements Factory {
        @Override
        public Product createProduct() {
            return new ConcreteProductB();
        }
    }

    // 测试类
    public static class AbstractFactoryDemo {
        public static void main(String[] args) {
            Factory factoryA = new ConcreteFactoryA();
            Product productA = factoryA.createProduct();
            productA.use();

            Factory factoryB = new ConcreteFactoryB();
            Product productB = factoryB.createProduct();
            productB.use();
        }
    }
}
