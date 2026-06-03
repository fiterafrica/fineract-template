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
package org.apache.fineract.infrastructure.configuration.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.fineract.infrastructure.configuration.exception.ExternalServiceForbiddenException;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.core.config.FineractProperties.FineractSupportedProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ExternalServiceHelper} SSRF protection.
 */
class ExternalServiceHelperTest {

    private FineractProperties fineractProperties;
    private FineractSupportedProperties supportedProperties;

    @BeforeEach
    void setUp() {
        fineractProperties = mock(FineractProperties.class);
        supportedProperties = mock(FineractSupportedProperties.class);
        when(fineractProperties.getSupported()).thenReturn(supportedProperties);
    }

    @Nested
    @DisplayName("URL Validation Tests")
    class UrlValidationTests {

        @Test
        @DisplayName("Should reject null URL")
        void shouldRejectNullUrl() {
            assertThrows(ExternalServiceForbiddenException.class, () -> ExternalServiceHelper.validateUrl(fineractProperties, null));
        }

        @Test
        @DisplayName("Should reject empty URL")
        void shouldRejectEmptyUrl() {
            assertThrows(ExternalServiceForbiddenException.class, () -> ExternalServiceHelper.validateUrl(fineractProperties, ""));
        }

        @Test
        @DisplayName("Should reject blank URL")
        void shouldRejectBlankUrl() {
            assertThrows(ExternalServiceForbiddenException.class, () -> ExternalServiceHelper.validateUrl(fineractProperties, "   "));
        }

        @Test
        @DisplayName("Should reject malformed URL")
        void shouldRejectMalformedUrl() {
            when(supportedProperties.getUrls()).thenReturn(List.of("not-a-valid-url"));
            assertThrows(ExternalServiceForbiddenException.class,
                    () -> ExternalServiceHelper.validateUrl(fineractProperties, "not-a-valid-url"));
        }
    }

    @Nested
    @DisplayName("URL Scheme Validation Tests")
    class UrlSchemeTests {

        @Test
        @DisplayName("Should reject file:// scheme")
        void shouldRejectFileScheme() {
            when(supportedProperties.getUrls()).thenReturn(List.of("file:///etc/passwd"));
            assertThrows(ExternalServiceForbiddenException.class,
                    () -> ExternalServiceHelper.validateUrl(fineractProperties, "file:///etc/passwd"));
        }

        @Test
        @DisplayName("Should reject ftp:// scheme")
        void shouldRejectFtpScheme() {
            when(supportedProperties.getUrls()).thenReturn(List.of("ftp://example.com/file"));
            assertThrows(ExternalServiceForbiddenException.class,
                    () -> ExternalServiceHelper.validateUrl(fineractProperties, "ftp://example.com/file"));
        }

        @Test
        @DisplayName("Should reject javascript: scheme")
        void shouldRejectJavascriptScheme() {
            when(supportedProperties.getUrls()).thenReturn(List.of("javascript:alert(1)"));
            assertThrows(ExternalServiceForbiddenException.class,
                    () -> ExternalServiceHelper.validateUrl(fineractProperties, "javascript:alert(1)"));
        }
    }

    @Nested
    @DisplayName("SSRF Protection - Internal Address Blocking Tests")
    class SsrfProtectionTests {

        @Test
        @DisplayName("Should reject localhost")
        void shouldRejectLocalhost() {
            when(supportedProperties.getUrls()).thenReturn(List.of("http://localhost:8080/api"));
            when(supportedProperties.isAllowInternalAddresses()).thenReturn(false);

            assertThrows(ExternalServiceForbiddenException.class,
                    () -> ExternalServiceHelper.validateUrl(fineractProperties, "http://localhost:8080/api"));
        }

        @Test
        @DisplayName("Should reject 127.0.0.1")
        void shouldRejectLoopbackIp() {
            when(supportedProperties.getUrls()).thenReturn(List.of("http://127.0.0.1:8080/api"));
            when(supportedProperties.isAllowInternalAddresses()).thenReturn(false);

            assertThrows(ExternalServiceForbiddenException.class,
                    () -> ExternalServiceHelper.validateUrl(fineractProperties, "http://127.0.0.1:8080/api"));
        }

        @Test
        @DisplayName("Should reject IPv6 loopback ::1")
        void shouldRejectIpv6Loopback() {
            when(supportedProperties.getUrls()).thenReturn(List.of("http://[::1]:8080/api"));
            when(supportedProperties.isAllowInternalAddresses()).thenReturn(false);

            assertThrows(ExternalServiceForbiddenException.class,
                    () -> ExternalServiceHelper.validateUrl(fineractProperties, "http://[::1]:8080/api"));
        }

        @Test
        @DisplayName("Should reject .local domain")
        void shouldRejectLocalDomain() {
            when(supportedProperties.getUrls()).thenReturn(List.of("http://myserver.local/api"));
            when(supportedProperties.isAllowInternalAddresses()).thenReturn(false);

            assertThrows(ExternalServiceForbiddenException.class,
                    () -> ExternalServiceHelper.validateUrl(fineractProperties, "http://myserver.local/api"));
        }
    }

    @Nested
    @DisplayName("Allowlist Tests")
    class AllowlistTests {

        @Test
        @DisplayName("Should reject URL not in allowlist")
        void shouldRejectUrlNotInAllowlist() {
            when(supportedProperties.getUrls()).thenReturn(List.of("http://localhost:8080/allowed"));
            when(supportedProperties.isAllowInternalAddresses()).thenReturn(true);

            assertThrows(ExternalServiceForbiddenException.class,
                    () -> ExternalServiceHelper.validateUrl(fineractProperties, "http://localhost:8080/notallowed"));
        }

        @Test
        @DisplayName("Should reject when allowlist is null")
        void shouldRejectWhenAllowlistIsNull() {
            when(supportedProperties.getUrls()).thenReturn(null);
            when(supportedProperties.isAllowInternalAddresses()).thenReturn(true);

            assertThrows(ExternalServiceForbiddenException.class,
                    () -> ExternalServiceHelper.validateUrl(fineractProperties, "http://localhost:8080/api"));
        }

        @Test
        @DisplayName("Should reject when supported properties is null")
        void shouldRejectWhenSupportedPropertiesIsNull() {
            when(fineractProperties.getSupported()).thenReturn(null);

            assertThrows(ExternalServiceForbiddenException.class,
                    () -> ExternalServiceHelper.validateUrl(fineractProperties, "http://localhost:8080/api"));
        }

        @Test
        @DisplayName("Should accept URL in allowlist")
        void shouldAcceptUrlInAllowlist() {
            // Use localhost with allowInternalAddresses=true since external domains may not resolve in test env
            String allowedUrl = "http://localhost:8080/webhook";
            when(supportedProperties.getUrls()).thenReturn(List.of(allowedUrl));
            when(supportedProperties.isAllowInternalAddresses()).thenReturn(true);

            assertDoesNotThrow(() -> ExternalServiceHelper.validateUrl(fineractProperties, allowedUrl));
        }
    }

    @Nested
    @DisplayName("Development Mode - Allow Internal Addresses Tests")
    class DevModeTests {

        @Test
        @DisplayName("Should allow localhost when allowInternalAddresses is true")
        void shouldAllowLocalhostInDevMode() {
            String localhostUrl = "http://localhost:8080/api";
            when(supportedProperties.getUrls()).thenReturn(List.of(localhostUrl));
            when(supportedProperties.isAllowInternalAddresses()).thenReturn(true);

            assertDoesNotThrow(() -> ExternalServiceHelper.validateUrl(fineractProperties, localhostUrl));
        }

        @Test
        @DisplayName("Should allow 127.0.0.1 when allowInternalAddresses is true")
        void shouldAllowLoopbackInDevMode() {
            String loopbackUrl = "http://127.0.0.1:8080/api";
            when(supportedProperties.getUrls()).thenReturn(List.of(loopbackUrl));
            when(supportedProperties.isAllowInternalAddresses()).thenReturn(true);

            assertDoesNotThrow(() -> ExternalServiceHelper.validateUrl(fineractProperties, loopbackUrl));
        }

        @Test
        @DisplayName("Should still require URL in allowlist even when allowInternalAddresses is true")
        void shouldStillRequireAllowlistInDevMode() {
            when(supportedProperties.getUrls()).thenReturn(List.of("http://localhost:8080/api"));
            when(supportedProperties.isAllowInternalAddresses()).thenReturn(true);

            assertThrows(ExternalServiceForbiddenException.class,
                    () -> ExternalServiceHelper.validateUrl(fineractProperties, "http://localhost:9999/other"));
        }
    }
}
