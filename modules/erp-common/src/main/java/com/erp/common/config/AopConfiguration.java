package com.erp.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true) // Force CGLIB proxies
public class AopConfiguration {
    // This ensures AOP is properly configured
    // proxyTargetClass = true forces CGLIB proxies which work better with @Repository interfaces
}