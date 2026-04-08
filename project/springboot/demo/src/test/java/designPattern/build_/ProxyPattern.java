package designPattern.build_;

/**
 * @author 13225
 * @date 2025/11/6 13:02
 * 代理模式允许通过一个代理对象来控制对另一个对象的访问。这种模式常用于实现懒加载、访问控制、日志记录等功能
 */
public class ProxyPattern {

    // Subject接口
    interface Image {
        void display();
    }

    // 真实主题类
    static class RealImage implements Image {
        private final String filename;

        public RealImage(String filename) {
            this.filename = filename;
            loadImageFromDisk();
        }

        private void loadImageFromDisk() {
            System.out.println("Loading " + filename);
        }

        @Override
        public void display() {
            System.out.println("Displaying " + filename);
        }
    }

    // 代理类 (代理类来管理RealImage的生命周期，相当于ImageManager)
    static class ProxyImage implements Image {
        private RealImage realImage;
        private final String filename;

        public ProxyImage(String filename) {
            this.filename = filename;
        }

        @Override
        public void display() {
            // 只有在需要时才加载真实的图像
            if (realImage == null) {
                realImage = new RealImage(filename);
            }
            realImage.display();
        }
    }

    // 测试类
    public static class ProxyPatternDemo {
        public static void main(String[] args) {
            Image image1 = new ProxyImage("image1.jpg");
            Image image2 = new ProxyImage("image2.jpg");

            // 第一次调用时加载并显示图像
            image1.display();
            System.out.println("\n"); // 添加空行以便于输出分隔
            // 再次调用时直接显示图像，无需重新加载
            image1.display();
            System.out.println("\n"); // 添加空行以便于输出分隔
            // 第一次调用时加载并显示图像
            image2.display();
        }
    }

}
