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
package org.apache.fineract.infrastructure.hooks.processor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import org.apache.fineract.infrastructure.configuration.service.ExternalServiceHelper;
import org.apache.fineract.infrastructure.configuration.service.SupportedUrlService;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Service
public final class ProcessorHelper {

    // Nota bene: Similar code to insecure HTTPS is also in Fineract Client's
    // org.apache.fineract.client.util.FineractClient.Builder.insecure()

    private final FineractProperties fineractProperties;

    private static final Logger LOG = LoggerFactory.getLogger(ProcessorHelper.class);

    /**
     * Deprecated insecure HTTP client switch. Kept only for backward compatibility; TLS validation is always enforced.
     */
    private final boolean insecureHttpClient = Boolean.getBoolean("fineract.insecureHttpClient");

    private final SupportedUrlService supportedUrlService;

    @Autowired
    public ProcessorHelper(FineractProperties fineractProperties, SupportedUrlService supportedUrlService) {
        this.fineractProperties = fineractProperties;
        this.supportedUrlService = supportedUrlService;
    }

    private OkHttpClient createClient() {
        var okBuilder = new OkHttpClient.Builder();
        if (insecureHttpClient) {
            configureInsecureClient(okBuilder);
        }
        // SSRF Protection: Use custom DNS resolver to validate resolved IPs at connection time
        // This prevents DNS rebinding attacks where a hostname resolves to different IPs between validation and use
        boolean allowInternalAddresses = fineractProperties.getSupported() != null
                && fineractProperties.getSupported().isAllowInternalAddresses();
        okBuilder.dns(new SsrfSafeDns(allowInternalAddresses));
        return okBuilder.build();
    }

    /**
     * Custom DNS resolver that validates resolved IP addresses to prevent SSRF attacks. This provides defense-in-depth
     * against DNS rebinding attacks.
     */
    private static class SsrfSafeDns implements Dns {

        private final boolean allowInternalAddresses;

        SsrfSafeDns(boolean allowInternalAddresses) {
            this.allowInternalAddresses = allowInternalAddresses;
        }

        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            List<InetAddress> addresses = Arrays.asList(InetAddress.getAllByName(hostname));
            if (!allowInternalAddresses) {
                for (InetAddress address : addresses) {
                    if (isInternalAddress(address)) {
                        LOG.warn("DNS rebinding attack prevented: {} resolved to internal address {}", hostname, address.getHostAddress());
                        throw new UnknownHostException("Internal addresses are not allowed: " + hostname);
                    }
                }
            }
            return addresses;
        }

        private boolean isInternalAddress(InetAddress address) {
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress()
                    || address.isMulticastAddress()) {
                return true;
            }

            byte[] bytes = address.getAddress();
            if (bytes.length == 4) {
                int firstOctet = bytes[0] & 0xFF;
                int secondOctet = bytes[1] & 0xFF;
                // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 169.254.0.0/16, 127.0.0.0/8, 0.0.0.0/8
                if (firstOctet == 10 || firstOctet == 127 || firstOctet == 0
                        || (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) || (firstOctet == 192 && secondOctet == 168)
                        || (firstOctet == 169 && secondOctet == 254)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void configureInsecureClient(final OkHttpClient.Builder okBuilder) {
        LOG.warn("Ignoring `fineract.insecureHttpClient=true`: insecure TLS settings are disabled; using default secure TLS validation.");
    }

    @SuppressWarnings("rawtypes")
    public Callback createCallback(final String url) {
        return new Callback() {

            @Override
            public void onResponse(@SuppressWarnings("unused") Call call, retrofit2.Response response) {
                LOG.info("URL: {} - Status: {}", url, response.code());
            }

            @Override
            public void onFailure(@SuppressWarnings("unused") Call call, Throwable t) {
                LOG.error("URL: {} - Retrofit failure occured", url, t);
            }
        };
    }

    public WebHookService createWebHookService(final String url) {
        final OkHttpClient client = createClient();
        ExternalServiceHelper.validateUrl(fineractProperties, supportedUrlService, url);

        final Retrofit retrofit = new Retrofit.Builder().baseUrl(url).client(client).addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(WebHookService.class);
    }

    @SuppressWarnings("rawtypes")
    public Callback createCallback(final String url, String payload) {

        return new Callback() {

            @Override
            public void onResponse(@SuppressWarnings("unused") Call call, retrofit2.Response response) {
                LOG.info("URL: {} - Status: {}", url, response.code());
            }

            @Override
            public void onFailure(@SuppressWarnings("unused") Call call, Throwable t) {
                LOG.error("URL: {} - Retrofit failure occured", url, t);
            }
        };
    }
}
