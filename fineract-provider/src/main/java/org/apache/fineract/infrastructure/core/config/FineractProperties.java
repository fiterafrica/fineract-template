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

package org.apache.fineract.infrastructure.core.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "fineract")
public class FineractProperties {

    private String nodeId;

    private FineractTenantProperties tenant;

    private FineractModeProperties mode;

    private FineractCorrelationProperties correlation;

    private FineractContentProperties content;

    private FineractTemplateProperties template;

    private FineractSupportedProperties supported;

    private FineractCorsProperties cors;

    @Getter
    @Setter
    public static class FineractTenantProperties {

        private String host;
        private Integer port;
        private String username;
        private String password;
        private String parameters;
        private String timezone;
        private String identifier;
        private String name;
        private String description;
        private String protocol;
        private String subprotocol;
    }

    @Getter
    @Setter
    public static class FineractModeProperties {

        private boolean readEnabled;
        private boolean writeEnabled;
        private boolean batchWorkerEnabled;
        private boolean batchManagerEnabled;

        public boolean isReadOnlyMode() {
            return readEnabled && !writeEnabled && !batchWorkerEnabled && !batchManagerEnabled;
        }
    }

    @Getter
    @Setter
    public static class FineractCorrelationProperties {

        private boolean enabled;
        private String headerName;
    }

    @Getter
    @Setter
    public static class FineractContentProperties {

        private boolean regexWhitelistEnabled;
        private List<String> regexWhitelist;
        private boolean mimeWhitelistEnabled;
        private List<String> mimeWhitelist;
        private FineractContentFilesystemProperties filesystem;
        private FineractContentS3Properties s3;
    }

    @Getter
    @Setter
    public static class FineractContentFilesystemProperties {

        private String rootFolder;
    }

    @Getter
    @Setter
    public static class FineractContentS3Properties {

        private String bucketName;
        private String accessKey;
        private String secretKey;
        private String region;
    }

    @Getter
    @Setter
    public static class FineractTemplateProperties {

        private boolean regexWhitelistEnabled;
        private List<String> regexWhitelist;
    }

    @Getter
    @Setter
    public static class FineractSupportedProperties {

        private List<String> urls;
    }

    @Getter
    @Setter
    public static class FineractCorsProperties {

        /**
         * Enable/disable CORS support
         */
        private boolean enabled = true;

        /**
         * List of allowed origins (exact match)
         */
        private List<String> allowedOrigins = new ArrayList<>();

        /**
         * List of allowed origin patterns (regex)
         */
        private List<String> allowedOriginPatterns = new ArrayList<>();

        /**
         * HTTP methods to allow
         */
        private List<String> allowedMethods = new ArrayList<>();

        /**
         * Headers that clients are allowed to send
         */
        private List<String> allowedHeaders = new ArrayList<>();

        /**
         * Headers exposed to the client
         */
        private List<String> exposedHeaders = new ArrayList<>();

        /**
         * Whether to allow credentials (cookies, auth headers)
         */
        private boolean allowCredentials = true;

        /**
         * Preflight cache duration in seconds
         */
        private long maxAge = 3600;

        /**
         * Enable audit logging for CORS rejections
         */
        private boolean auditLoggingEnabled = true;

        /**
         * Check if origin is allowed
         */
        public boolean isOriginAllowed(String origin) {
            if (origin == null || origin.isEmpty()) {
                return false;
            }

            // Check exact match
            if (allowedOrigins != null && allowedOrigins.contains(origin)) {
                return true;
            }

            // Check pattern match
            if (allowedOriginPatterns != null) {
                for (String pattern : allowedOriginPatterns) {
                    try {
                        if (origin.matches(pattern)) {
                            return true;
                        }
                    } catch (Exception e) {
                        // Invalid regex pattern, skip
                    }
                }
            }

            return false;
        }
    }
}
