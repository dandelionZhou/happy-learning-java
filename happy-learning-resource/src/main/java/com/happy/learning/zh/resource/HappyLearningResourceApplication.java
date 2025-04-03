package com.happy.learning.zh.resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.happy.learning.zh.resource.feign")
@EnableDiscoveryClient
public class HappyLearningResourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HappyLearningResourceApplication.class, args);
    }

}
