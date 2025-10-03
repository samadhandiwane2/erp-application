package com.erp.common.monitoring;

import com.erp.common.config.MultiTenantDataSourceConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConnectionPoolMonitor {

    private final MultiTenantDataSourceConfig dataSourceConfig;

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorConnectionPools() {
        Map<Object, Object> dataSources = dataSourceConfig.getDataSources();

        for (Map.Entry<Object, Object> entry : dataSources.entrySet()) {
            if (entry.getValue() instanceof HikariDataSource) {
                HikariDataSource ds = (HikariDataSource) entry.getValue();
                HikariPoolMXBean pool = ds.getHikariPoolMXBean();

                int active = pool.getActiveConnections();
                int idle = pool.getIdleConnections();
                int waiting = pool.getThreadsAwaitingConnection();

                if (waiting > 0) {
                    log.warn("Pool [{}]: {} threads waiting for connection! Active={}, Idle={}",
                            entry.getKey(), waiting, active, idle);
                } else {
                    log.debug("Pool [{}]: Active={}, Idle={}, Total={}",
                            entry.getKey(), active, idle, active + idle);
                }
            }
        }
    }
}