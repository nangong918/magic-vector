package designPattern.create;

/**
 * @author 13225
 * @date 2025/11/6 11:17
 * 工厂模式：设计对象的属性，然后根据创建不同的对象实现设计的属性。
 * 工厂方法模式有一个问题就是，类的创建依赖工厂类，也就是说，如果想要拓展程序，必须对工厂类进行修改，这违背了开闭原则
 */
public class FactoryMethod {
    // FactoryMethodDemo.java

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

    // 工厂 (问题：工厂是具体的类，如果想要拓展工厂的功能就必须修改具体的类，这违背了开闭原则)
    static class ConcreteFactory {
        public Product createProduct(String type) {
            if (type.equals("A")) {
                return new ConcreteProductA();
            }
            else if (type.equals("B")) {
                return new ConcreteProductB();
            }
            return null;
        }
    }


    // 测试类
    public static class FactoryMethodDemo {
        public static void main(String[] args) {
            ConcreteFactory factory = new ConcreteFactory();
            Product productA = factory.createProduct("A");
            productA.use();
            Product productB = factory.createProduct("B");
            productB.use();
        }
    }
}
