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

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import org.apache.fineract.infrastructure.configuration.exception.ExternalServiceForbiddenException;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExternalServiceHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalServiceHelper.class);

    private ExternalServiceHelper() {
        // Utility class
    }

    /**
     * Validates if the given URL is allowed for external service calls using the SupportedUrlService. This method
     * supports both configuration-based and programmatically added URLs.
     *
     * @param fineractProperties
     *            the application properties containing supported URLs configuration
     * @param supportedUrlService
     *            the service for managing supported URLs (can include dynamically added URLs)
     * @param url
     *            the URL to validate
     * @throws ExternalServiceForbiddenException
     *             if the URL is not in the allowlist or points to an internal address
     */
    public static void validateUrl(FineractProperties fineractProperties, SupportedUrlService supportedUrlService, String url) {
        if (url == null || url.isBlank()) {
            LOG.warn("Empty or null URL provided for external service");
            throw new ExternalServiceForbiddenException("URL cannot be empty");
        }

        // Validate URL format and scheme
        validateUrlFormat(url);

        // SSRF Protection: Check if URL resolves to internal/private IP address
        boolean allowInternalAddresses = fineractProperties.getSupported() != null
                && fineractProperties.getSupported().isAllowInternalAddresses();
        if (!allowInternalAddresses) {
            validateNotInternalAddress(url);
        } else {
            LOG.warn("SECURITY WARNING: Internal addresses are allowed. This should only be used in development/testing environments.");
        }

        // Check against allowlist using the service (includes both config and dynamic URLs)
        if (!supportedUrlService.isUrlSupported(url)) {
            LOG.warn("URL not in allowlist: {}", url);
            throw new ExternalServiceForbiddenException(url);
        }
    }

    /**
     * Validates if the given URL is allowed for external service calls. This method provides SSRF (Server-Side Request
     * Forgery) protection by:
     * <ul>
     * <li>Checking against a configured allowlist of URLs</li>
     * <li>Blocking requests to internal/private IP addresses</li>
     * <li>Validating URL scheme (only http/https allowed)</li>
     * </ul>
     *
     * @param fineractProperties
     *            the application properties containing supported URLs configuration
     * @param url
     *            the URL to validate
     * @throws ExternalServiceForbiddenException
     *             if the URL is not in the allowlist or points to an internal address
     */
    public static void validateUrl(FineractProperties fineractProperties, String url) {
        if (url == null || url.isBlank()) {
            LOG.warn("Empty or null URL provided for external service");
            throw new ExternalServiceForbiddenException("URL cannot be empty");
        }

        // Validate URL format and scheme
        validateUrlFormat(url);

        // SSRF Protection: Check if URL resolves to internal/private IP address
        // Skip this check only if explicitly allowed in configuration (for dev/test environments)
        boolean allowInternalAddresses = fineractProperties.getSupported() != null
                && fineractProperties.getSupported().isAllowInternalAddresses();
        if (!allowInternalAddresses) {
            validateNotInternalAddress(url);
        } else {
            LOG.warn("SECURITY WARNING: Internal addresses are allowed. This should only be used in development/testing environments.");
        }

        // Check against allowlist if configured
        if (fineractProperties.getSupported() != null) {
            if (fineractProperties.getSupported().getUrls() == null || !fineractProperties.getSupported().getUrls().contains(url)) {
                LOG.warn("URL not in allowlist: {}", url);
                throw new ExternalServiceForbiddenException(url);
            }
        } else {
            // If no allowlist is configured, reject all URLs for security
            LOG.warn("No URL allowlist configured, rejecting URL: {}", url);
            throw new ExternalServiceForbiddenException("No URL allowlist configured. Please configure supported URLs.");
        }
    }

    /**
     * Validates URL format and ensures only http/https schemes are allowed.
     */
    private static void validateUrlFormat(String url) {
        try {
            URL parsedUrl = new URL(url);
            String protocol = parsedUrl.getProtocol().toLowerCase();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                LOG.warn("Invalid URL scheme rejected: {}", protocol);
                throw new ExternalServiceForbiddenException("Only http and https URLs are allowed");
            }
        } catch (MalformedURLException e) {
            LOG.warn("Malformed URL rejected: {}", url);
            throw new ExternalServiceForbiddenException("Invalid URL format: " + url);
        }
    }

    /**
     * SSRF Protection: Validates that the URL does not resolve to an internal/private IP address. This prevents
     * attackers from using the server to make requests to internal services.
     */
    private static void validateNotInternalAddress(String url) {
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost();

            if (host == null || host.isBlank()) {
                throw new ExternalServiceForbiddenException("URL must have a valid host");
            }

            // Check for obvious localhost patterns before DNS resolution
            String lowerHost = host.toLowerCase();
            if (lowerHost.equals("localhost") || lowerHost.equals("127.0.0.1") || lowerHost.equals("::1") || lowerHost.equals("[::1]")
                    || lowerHost.startsWith("0.") || lowerHost.endsWith(".local")) {
                LOG.warn("Localhost URL rejected: {}", url);
                throw new ExternalServiceForbiddenException("Internal addresses are not allowed");
            }

            // Resolve hostname and check all IP addresses it resolves to
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isInternalAddress(address)) {
                    LOG.warn("URL resolves to internal address, rejected: {} -> {}", url, address.getHostAddress());
                    throw new ExternalServiceForbiddenException("Internal addresses are not allowed");
                }
            }
        } catch (MalformedURLException e) {
            throw new ExternalServiceForbiddenException("Invalid URL format: " + url);
        } catch (UnknownHostException e) {
            LOG.warn("Cannot resolve host for URL: {}", url);
            throw new ExternalServiceForbiddenException("Cannot resolve host: " + url);
        }
    }

    /**
     * Checks if an IP address is internal/private. This includes: - Loopback addresses (127.x.x.x, ::1) - Private
     * networks (10.x.x.x, 172.16-31.x.x, 192.168.x.x) - Link-local addresses (169.254.x.x, fe80::) - Site-local
     * addresses - Any local address (0.0.0.0)
     */
    private static boolean isInternalAddress(InetAddress address) {
        return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress()
                || address.isMulticastAddress() || isPrivateIPv4Range(address);
    }

    /**
     * Additional check for private IPv4 ranges that might not be covered by standard Java methods.
     */
    private static boolean isPrivateIPv4Range(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length != 4) {
            return false; // Not IPv4
        }

        int firstOctet = bytes[0] & 0xFF;
        int secondOctet = bytes[1] & 0xFF;

        // 10.0.0.0/8
        if (firstOctet == 10) {
            return true;
        }
        // 172.16.0.0/12
        if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) {
            return true;
        }
        // 192.168.0.0/16
        if (firstOctet == 192 && secondOctet == 168) {
            return true;
        }
        // 169.254.0.0/16 (link-local)
        if (firstOctet == 169 && secondOctet == 254) {
            return true;
        }
        // 127.0.0.0/8 (loopback)
        if (firstOctet == 127) {
            return true;
        }
        // 0.0.0.0/8
        if (firstOctet == 0) {
            return true;
        }

        return false;
    }
}
