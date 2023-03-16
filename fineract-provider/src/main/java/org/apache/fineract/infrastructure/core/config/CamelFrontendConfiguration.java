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

import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.atmosphere.websocket.CamelWebSocketServlet;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.fineract.commands.provider.CommandHandlerProvider;
import org.apache.fineract.commands.service.CamelCommandProcessingService;
import org.apache.fineract.commands.service.CommandProcessingService;
import org.apache.fineract.commands.service.CommandSourceService;
import org.apache.fineract.commands.service.IdempotencyKeyGenerator;
import org.apache.fineract.commands.service.IdempotencyKeyResolver;
import org.apache.fineract.commands.service.SynchronousCommandProcessingService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.domain.FineractRequestContextHolder;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.transactions.service.TransactionStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty("fineract.camel.frontend.enabled")
public class CamelFrontendConfiguration {

    Logger log = LoggerFactory.getLogger(CamelFrontendConfiguration.class);

    @Value("${camel.component.websocket.ssl-keystore:}")
    private String keystore;

    @Value("${camel.component.websocket.ssl-password:}")
    private String keystorePassword;

    @Value("${camel.component.websocket.ssl-key-password:}")
    private String keyPassword;

    private final TransactionStatusService transactionStatusService;

    @Autowired
    public CamelFrontendConfiguration(TransactionStatusService transactionStatusService) {
        this.transactionStatusService = transactionStatusService;
    }

    @Bean
    public CommandProcessingService camelCommandProcessingService(final ProducerTemplate producerTemplate,
            final PlatformSecurityContext securityContext) {
        return new CamelCommandProcessingService(producerTemplate, securityContext, transactionStatusService);
    }

    @Bean
    @Primary
    public CommandProcessingService synchronousCommandProcessingService(final PlatformSecurityContext context,
            final ApplicationContext applicationContext, final ToApiJsonSerializer<Map<String, Object>> toApiJsonSerializer,
            final ToApiJsonSerializer<CommandProcessingResult> toApiResultJsonSerializer,
            final ConfigurationDomainService configurationDomainService, final CommandHandlerProvider commandHandlerProvider,
            final IdempotencyKeyResolver idempotencyKeyResolver, final IdempotencyKeyGenerator idempotencyKeyGenerator,
            final CommandSourceService commandSourceService, final FineractRequestContextHolder fineractRequestContextHolder) {
        return new SynchronousCommandProcessingService(context, applicationContext, toApiJsonSerializer, toApiResultJsonSerializer,
                configurationDomainService, commandHandlerProvider, idempotencyKeyResolver, idempotencyKeyGenerator, commandSourceService,
                fineractRequestContextHolder);
    }

    @Bean
    public SSLContextParameters sslContextParameters() {
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(keystore);
        ksp.setPassword(keystorePassword);

        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyStore(ksp);
        kmp.setKeyPassword(keyPassword);

        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(ksp);

        SSLContextParameters scp = new SSLContextParameters();
        scp.setKeyManagers(kmp);
        scp.setTrustManagers(tmp);

        return scp;
    }

    @Bean
    public ServletRegistrationBean<CamelWebSocketServlet> servletRegistrationBean() {
        ServletRegistrationBean<CamelWebSocketServlet> bean = new ServletRegistrationBean<>(new CamelWebSocketServlet(), "/camel/*");
        bean.setName("CamelWsServlet");
        bean.setLoadOnStartup(-1);
        bean.setInitParameters(Map.<String, String>of("events", "true"));
        return bean;
    }

}
