package com.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.erp.common", "com.erp.security", "com.erp.admin", "com.erp.tenant", "com.erp"})
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {"com.erp.common.repository", "com.erp.security.repository", "com.erp.admin.repository", "com.erp.tenant.repository"})
@EntityScan(basePackages = {"com.erp.common.entity", "com.erp.security.entity", "com.erp.admin.entity", "com.erp.tenant.entity"})
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }

}
