package com.happy.learning.zh.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/oauth/**")
                .allowedOrigins("https://frontend.com")
                .allowedMethods("POST", "GET")
                .allowedHeaders("Authorization", "Content-Type");

        registry.addMapping("/sms/**")
                .allowedOrigins("https://frontend.com")
                .allowedMethods("POST", "GET")
                .allowedHeaders("Authorization", "Content-Type");
    }
}
