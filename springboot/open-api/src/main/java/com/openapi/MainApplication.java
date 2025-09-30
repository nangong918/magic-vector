package com.openapi;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

// mybatis-plus
@MapperScan({
        // minio
        "com.minio.mapper",
})
@EnableCaching
@SpringBootApplication(
        scanBasePackages = {
                // 本地
                "com.openapi",
                // minio
                "com.minio",
        }
)
public class MainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

}
