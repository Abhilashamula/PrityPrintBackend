package com.pingprint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PingPrintApplication {
    public static void main(String[] args) {
        SpringApplication.run(PingPrintApplication.class, args);
    }
}
