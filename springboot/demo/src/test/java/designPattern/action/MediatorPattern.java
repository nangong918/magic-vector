package designPattern.action;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/11/6 14:52
 * 中介模式: 管理用户之间的通讯数据
 */
public class MediatorPattern {

    // 中介者接口
    interface ChatMediator {
        void sendMessage(String message, User user);
        void addUser(User user);
    }

    // 具体中介者类
    static class ChatRoom implements ChatMediator {
        private final List<User> users = new ArrayList<>();

        @Override
        public void sendMessage(String message, User user) {
            for (User u : users) {
                // 不将消息发送给发送者
                if (u != user) {
                    u.receive(message);
                }
            }
        }

        @Override
        public void addUser(User user) {
            users.add(user);
        }
    }

    // 组件接口
    static abstract class User {
        protected ChatMediator mediator;
        protected String name;

        public User(ChatMediator mediator, String name) {
            this.mediator = mediator;
            this.name = name;
        }

        public abstract void send(String message);
        public abstract void receive(String message);
    }

    // 具体组件类：用户A
    static class UserA extends User {
        public UserA(ChatMediator mediator, String name) {
            super(mediator, name);
        }

        @Override
        public void send(String message) {
            System.out.println(name + ": Sending Message = " + message);
            mediator.sendMessage(message, this);
        }

        @Override
        public void receive(String message) {
            System.out.println(name + ": Received Message = " + message);
        }
    }

    // 具体组件类：用户B
    static class UserB extends User {
        public UserB(ChatMediator mediator, String name) {
            super(mediator, name);
        }

        @Override
        public void send(String message) {
            System.out.println(name + ": Sending Message = " + message);
            mediator.sendMessage(message, this);
        }

        @Override
        public void receive(String message) {
            System.out.println(name + ": Received Message = " + message);
        }
    }

    // 测试类
    public static class MediatorPatternDemo {
        public static void main(String[] args) {
            ChatMediator mediator = new ChatRoom();

            User userA = new UserA(mediator, "User A");
            User userB = new UserB(mediator, "User B");

            mediator.addUser(userA);
            mediator.addUser(userB);

            userA.send("Hello, User B!");
            userB.send("Hi, User A!");
        }
    }

}
