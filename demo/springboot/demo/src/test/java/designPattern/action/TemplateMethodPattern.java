package designPattern.action;

/**
 * @author 13225
 * @date 2025/11/6 13:52
 * 提前定义一个方法，按照模板方法进行执行
 * Android的Activity生命周期就是一个模板方法，允许开发者重写，但是不能改变生命周期调用顺序
 * 必须使用abstract类 + final method，不能使用interface + default method，因为这样方法就可变，违背了模板方法不可变的规则。
 */
public class TemplateMethodPattern {

    // 抽象类 (虽然接口也能实现，但是default方法可变，违背了模板方法的方法不可变规则)
    interface BeverageInterface {
        // 模板方法
        default void prepareRecipe() {
            boilWater();
            brew();
            pourInCup();
            addCondiments();
        }

        // 具体步骤
        private void boilWater() {
            System.out.println("Boiling water");
        }

        // 抽象方法：冲泡
        void brew();

        // 具体步骤
        private void pourInCup() {
            System.out.println("Pouring into cup");
        }

        // 抽象方法：添加配料
        void addCondiments();
    }

    // 抽象类
    abstract static class Beverage {
        // 模板方法
        public final void prepareRecipe() {
            boilWater();
            brew();
            pourInCup();
            addCondiments();
        }

        // 具体步骤
        private void boilWater() {
            System.out.println("Boiling water");
        }

        // 抽象方法：冲泡
        protected abstract void brew();

        // 具体步骤
        private void pourInCup() {
            System.out.println("Pouring into cup");
        }

        // 抽象方法：添加配料
        protected abstract void addCondiments();
    }

    // 具体类：茶
    static class Tea extends Beverage {
        @Override
        protected void brew() {
            System.out.println("Steeping the tea");
        }

        @Override
        protected void addCondiments() {
            System.out.println("Adding lemon");
        }
    }

    // 具体类：咖啡
    static class Coffee extends Beverage {
        @Override
        protected void brew() {
            System.out.println("Dripping coffee through filter");
        }

        @Override
        protected void addCondiments() {
            System.out.println("Adding sugar and milk");
        }
    }

    // 测试类
    public static class TemplateMethodPatternDemo {
        public static void main(String[] args) {
            Beverage tea = new Tea();
            tea.prepareRecipe();

            System.out.println();

            Beverage coffee = new Coffee();
            coffee.prepareRecipe();
        }
    }

}
