package designPattern.build_;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 13225
 * @date 2025/11/6 13:41
 * 享元模式：它通过共享对象来减少内存使用和提高性能。享元模式适合于需要大量相似对象的场景，它通过将对象的共享内存部分与可变部分分开，从而实现高效的资源管理。
 * 不就是map找对象吗？Websocket的SessionManager就是这个模式
 */
public class FlyweightPattern {

    // 享元接口
    interface Shape {
        void draw(int x, int y);
    }

    // 具体享元类：圆形
    static class Circle implements Shape {
        private final String color; // 共享的状态

        public Circle(String color) {
            this.color = color;
        }

        @Override
        public void draw(int x, int y) {
            System.out.println("Circle: Drawn at (" + x + ", " + y + ") with color " + color);
        }
    }

    // 享元工厂
    static class ShapeFactory {
        private final Map<String, Shape> shapes = new HashMap<>();

        public Shape getCircle(String color) {
            Shape circle = shapes.get(color);
            if (circle == null) {
                circle = new Circle(color);
                shapes.put(color, circle);
                System.out.println("Creating circle of color: " + color);
            }
            return circle;
        }
    }

    // 测试类
    public static class FlyweightPatternDemo {
        public static void main(String[] args) {
            ShapeFactory shapeFactory = new ShapeFactory();

            // 创建和使用不同颜色的圆
            Shape redCircle = shapeFactory.getCircle("Red");
            redCircle.draw(10, 20);

            Shape greenCircle = shapeFactory.getCircle("Green");
            greenCircle.draw(30, 40);

            Shape anotherRedCircle = shapeFactory.getCircle("Red");
            anotherRedCircle.draw(50, 60);

            // 检查是否共享同一个对象
            System.out.println("Are both red circles the same object? " + (redCircle == anotherRedCircle));
        }
    }

}
