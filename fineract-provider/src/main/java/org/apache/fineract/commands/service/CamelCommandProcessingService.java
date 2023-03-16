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
package org.apache.fineract.commands.service;

import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_APPROVED_BY_CHECKER;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_AUTH_TOKEN;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_CORRELATION_ID;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_RUN_AS;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_TENANT_ID;

import java.util.Map;
import java.util.UUID;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.rabbitmq.RabbitMQConstants;
import org.apache.fineract.commands.domain.CommandSource;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.route.CamelEndpoints;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.transactions.service.TransactionStatusService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class CamelCommandProcessingService implements CommandProcessingService {

    // TODO: would be better to put everything in a mapped properties class/object
    @Value("${fineract.camel.processing.timeout}")
    private Long timeout;

    private final ProducerTemplate producerTemplate;
    private final PlatformSecurityContext securityContext;
    private final TransactionStatusService transactionStatusService;

    @Autowired
    public CamelCommandProcessingService(final ProducerTemplate producerTemplate, final PlatformSecurityContext securityContext,
            TransactionStatusService transactionStatusService) {
        this.producerTemplate = producerTemplate;
        this.securityContext = securityContext;
        this.transactionStatusService = transactionStatusService;
    }

    @Override
    public CommandProcessingResult executeCommand(CommandWrapper wrapper, JsonCommand command, boolean isApprovedByChecker) {
        final String tenantIdentifier = ThreadLocalContextUtil.getTenant().getTenantIdentifier();
        final AppUser appUser = this.securityContext
                .authenticatedUser(CommandWrapper.wrap(wrapper.actionName(), wrapper.entityName(), null, null));
        final String authToken = ThreadLocalContextUtil.getAuthToken();
        final String correlationId = UUID.randomUUID().toString();

        producerTemplate.sendBodyAndHeaders(CamelEndpoints.SEDA_FINERACT_PROCESS_AND_LOG, ExchangePattern.InOnly, wrapper,
                Map.of(FINERACT_HEADER_CORRELATION_ID, correlationId, FINERACT_HEADER_AUTH_TOKEN, authToken, FINERACT_HEADER_RUN_AS,
                        appUser.getUsername(), FINERACT_HEADER_TENANT_ID, tenantIdentifier, FINERACT_HEADER_APPROVED_BY_CHECKER,
                        isApprovedByChecker, RabbitMQConstants.DELIVERY_MODE, 1));
        transactionStatusService.logCorrelationId(correlationId);
        return CommandProcessingResult.correlationIdResult(correlationId);
    }

    @Override
    public CommandProcessingResult logCommand(CommandSource commandSource) {
        final String tenantIdentifier = ThreadLocalContextUtil.getTenant().getTenantIdentifier();
        final AppUser appUser = this.securityContext.authenticatedUser();
        final String authToken = ThreadLocalContextUtil.getAuthToken();
        final String correlationId = UUID.randomUUID().toString();

        producerTemplate.sendBodyAndHeaders(CamelEndpoints.SEDA_FINERACT_LOG, ExchangePattern.InOnly, commandSource,
                Map.of(FINERACT_HEADER_CORRELATION_ID, correlationId, FINERACT_HEADER_AUTH_TOKEN, authToken, FINERACT_HEADER_RUN_AS,
                        appUser.getUsername(), FINERACT_HEADER_TENANT_ID, tenantIdentifier));
        transactionStatusService.logCorrelationId(correlationId);
        return CommandProcessingResult.correlationIdResult(correlationId);
    }

    @Override
    public boolean validateCommand(CommandWrapper commandWrapper, AppUser user) {
        // NOTE: let's defer the decision if execution is allowed or not to the backend; might generate a bit more
        // traffic for RabbitMQ, but this kind of situation should be actually rare
        // TODO: In case you create an active client this would prevent the client from being created
        return true;
        /*
         * if (commandAsyncFilter.filterProcessAndLog(commandWrapper)) { return true; } else { boolean
         * rollbackTransaction =
         * this.configurationDomainService.isMakerCheckerEnabledForTask(commandWrapper.taskPermissionName());
         * user.validateHasPermissionTo(commandWrapper.getTaskPermissionName()); return rollbackTransaction; }
         */
    }
}
