package designPattern.build_;

/**
 * @author 13225
 * @date 2025/11/6 12:58
 * 装饰器模式的本质确实是通过传入一个对象，并对其进行增强或修改，而不需要改变对象的结构。C/Cpp经常用到
 */
public class DecoratorPattern {

    // 组件接口
    interface Coffee {
        String getDescription();
        double cost();
    }

    // 具体组件类
    static class SimpleCoffee implements Coffee {
        @Override
        public String getDescription() {
            return "Simple Coffee";
        }

        @Override
        public double cost() {
            return 2.0; // 基础咖啡的价格
        }
    }

    // 装饰器抽象类
    abstract static class CoffeeDecorator implements Coffee {
        protected Coffee coffee;

        public CoffeeDecorator(Coffee coffee) {
            this.coffee = coffee;
        }

        @Override
        public String getDescription() {
            return coffee.getDescription();
        }

        @Override
        public double cost() {
            return coffee.cost();
        }
    }

    // 具体装饰器类：添加牛奶
    static class MilkDecorator extends CoffeeDecorator {
        public MilkDecorator(Coffee coffee) {
            super(coffee);
        }

        @Override
        public String getDescription() {
            return coffee.getDescription() + ", Milk";
        }

        @Override
        public double cost() {
            return coffee.cost() + 0.5; // 牛奶的额外费用
        }
    }

    // 具体装饰器类：添加糖
    static class SugarDecorator extends CoffeeDecorator {
        public SugarDecorator(Coffee coffee) {
            super(coffee);
        }

        @Override
        public String getDescription() {
            return coffee.getDescription() + ", Sugar";
        }

        @Override
        public double cost() {
            return coffee.cost() + 0.2; // 糖的额外费用
        }
    }

    // 测试类
    public static class DecoratorPatternDemo {
        public static void main(String[] args) {
            // 创建基础咖啡
            Coffee coffee = new SimpleCoffee();
            System.out.println(coffee.getDescription() + " $" + coffee.cost());

            // 添加牛奶
            coffee = new MilkDecorator(coffee);
            System.out.println(coffee.getDescription() + " $" + coffee.cost());

            // 添加糖
            coffee = new SugarDecorator(coffee);
            System.out.println(coffee.getDescription() + " $" + coffee.cost());
        }
    }

}
