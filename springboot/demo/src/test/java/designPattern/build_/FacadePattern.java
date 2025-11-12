package designPattern.build_;

/**
 * @author 13225
 * @date 2025/11/6 13:12
 * 外观模式
 * 外观模式的本质确实可以看作是一个“管理者”（或称为“门面”）类，它对多个子系统对象进行管理，并向外部提供一个统一的接口，从而简化了对这些子系统的访问
 */
public class FacadePattern {

    // 子系统类：音响
    static class SoundSystem {
        public void turnOn() {
            System.out.println("Sound system is turned on.");
        }

        public void turnOff() {
            System.out.println("Sound system is turned off.");
        }

        public void setVolume(int level) {
            System.out.println("Sound volume set to " + level + ".");
        }
    }

    // 子系统类：投影仪
    static class Projector {
        public void turnOn() {
            System.out.println("Projector is turned on.");
        }

        public void turnOff() {
            System.out.println("Projector is turned off.");
        }

        public void setInput(String input) {
            System.out.println("Input set to " + input + " on projector.");
        }
    }

    // 子系统类：DVD播放器
    static class DVDPlayer {
        public void turnOn() {
            System.out.println("DVD player is turned on.");
        }

        public void turnOff() {
            System.out.println("DVD player is turned off.");
        }

        public void playMovie(String movie) {
            System.out.println("Playing movie: " + movie);
        }
    }

    // 外观类
    static class HomeTheaterFacade {
        private final SoundSystem soundSystem;
        private final Projector projector;
        private final DVDPlayer dvdPlayer;

        public HomeTheaterFacade(SoundSystem soundSystem, Projector projector, DVDPlayer dvdPlayer) {
            this.soundSystem = soundSystem;
            this.projector = projector;
            this.dvdPlayer = dvdPlayer;
        }

        public void watchMovie(String movie) {
            System.out.println("Get ready to watch a movie...");
            projector.turnOn();
            projector.setInput("DVD");
            soundSystem.turnOn();
            soundSystem.setVolume(5);
            dvdPlayer.turnOn();
            dvdPlayer.playMovie(movie);
        }

        public void endMovie() {
            System.out.println("Shutting down the home theater...");
            dvdPlayer.turnOff();
            soundSystem.turnOff();
            projector.turnOff();
        }
    }

    // 测试类
    public static class FacadePatternDemo {
        public static void main(String[] args) {
            SoundSystem soundSystem = new SoundSystem();
            Projector projector = new Projector();
            DVDPlayer dvdPlayer = new DVDPlayer();

            HomeTheaterFacade homeTheater = new HomeTheaterFacade(soundSystem, projector, dvdPlayer);

            homeTheater.watchMovie("Inception");
            System.out.println();
            homeTheater.endMovie();
        }
    }

}
