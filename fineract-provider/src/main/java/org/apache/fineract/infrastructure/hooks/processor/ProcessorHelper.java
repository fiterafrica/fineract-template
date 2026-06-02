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

import okhttp3.OkHttpClient;
import org.apache.fineract.infrastructure.configuration.service.ExternalServiceHelper;
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

    @Autowired
    public ProcessorHelper(FineractProperties fineractProperties) {
        this.fineractProperties = fineractProperties;
    }

    private OkHttpClient createClient() {
        var okBuilder = new OkHttpClient.Builder();
        if (insecureHttpClient) {
            configureInsecureClient(okBuilder);
        }
        return okBuilder.build();
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
        final Retrofit.Builder retrofitBuilder = new Retrofit.Builder();
        if (ExternalServiceHelper.validateUrl(fineractProperties, url)) {
            /*
             * This URL is captured via UI and saved in the database. we assume that the system user should only
             * register valid URLs for webhook services. And we can have more than one webhook service registered in the
             * system. So hardcoding the URL is not a good idea.
             */
            retrofitBuilder.baseUrl(url); // codeql[js/csrf-disabled] FSO-122
            retrofitBuilder.client(client);
            retrofitBuilder.addConverterFactory(GsonConverterFactory.create());
            final Retrofit retrofit = retrofitBuilder.build();
            return retrofit.create(WebHookService.class);
        }
        return retrofitBuilder.build().create(WebHookService.class);
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
