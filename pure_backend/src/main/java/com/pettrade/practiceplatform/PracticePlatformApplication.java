package com.pettrade.practiceplatform;

import com.pettrade.practiceplatform.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
public class PracticePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PracticePlatformApplication.class, args);
    }
}
