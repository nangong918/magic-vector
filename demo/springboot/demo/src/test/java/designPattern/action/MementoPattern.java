package designPattern.action;

import lombok.Getter;
import lombok.Setter;

/**
 * @author 13225
 * @date 2025/11/6 14:34
 * 备忘录模式: 创建一个可以撤销的缓存
 */
public class MementoPattern {

    // 备忘录类
    @Getter
    static class Memento {
        private final String state;

        public Memento(String state) {
            this.state = state;
        }

    }

    // 发起人类
    @Getter
    @Setter
    static class TextEditor {
        private String text;

        // 创建备忘录
        public Memento save() {
            return new Memento(text);
        }

        // 恢复状态
        public void restore(Memento memento) {
            this.text = memento.getState();
        }
    }

    // 管理者类
    static class Caretaker {
        private Memento memento;

        public void saveState(TextEditor editor) {
            memento = editor.save();
        }

        public void restoreState(TextEditor editor) {
            editor.restore(memento);
        }
    }

    // 测试类
    public static class MementoPatternDemo {
        public static void main(String[] args) {
            TextEditor editor = new TextEditor();
            Caretaker caretaker = new Caretaker();

            // 设置文本并保存状态
            editor.setText("Hello, world!");
            caretaker.saveState(editor);
            System.out.println("Current Text: " + editor.getText());

            // 修改文本
            editor.setText("Hello, Memento Pattern!");
            System.out.println("Current Text: " + editor.getText());

            // 恢复状态
            caretaker.restoreState(editor);
            System.out.println("Restored Text: " + editor.getText());
        }
    }

}
