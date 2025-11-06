package designPattern;

/**
 * @author 13225
 * @date 2025/11/6 11:45
 * 单例模式：保证一个类只有一个实例，并提供一个全局访问点。对资源统一管理。
 * Spring框架的@Component和@Service都是单例模式。注意有些时候需要避免单例模式，比如Websocket的每个链接都是独立的而不是单例。所以数据不能作为单例管理。
 */
public class SingletonPattern {
    // SingletonDemo.java

    // 单例类
    static class Singleton {
        // 私有静态变量，保存单例实例
        private static Singleton instance;

        // 私有构造函数，防止外部实例化
        private Singleton() {}

        // 公共静态方法，提供全局访问点
        public static synchronized Singleton getInstance() {
            if (instance == null) {
                instance = new Singleton();
            }
            return instance;
        }

        // 示例方法
        public void showMessage() {
            System.out.println("Hello from Singleton!");
        }
    }

    // 测试类
    public static class SingletonDemo {
        public static void main(String[] args) {
            // 获取单例实例
            Singleton singleton = Singleton.getInstance();
            singleton.showMessage();
        }
    }
}
