package com.happy.learning.zh.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class HappyLearningAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(HappyLearningAuthApplication.class, args);
    }

}
