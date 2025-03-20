package com.happy.learning.zh.rbac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class HappyLearningRbacApplication {

    public static void main(String[] args) {
        SpringApplication.run(HappyLearningRbacApplication.class, args);
    }

}
