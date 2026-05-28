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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for ResponseCorsFilter to verify CORS whitelist implementation.
 * Tests for VAPT-CORS-001 security fix.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResponseCorsFilterTest {

    @Mock
    private FineractProperties fineractProperties;

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private ContainerResponseContext responseContext;

    @Mock
    private UriInfo uriInfo;

    @InjectMocks
    private ResponseCorsFilter corsFilter;

    private FineractProperties.FineractCorsProperties corsConfig;
    private MultivaluedMap<String, Object> responseHeaders;

    @BeforeEach
    void setUp() {
        corsConfig = new FineractProperties.FineractCorsProperties();
        corsConfig.setEnabled(true);
        corsConfig.setAllowedOrigins(Arrays.asList("https://allowed-site.com", "https://another-allowed.com"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        corsConfig.setExposedHeaders(Arrays.asList("X-Custom-Header"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600);
        corsConfig.setAuditLoggingEnabled(true);

        responseHeaders = new MultivaluedHashMap<>();
        lenient().when(fineractProperties.getCors()).thenReturn(corsConfig);
        lenient().when(responseContext.getHeaders()).thenReturn(responseHeaders);
        lenient().when(requestContext.getUriInfo()).thenReturn(uriInfo);
        lenient().when(uriInfo.getPath()).thenReturn("/api/v1/test");
    }

    @Test
    void testAllowedOriginGetsCorsHeaders() {
        when(requestContext.getHeaderString("Origin")).thenReturn("https://allowed-site.com");
        when(requestContext.getMethod()).thenReturn("GET");

        corsFilter.filter(requestContext, responseContext);

        assertTrue(responseHeaders.containsKey("Access-Control-Allow-Origin"));
        assertEquals("https://allowed-site.com", responseHeaders.getFirst("Access-Control-Allow-Origin"));
        assertEquals("true", responseHeaders.getFirst("Access-Control-Allow-Credentials"));
        assertNotNull(responseHeaders.getFirst("Access-Control-Allow-Methods"));
        assertNotNull(responseHeaders.getFirst("Access-Control-Allow-Headers"));
    }

    @Test
    void testRejectedOriginGetsNoCorsHeaders() {
        when(requestContext.getHeaderString("Origin")).thenReturn("https://malicious-site.com");
        when(requestContext.getMethod()).thenReturn("GET");

        corsFilter.filter(requestContext, responseContext);

        assertFalse(responseHeaders.containsKey("Access-Control-Allow-Origin"));
        assertFalse(responseHeaders.containsKey("Access-Control-Allow-Credentials"));
    }

    @Test
    void testNoOriginHeaderNoCorsHeaders() {
        when(requestContext.getHeaderString("Origin")).thenReturn(null);

        corsFilter.filter(requestContext, responseContext);

        assertTrue(responseHeaders.isEmpty());
    }

    @Test
    void testEmptyOriginHeaderNoCorsHeaders() {
        when(requestContext.getHeaderString("Origin")).thenReturn("");

        corsFilter.filter(requestContext, responseContext);

        assertTrue(responseHeaders.isEmpty());
    }

    @Test
    void testCorsDisabledNoCorsHeaders() {
        corsConfig.setEnabled(false);
        when(requestContext.getHeaderString("Origin")).thenReturn("https://allowed-site.com");

        corsFilter.filter(requestContext, responseContext);

        assertTrue(responseHeaders.isEmpty());
    }

    @Test
    void testNullCorsConfigNoCorsHeaders() {
        when(fineractProperties.getCors()).thenReturn(null);
        when(requestContext.getHeaderString("Origin")).thenReturn("https://allowed-site.com");

        corsFilter.filter(requestContext, responseContext);

        assertTrue(responseHeaders.isEmpty());
    }

    @Test
    void testPatternMatchingWorks() {
        corsConfig.setAllowedOriginPatterns(Arrays.asList("https://.*\\.theoxygen\\.com"));
        when(requestContext.getHeaderString("Origin")).thenReturn("https://app.theoxygen.com");
        when(requestContext.getMethod()).thenReturn("GET");

        corsFilter.filter(requestContext, responseContext);

        assertTrue(responseHeaders.containsKey("Access-Control-Allow-Origin"));
        assertEquals("https://app.theoxygen.com", responseHeaders.getFirst("Access-Control-Allow-Origin"));
    }

    @Test
    void testSubdomainPatternMatchingWorks() {
        corsConfig.setAllowedOriginPatterns(Arrays.asList("https://.*\\.theoxygen\\.com"));
        when(requestContext.getHeaderString("Origin")).thenReturn("https://admin.theoxygen.com");
        when(requestContext.getMethod()).thenReturn("POST");

        corsFilter.filter(requestContext, responseContext);

        assertTrue(responseHeaders.containsKey("Access-Control-Allow-Origin"));
        assertEquals("https://admin.theoxygen.com", responseHeaders.getFirst("Access-Control-Allow-Origin"));
    }

    @Test
    void testWildcardNotAllowed() {
        // Verify that wildcard is not automatically allowed
        when(requestContext.getHeaderString("Origin")).thenReturn("*");
        when(requestContext.getMethod()).thenReturn("GET");

        corsFilter.filter(requestContext, responseContext);

        assertFalse(responseHeaders.containsKey("Access-Control-Allow-Origin"));
    }

    @Test
    void testCredentialsOnlyWithSpecificOrigin() {
        when(requestContext.getHeaderString("Origin")).thenReturn("https://allowed-site.com");
        when(requestContext.getMethod()).thenReturn("GET");
        corsConfig.setAllowCredentials(true);

        corsFilter.filter(requestContext, responseContext);

        assertEquals("https://allowed-site.com", responseHeaders.getFirst("Access-Control-Allow-Origin"));
        assertEquals("true", responseHeaders.getFirst("Access-Control-Allow-Credentials"));
        // Verify it's NOT wildcard
        assertNotEquals("*", responseHeaders.getFirst("Access-Control-Allow-Origin"));
    }

    @Test
    void testMaxAgeHeader() {
        when(requestContext.getHeaderString("Origin")).thenReturn("https://allowed-site.com");
        when(requestContext.getMethod()).thenReturn("OPTIONS");
        corsConfig.setMaxAge(7200);

        corsFilter.filter(requestContext, responseContext);

        assertEquals("7200", responseHeaders.getFirst("Access-Control-Max-Age"));
    }

    @Test
    void testExposedHeaders() {
        when(requestContext.getHeaderString("Origin")).thenReturn("https://allowed-site.com");
        when(requestContext.getMethod()).thenReturn("GET");

        corsFilter.filter(requestContext, responseContext);

        String exposedHeaders = (String) responseHeaders.getFirst("Access-Control-Expose-Headers");
        assertNotNull(exposedHeaders);
        assertTrue(exposedHeaders.contains("X-Custom-Header"));
    }
}

