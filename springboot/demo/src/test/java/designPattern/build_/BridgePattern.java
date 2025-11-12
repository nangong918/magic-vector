package designPattern.build_;

/**
 * @author 13225
 * @date 2025/11/6 13:31
 * 桥接模式
 * 桥接模式就是将一个具体的对象拆分为多个接口
 * 假设我们有一个绘图应用程序，可以绘制不同形状（如圆形和正方形），并通过不同的颜色（如红色和蓝色）进行填充。桥接模式可以帮助我们将形状接口和颜色接口的实现分离开来
 */
public class BridgePattern {

    // 实现部分接口
    interface Color {
        String applyColor();
    }

    // 具体实现类：红色
    static class Red implements Color {
        @Override
        public String applyColor() {
            return "Red";
        }
    }

    // 具体实现类：蓝色
    static class Blue implements Color {
        @Override
        public String applyColor() {
            return "Blue";
        }
    }

    // 抽象部分类
    static abstract class Shape {
        protected Color color;

        protected Shape(Color color) {
            this.color = color;
        }

        abstract void draw();
    }

    // 具体抽象类：圆形
    static class Circle extends Shape {
        public Circle(Color color) {
            super(color);
        }

        @Override
        public void draw() {
            System.out.println("Drawing Circle in color: " + color.applyColor());
        }
    }

    // 具体抽象类：正方形
    static class Square extends Shape {
        public Square(Color color) {
            super(color);
        }

        @Override
        public void draw() {
            System.out.println("Drawing Square in color: " + color.applyColor());
        }
    }

    // 测试类
    public static class BridgePatternDemo {
        public static void main(String[] args) {
            Shape redCircle = new Circle(new Red());
            Shape blueSquare = new Square(new Blue());

            redCircle.draw();
            blueSquare.draw();
        }
    }

}
