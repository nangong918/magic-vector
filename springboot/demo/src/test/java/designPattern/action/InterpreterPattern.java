package designPattern.action;

import lombok.Getter;

/**
 * @author 13225
 * @date 2025/11/6 14:59
 * 解释器模式: 它提供了一个语言的文法表示，并定义了一个解释器来解释该语言中的句子。解释器模式通常用于设计一种特定的语言或表达式解析器，尤其适合于需要频繁解释或计算的场合
 */
public class InterpreterPattern {

    // 上下文类
    @Getter
    static class Context {
        private final String input;

        public Context(String input) {
            this.input = input;
        }

    }

    // 抽象表达式
    interface Expression {
        int interpret(Context context);
    }

    // 终结符表达式：数字
    static class NumberExpression implements Expression {
        private final int number;

        public NumberExpression(int number) {
            this.number = number;
        }

        @Override
        public int interpret(Context context) {
            return number;
        }
    }

    // 非终结符表达式：加法
    static class AddExpression implements Expression {
        private final Expression leftExpression;
        private final Expression rightExpression;

        public AddExpression(Expression left, Expression right) {
            this.leftExpression = left;
            this.rightExpression = right;
        }

        @Override
        public int interpret(Context context) {
            return leftExpression.interpret(context) + rightExpression.interpret(context);
        }
    }

    // 非终结符表达式：减法
    static class SubtractExpression implements Expression {
        private final Expression leftExpression;
        private final Expression rightExpression;

        public SubtractExpression(Expression left, Expression right) {
            this.leftExpression = left;
            this.rightExpression = right;
        }

        @Override
        public int interpret(Context context) {
            return leftExpression.interpret(context) - rightExpression.interpret(context);
        }
    }

    // 测试类
    public static class InterpreterPatternDemo {
        public static void main(String[] args) {
            // 构建表达式 (5 + 10 - 3)
            Expression expression = new SubtractExpression(
                    new AddExpression(new NumberExpression(5), new NumberExpression(10)),
                    new NumberExpression(3)
            );

            Context context = new Context("5 + 10 - 3");
            int result = expression.interpret(context);

            System.out.println("Result: " + result); // 输出: Result: 12
        }
    }

}
