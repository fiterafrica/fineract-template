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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty("fineract.camel.backend.enabled")
public class CamelBackendConfiguration {

    private final TransactionStatusService transactionStatusService;

    @Autowired
    public CamelBackendConfiguration(TransactionStatusService transactionStatusService) {
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
}
