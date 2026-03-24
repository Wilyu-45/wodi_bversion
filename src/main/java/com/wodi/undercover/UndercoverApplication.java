package com.wodi.undercover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class UndercoverApplication {
    public static void main(String[] args) {
        SpringApplication.run(UndercoverApplication.class, args);
    }
}
