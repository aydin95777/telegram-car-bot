package com.telegrambot.carbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CarBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarBotApplication.class, args);
    }
}
