package com.erp.common.config;

import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfiguration implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(new ContextCopyingDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * Decorator that copies ThreadLocal context to async threads
     */
    public static class ContextCopyingDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture context from parent thread
            String tenantCode = TenantContext.getCurrentTenant();
            Long tenantId = TenantContext.getCurrentTenantId();
            String schemaContext = SchemaContext.getCurrentSchema();

            log.debug("Capturing context for async task - Tenant: {}, Schema: {}",
                    tenantCode, schemaContext);

            return () -> {
                try {
                    // Set context in child thread
                    if (tenantCode != null) {
                        TenantContext.setCurrentTenant(tenantId, tenantCode, null);
                        log.debug("Restored tenant context in async thread: {}", tenantCode);
                    }
                    if (schemaContext != null) {
                        SchemaContext.setSchema(schemaContext);
                        log.debug("Restored schema context in async thread: {}", schemaContext);
                    }

                    // Execute the actual task
                    runnable.run();

                } finally {
                    // Clean up context after task
                    TenantContext.clear();
                    SchemaContext.clear();
                    log.debug("Cleared context after async task completion");
                }
            };
        }
    }

}