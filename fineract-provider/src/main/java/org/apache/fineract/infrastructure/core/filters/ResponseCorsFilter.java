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
package org.apache.fineract.infrastructure.core.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Filter that handles Cross-Origin Resource Sharing (CORS) for the platform API.
 * Only whitelisted origins receive CORS headers - wildcard (*) is no longer used.
 */
@Provider
@Component
@Scope("singleton")
@Slf4j
public class ResponseCorsFilter implements ContainerResponseFilter {

    @Autowired
    private FineractProperties fineractProperties;

    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {
        FineractProperties.FineractCorsProperties corsConfig = fineractProperties.getCors();

        // If CORS config is null or disabled, skip
        if (corsConfig == null || !corsConfig.isEnabled()) {
            return;
        }

        String origin = request.getHeaderString("Origin");

        // Not a CORS request if no Origin header
        if (origin == null || origin.isEmpty()) {
            return;
        }

        // Validate origin against whitelist
        if (corsConfig.isOriginAllowed(origin)) {
            // Set the specific origin, not wildcard
            response.getHeaders().add("Access-Control-Allow-Origin", origin);

            if (corsConfig.isAllowCredentials()) {
                response.getHeaders().add("Access-Control-Allow-Credentials", "true");
            }

            response.getHeaders().add("Access-Control-Allow-Methods",
                String.join(", ", corsConfig.getAllowedMethods()));

            response.getHeaders().add("Access-Control-Allow-Headers",
                String.join(", ", corsConfig.getAllowedHeaders()));

            response.getHeaders().add("Access-Control-Expose-Headers",
                String.join(", ", corsConfig.getExposedHeaders()));

            response.getHeaders().add("Access-Control-Max-Age",
                String.valueOf(corsConfig.getMaxAge()));

            log.debug("CORS allowed for origin: {}", origin);
        } else {
            // Log rejected CORS attempt
            if (corsConfig.isAuditLoggingEnabled()) {
                log.warn("SECURITY_AUDIT: CORS_REJECTED | origin={} | path={} | method={}",
                    origin,
                    request.getUriInfo().getPath(),
                    request.getMethod());
            }
            // Do NOT add any CORS headers - browser will block the request
        }
    }
}
