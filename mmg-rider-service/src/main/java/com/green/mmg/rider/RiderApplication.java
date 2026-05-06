package com.green.mmg.rider;

import com.green.mmg.rider.config.RiderProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = {"com.green.mmg.rider", "com.green.mmg.common"})
@EnableJpaAuditing
@EnableConfigurationProperties(RiderProperties.class)
public class RiderApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiderApplication.class, args);
    }
}
