/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.security.filter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.businessdate.service.BusinessDateReadPlatformService;
import org.apache.fineract.infrastructure.cache.domain.CacheType;
import org.apache.fineract.infrastructure.cache.service.CacheWritePlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.data.PlatformRequestLog;
import org.apache.fineract.infrastructure.security.exception.InvalidTenantIdentifierException;
import org.apache.fineract.infrastructure.security.service.BasicAuthTenantDetailsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.GenericFilterBean;

/**
 *
 * This filter is responsible for extracting multi-tenant from the request and setting Cross-Origin details to response.
 *
 * If multi-tenant are valid, the details of the tenant are stored in {@link FineractPlatformTenant} and stored in a
 * {@link ThreadLocal} variable for this request using {@link ThreadLocalContextUtil}.
 *
 * If multi-tenant are invalid, a http error response is returned.
 *
 * Used to support Oauth2 authentication and the service is loaded only when "oauth" profile is active.
 */
@Service
@ConditionalOnProperty("fineract.security.oauth.enabled")
@RequiredArgsConstructor
@Slf4j
public class TenantAwareTenantIdentifierFilter extends GenericFilterBean {

    private static AtomicBoolean firstRequestProcessed = new AtomicBoolean();

    private final BasicAuthTenantDetailsService basicAuthTenantDetailsService;
    private final ToApiJsonSerializer<PlatformRequestLog> toApiJsonSerializer;
    private final ConfigurationDomainService configurationDomainService;
    private final CacheWritePlatformService cacheWritePlatformService;
    private final BusinessDateReadPlatformService businessDateReadPlatformService;
    private final FineractProperties fineractProperties;

    private final String tenantRequestHeader = "Fineract-Platform-TenantId";
    private final boolean exceptionIfHeaderMissing = true;
    private final String apiUri = "/api/v1/";

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        final StopWatch task = new StopWatch();
        task.start();

        try {
            // Set CORS headers based on whitelist configuration
            setCorsHeaders(request, response);

            if (!"OPTIONS".equalsIgnoreCase(request.getMethod())) {

                String tenantIdentifier = request.getHeader(this.tenantRequestHeader);
                if (org.apache.commons.lang3.StringUtils.isBlank(tenantIdentifier)) {
                    tenantIdentifier = request.getParameter("tenantIdentifier");
                }

                if (tenantIdentifier == null && this.exceptionIfHeaderMissing) {
                    throw new InvalidTenantIdentifierException("No tenant identifier found: Add request header of '"
                            + this.tenantRequestHeader + "' or add the parameter 'tenantIdentifier' to query string of request URL.");
                }

                String pathInfo = request.getRequestURI();
                boolean isReportRequest = false;
                if (pathInfo != null && pathInfo.contains("report")) {
                    isReportRequest = true;
                }
                final FineractPlatformTenant tenant = this.basicAuthTenantDetailsService.loadTenantById(tenantIdentifier, isReportRequest);
                ThreadLocalContextUtil.setTenant(tenant);
                HashMap<BusinessDateType, LocalDate> businessDates = this.businessDateReadPlatformService.getBusinessDates();
                ThreadLocalContextUtil.setBusinessDates(businessDates);
                String authToken = request.getHeader("Authorization");

                if (authToken != null && authToken.startsWith("bearer ")) {
                    ThreadLocalContextUtil.setAuthToken(authToken.replaceFirst("bearer ", ""));
                }

                if (!firstRequestProcessed.get()) {
                    final String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(),
                            request.getContextPath() + apiUri);
                    System.setProperty("baseUrl", baseUrl);

                    final boolean ehcacheEnabled = this.configurationDomainService.isEhcacheEnabled();
                    if (ehcacheEnabled) {
                        this.cacheWritePlatformService.switchToCache(CacheType.SINGLE_NODE);
                    } else {
                        this.cacheWritePlatformService.switchToCache(CacheType.NO_CACHE);
                    }
                    firstRequestProcessed.set(true);
                }
                chain.doFilter(request, response);
            }
        } catch (final InvalidTenantIdentifierException e) {

            log.error("Invalid tenant identifier: {}", e.getMessage());
            // deal with exception at low level
            SecurityContextHolder.getContext().setAuthentication(null);

            response.addHeader("WWW-Authenticate", "Basic realm=\"" + "Fineract Platform API" + "\"");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid tenant identifier.");
        } finally {
            task.stop();
            final PlatformRequestLog logRequest = PlatformRequestLog.from(task, request);
            log.info("{}", this.toApiJsonSerializer.serialize(logRequest));
        }

    }

    /**
     * Sets CORS headers based on configured whitelist.
     * No longer uses wildcard (*) - only explicitly allowed origins get CORS headers.
     */
    private void setCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        FineractProperties.FineractCorsProperties corsConfig = fineractProperties.getCors();

        if (corsConfig == null || !corsConfig.isEnabled()) {
            return;
        }

        String origin = request.getHeader("Origin");

        if (origin == null || origin.isEmpty()) {
            return;
        }

        if (corsConfig.isOriginAllowed(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);

            if (corsConfig.isAllowCredentials()) {
                response.setHeader("Access-Control-Allow-Credentials", "true");
            }

            response.setHeader("Access-Control-Allow-Methods",
                String.join(", ", corsConfig.getAllowedMethods()));

            response.setHeader("Access-Control-Allow-Headers",
                String.join(", ", corsConfig.getAllowedHeaders()));

            response.setHeader("Access-Control-Expose-Headers",
                String.join(", ", corsConfig.getExposedHeaders()));

            response.setHeader("Access-Control-Max-Age",
                String.valueOf(corsConfig.getMaxAge()));

            log.debug("CORS allowed for origin: {}", origin);
        } else {
            if (corsConfig.isAuditLoggingEnabled()) {
                log.warn("SECURITY_AUDIT: CORS_REJECTED | origin={} | path={} | method={}",
                    origin, request.getRequestURI(), request.getMethod());
            }
            // Do NOT add any CORS headers - browser will block the request
        }
    }
}
