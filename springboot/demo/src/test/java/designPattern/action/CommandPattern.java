package designPattern.action;

import lombok.Setter;

/**
 * @author 13225
 * @date 2025/11/6 14:20
 * 命令模式
 * 它将请求封装为一个对象，从而使你能够使用不同的请求、队列或日志请求，以及支持可撤销的操作
 * 就是Restful的Request请求
 */
public class CommandPattern {

    // 命令接口
    interface Command {
        void execute();
    }

    // 接收者类：灯
    static class Light {
        public void turnOn() {
            System.out.println("The light is ON");
        }

        public void turnOff() {
            System.out.println("The light is OFF");
        }
    }

    // 接收者类：风扇
    static class Fan {
        public void start() {
            System.out.println("The fan is ON");
        }

        public void stop() {
            System.out.println("The fan is OFF");
        }
    }

    // 具体命令类：打开灯命令
    static class LightOnCommand implements Command {
        private final Light light;

        public LightOnCommand(Light light) {
            this.light = light;
        }

        @Override
        public void execute() {
            light.turnOn();
        }
    }

    // 具体命令类：关闭灯命令
    static class LightOffCommand implements Command {
        private final Light light;

        public LightOffCommand(Light light) {
            this.light = light;
        }

        @Override
        public void execute() {
            light.turnOff();
        }
    }

    // 具体命令类：打开风扇命令
    static class FanOnCommand implements Command {
        private final Fan fan;

        public FanOnCommand(Fan fan) {
            this.fan = fan;
        }

        @Override
        public void execute() {
            fan.start();
        }
    }

    // 具体命令类：关闭风扇命令
    static class FanOffCommand implements Command {
        private final Fan fan;

        public FanOffCommand(Fan fan) {
            this.fan = fan;
        }

        @Override
        public void execute() {
            fan.stop();
        }
    }

    // 调用者类
    @Setter
    static class RemoteControl {
        private Command command;

        public void pressButton() {
            command.execute();
        }
    }

    // 测试类
    public static class CommandPatternDemo {
        public static void main(String[] args) {
            // 创建接收者
            Light light = new Light();
            Fan fan = new Fan();

            // 创建命令
            Command lightOn = new LightOnCommand(light);
            Command lightOff = new LightOffCommand(light);
            Command fanOn = new FanOnCommand(fan);
            Command fanOff = new FanOffCommand(fan);

            // 创建调用者
            RemoteControl remoteControl = new RemoteControl();

            // 使用遥控器控制灯和风扇
            remoteControl.setCommand(lightOn);
            remoteControl.pressButton();

            remoteControl.setCommand(fanOn);
            remoteControl.pressButton();

            remoteControl.setCommand(lightOff);
            remoteControl.pressButton();

            remoteControl.setCommand(fanOff);
            remoteControl.pressButton();
        }
    }

}
