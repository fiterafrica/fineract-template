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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public final class ProcessorHelper {

    // Nota bene: Similar code to insecure HTTPS is also in Fineract Client's
    // org.apache.fineract.client.util.FineractClient.Builder.insecure()

    private final FineractProperties fineractProperties;
    private final SupportedUrlService supportedUrlService;

    private static final Logger LOG = LoggerFactory.getLogger(ProcessorHelper.class);

    @SuppressWarnings("unused")
    private static final X509TrustManager insecureX509TrustManager = new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}// NOSONAR

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}// NOSONAR

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }
    };

    /**
     * Configure HTTP client to be "insecure", as in skipping host SSL certificate verification. While this can be
     * useful during development e.g. when using self-signed certificates, it should never be enabled in production (due
     * to "man in the middle").
     */
    private final boolean insecureHttpClient = Boolean.getBoolean("fineract.insecureHttpClient");
    private final SSLContext insecureSSLContext;

    @Autowired
    public ProcessorHelper(FineractProperties fineractProperties, SupportedUrlService supportedUrlService)
            throws KeyManagementException, NoSuchAlgorithmException {
        this.fineractProperties = fineractProperties;
        this.supportedUrlService = supportedUrlService;
        if (insecureHttpClient) {
            insecureSSLContext = createInsecureSSLContext();
        } else {
            insecureSSLContext = null;
        }
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
        okBuilder.sslSocketFactory(insecureSSLContext.getSocketFactory(), insecureX509TrustManager);
        HostnameVerifier insecureHostnameVerifier = (hostname, session) -> true;// NOSONAR
        okBuilder.hostnameVerifier(insecureHostnameVerifier);
    }

    private SSLContext createInsecureSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext insecureSSLContext = SSLContext.getInstance("TLS"); // TODO "TLS" or "SSL" as in
        // FineractClient.Builder?
        insecureSSLContext.init(null, new TrustManager[] { insecureX509TrustManager }, new SecureRandom());
        return insecureSSLContext;
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
