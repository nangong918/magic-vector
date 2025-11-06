package designPattern.create;

import lombok.Builder;
import lombok.ToString;

/**
 * @author 13225
 * @date 2025/11/6 11:47
 * 建造者模式：将一个复杂对象的构建与它的表示分离，使得同样的构建过程可以创建不同的表示。
 */
public class BuilderPattern {

    // BuilderDemo.java

    // 复杂对象类
    static class Product {
        private final String partA;
        private final String partB;
        private final String partC;

        // Private constructor to enforce the use of Builder
        private Product(Builder builder) {
            this.partA = builder.partA;
            this.partB = builder.partB;
            this.partC = builder.partC;
        }

        @Override
        public String toString() {
            return "Product{" +
                    "partA='" + partA + '\'' +
                    ", partB='" + partB + '\'' +
                    ", partC='" + partC + '\'' +
                    '}';
        }

        // 静态内部 Builder 类
        static class Builder {
            private String partA;
            private String partB;
            private String partC;

            public Builder setPartA(String partA) {
                this.partA = partA;
                return this;
            }

            public Builder setPartB(String partB) {
                this.partB = partB;
                return this;
            }

            public Builder setPartC(String partC) {
                this.partC = partC;
                return this;
            }

            public Product build() {
                return new Product(this);
            }
        }
    }

    // lombok实现
    @Builder
    @ToString
    static class Product2 {
        private String partA;
        private String partB;
        private String partC;
    }

    // 测试类
    public static class BuilderDemo {
        public static void main(String[] args) {
            // 使用建造者模式创建复杂对象
            Product product = new Product.Builder()
                    .setPartA("Part A")
                    .setPartB("Part B")
                    .setPartC("Part C")
                    .build();

            System.out.println(product);

            // 使用 Lombok 的 Builder 创建复杂对象
            Product2 product2 = Product2.builder()
                    .partA("Part A")
                    .partB("Part B")
                    .partC("Part C")
                    .build();

            System.out.println(product2);
        }
    }

}
