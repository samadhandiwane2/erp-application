package com.erp.common.filter;

import com.erp.common.context.SchemaContext;
import com.erp.common.context.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class RequestContextLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);

        try {
            chain.doFilter(request, response);
        } finally {
            // Log final context state for debugging
            if (log.isDebugEnabled()) {
                log.debug("[{}] {} {} - Final: Tenant={}, Schema={}",
                        requestId,
                        httpRequest.getMethod(),
                        httpRequest.getRequestURI(),
                        TenantContext.getCurrentTenant(),
                        SchemaContext.getCurrentSchema()
                );
            }
        }
    }

}