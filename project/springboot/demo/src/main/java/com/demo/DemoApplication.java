package com.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

// mybatis-plus
@MapperScan({
        // demo
        "com.demo.mapper",
})
@EnableCaching
@SpringBootApplication(
        scanBasePackages = {
                // 本地
                "com.demo",
        }
)
@EnableWebSocket
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
