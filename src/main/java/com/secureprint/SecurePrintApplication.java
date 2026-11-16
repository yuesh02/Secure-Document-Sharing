package com.secureprint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SecurePrintApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecurePrintApplication.class, args);
    }
}
