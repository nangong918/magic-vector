package designPattern.action;

import lombok.Setter;

/**
 * @author 13225
 * @date 2025/11/6 13:50
 * 策略模式
 * 封装算法
 */
public class StrategyPattern {

    // 策略接口
    interface TextFormatter {
        String format(String text);
    }

    // 具体策略类：大写格式化
    static class UpperCaseFormatter implements TextFormatter {
        @Override
        public String format(String text) {
            return text.toUpperCase();
        }
    }

    // 具体策略类：小写格式化
    static class LowerCaseFormatter implements TextFormatter {
        @Override
        public String format(String text) {
            return text.toLowerCase();
        }
    }

    // 具体策略类：标题格式化
    static class TitleCaseFormatter implements TextFormatter {
        @Override
        public String format(String text) {
            String[] words = text.split(" ");
            StringBuilder titleCase = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    titleCase.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase()).append(" ");
                }
            }
            return titleCase.toString().trim();
        }
    }

    // 上下文类
    @Setter
    static class TextEditor {
        private TextFormatter formatter;

        public void formatText(String text) {
            if (formatter != null) {
                String formattedText = formatter.format(text);
                System.out.println("Formatted Text: " + formattedText);
            } else {
                System.out.println("No formatting strategy set.");
            }
        }
    }

    // 测试类
    public static class StrategyPatternDemo {
        public static void main(String[] args) {
            TextEditor editor = new TextEditor();

            // 使用大写格式化策略
            editor.setFormatter(new UpperCaseFormatter());
            editor.formatText("hello world");

            // 使用小写格式化策略
            editor.setFormatter(new LowerCaseFormatter());
            editor.formatText("Hello World");

            // 使用标题格式化策略
            editor.setFormatter(new TitleCaseFormatter());
            editor.formatText("hello world");
        }
    }

}
