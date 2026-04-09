package designPattern.action;

/**
 * @author 13225
 * @date 2025/11/6 14:13
 * 责任链模式，找到一个能处理的对象
 */
public class ChainOfResponsibilityPattern {

    // 处理器接口
    interface Logger {
        void setNext(Logger nextLogger);
        void logMessage(int level, String message);
    }

    // 具体处理器类：控制台日志
    static class ConsoleLogger implements Logger {
        private final int level;
        private Logger nextLogger;

        public ConsoleLogger(int level) {
            this.level = level;
        }

        @Override
        public void setNext(Logger nextLogger) {
            this.nextLogger = nextLogger;
        }

        @Override
        public void logMessage(int level, String message) {
            if (this.level <= level) {
                System.out.println("Console Logger: " + message);
            }
            if (nextLogger != null) {
                nextLogger.logMessage(level, message);
            }
        }
    }

    // 具体处理器类：文件日志
    static class FileLogger implements Logger {
        private final int level;
        private Logger nextLogger;

        public FileLogger(int level) {
            this.level = level;
        }

        @Override
        public void setNext(Logger nextLogger) {
            this.nextLogger = nextLogger;
        }

        @Override
        public void logMessage(int level, String message) {
            if (this.level <= level) {
                System.out.println("File Logger: " + message);
            }
            if (nextLogger != null) {
                nextLogger.logMessage(level, message);
            }
        }
    }

    // 具体处理器类：数据库日志
    static class DatabaseLogger implements Logger {
        private final int level;
        private Logger nextLogger;

        public DatabaseLogger(int level) {
            this.level = level;
        }

        @Override
        public void setNext(Logger nextLogger) {
            this.nextLogger = nextLogger;
        }

        @Override
        public void logMessage(int level, String message) {
            if (this.level <= level) {
                System.out.println("Database Logger: " + message);
            }
            if (nextLogger != null) {
                nextLogger.logMessage(level, message);
            }
        }
    }

    // 测试类
    public static class ChainOfResponsibilityDemo {
        public static void main(String[] args) {
            // 创建处理器
            Logger consoleLogger = new ConsoleLogger(1);
            Logger fileLogger = new FileLogger(2);
            Logger databaseLogger = new DatabaseLogger(3);

            // 设置责任链
            consoleLogger.setNext(fileLogger);
            fileLogger.setNext(databaseLogger);

            // 发送请求
            System.out.println("Logging level 1:");
            consoleLogger.logMessage(1, "This is an info message.");

            System.out.println("\nLogging level 2:");
            consoleLogger.logMessage(2, "This is a debug message.");

            System.out.println("\nLogging level 3:");
            consoleLogger.logMessage(3, "This is an error message.");
        }
    }

}
