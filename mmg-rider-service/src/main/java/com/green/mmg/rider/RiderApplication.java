package com.green.mmg.rider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.green.mmg.rider", "com.green.mmg.common"})
public class RiderApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiderApplication.class, args);
    }
}
