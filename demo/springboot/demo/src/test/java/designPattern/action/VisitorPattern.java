package designPattern.action;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/11/6 14:44
 * 访问者模式：
 * 它允许你在不改变被访问对象的情况下，对其结构进行操作。通过将操作封装到访问者类中，访问者模式可以使得添加新的操作变得更加灵活，而不需要修改被访问的对象结构
 */
public class VisitorPattern {

    // 访问者接口
    interface ShapeVisitor {
        void visit(Circle circle);
        void visit(Rectangle rectangle);
    }

    // 具体访问者类：计算面积访问者
    @Getter
    static class AreaCalculator implements ShapeVisitor {
        private double totalArea = 0;

        @Override
        public void visit(Circle circle) {
            totalArea += Math.PI * circle.getRadius() * circle.getRadius();
        }

        @Override
        public void visit(Rectangle rectangle) {
            totalArea += rectangle.getWidth() * rectangle.getHeight();
        }

    }

    // 元素接口
    interface Shape {
        void accept(ShapeVisitor visitor);
    }

    // 具体元素类：圆形
    @Getter
    static class Circle implements Shape {
        private final double radius;

        public Circle(double radius) {
            this.radius = radius;
        }

        @Override
        public void accept(ShapeVisitor visitor) {
            visitor.visit(this);
        }
    }

    // 具体元素类：矩形
    @Getter
    static class Rectangle implements Shape {
        private final double width;
        private final double height;

        public Rectangle(double width, double height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void accept(ShapeVisitor visitor) {
            visitor.visit(this);
        }
    }

    // 对象结构
    static class ShapeCollection {
        private final List<Shape> shapes = new ArrayList<>();

        public void addShape(Shape shape) {
            shapes.add(shape);
        }

        public void accept(ShapeVisitor visitor) {
            for (Shape shape : shapes) {
                shape.accept(visitor);
            }
        }
    }

    // 测试类
    public static class VisitorPatternDemo {
        public static void main(String[] args) {
            ShapeCollection shapes = new ShapeCollection();
            shapes.addShape(new Circle(5));
            shapes.addShape(new Rectangle(4, 6));

            AreaCalculator areaCalculator = new AreaCalculator();
            shapes.accept(areaCalculator);

            System.out.println("Total Area: " + areaCalculator.getTotalArea());
        }
    }

}
