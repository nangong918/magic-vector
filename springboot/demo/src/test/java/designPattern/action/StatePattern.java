package designPattern.action;

import lombok.Setter;

/**
 * @author 13225
 * @date 2025/11/6 14:39
 * 状态设计模式：内部的状态更变向外同步。Android的Activity生命周期就是如此，其他的生命周期管理也需要使用此设计模式
 */
public class StatePattern {

    // 状态接口
    interface State {
        void play(MusicPlayer player);
        void pause(MusicPlayer player);
        void stop(MusicPlayer player);
    }

    // 具体状态类：播放状态
    static class PlayingState implements State {
        @Override
        public void play(MusicPlayer player) {
            System.out.println("Already playing.");
        }

        @Override
        public void pause(MusicPlayer player) {
            System.out.println("Pausing the music.");
            player.setState(new PausedState());
        }

        @Override
        public void stop(MusicPlayer player) {
            System.out.println("Stopping the music.");
            player.setState(new StoppedState());
        }
    }

    // 具体状态类：暂停状态
    static class PausedState implements State {
        @Override
        public void play(MusicPlayer player) {
            System.out.println("Resuming the music.");
            player.setState(new PlayingState());
        }

        @Override
        public void pause(MusicPlayer player) {
            System.out.println("Already paused.");
        }

        @Override
        public void stop(MusicPlayer player) {
            System.out.println("Stopping the music.");
            player.setState(new StoppedState());
        }
    }

    // 具体状态类：停止状态
    static class StoppedState implements State {
        @Override
        public void play(MusicPlayer player) {
            System.out.println("Starting the music.");
            player.setState(new PlayingState());
        }

        @Override
        public void pause(MusicPlayer player) {
            System.out.println("Can't pause. Music is stopped.");
        }

        @Override
        public void stop(MusicPlayer player) {
            System.out.println("Already stopped.");
        }
    }

    // 上下文类
    @Setter
    static class MusicPlayer {
        private State state;

        public MusicPlayer() {
            state = new StoppedState(); // 初始状态
        }

        public void play() {
            state.play(this);
        }

        public void pause() {
            state.pause(this);
        }

        public void stop() {
            state.stop(this);
        }
    }

    // 测试类
    public static class StatePatternDemo {
        public static void main(String[] args) {
            MusicPlayer player = new MusicPlayer();

            player.play();  // Starting the music.
            player.pause(); // Pausing the music.
            player.play();  // Resuming the music.
            player.stop();  // Stopping the music.
            player.pause(); // Can't pause. Music is stopped.
        }
    }

}
