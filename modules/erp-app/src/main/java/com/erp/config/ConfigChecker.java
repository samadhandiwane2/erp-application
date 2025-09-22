package com.erp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigChecker {

    @Value("${spring.mail.host:NOT_SET}")
    private String mailHost;

    @PostConstruct
    public void checkConfig() {
        System.out.println("Mail host configured as: " + mailHost);
    }
}
