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
package org.apache.fineract.commands.processor;

import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_APPROVED_BY_CHECKER;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_AUTH_TOKEN;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_CORRELATION_ID;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_RUN_AS;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_TENANT_ID;

import com.google.gson.JsonElement;
import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandProcessingService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.service.TenantDetailsService;
import org.apache.fineract.infrastructure.transactions.service.TransactionStatusService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CommandProcessAndLogSyncProcessor {

    private final TenantDetailsService tenantDetailsService;

    private final CommandProcessingService processingService;

    private final FromJsonHelper fromApiJsonHelper;

    private final AppUserRepository appUserRepository;

    private final TransactionStatusService transactionStatusService;

    @Autowired
    public CommandProcessAndLogSyncProcessor(TenantDetailsService tenantDetailsService,
            @Qualifier("synchronousCommandProcessingService") CommandProcessingService processingService, FromJsonHelper fromApiJsonHelper,
            AppUserRepository appUserRepository, TransactionStatusService transactionStatusService) {
        this.tenantDetailsService = tenantDetailsService;
        this.processingService = processingService;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.appUserRepository = appUserRepository;
        this.transactionStatusService = transactionStatusService;
    }

    public CommandProcessingResult process(@Header(FINERACT_HEADER_CORRELATION_ID) String correlationId,
            @Header(FINERACT_HEADER_AUTH_TOKEN) String authToken, @Header(FINERACT_HEADER_RUN_AS) String username,
            @Header(FINERACT_HEADER_TENANT_ID) String tenantId, @Header(FINERACT_HEADER_APPROVED_BY_CHECKER) Boolean approvedByChecker,
            @Body CommandWrapper commandWrapper) {

        final FineractPlatformTenant tenant = this.tenantDetailsService.loadTenantById(tenantId);
        ThreadLocalContextUtil.setTenant(tenant);

        ThreadLocalContextUtil.setAuthToken(authToken);

        final AppUser user = appUserRepository.findAppUserByName(username);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword()));

        // TODO: this looks awfully redundant... but let's keep it that way for now
        final String json = commandWrapper.getJson();
        final JsonElement parsedCommand = this.fromApiJsonHelper.parse(commandWrapper.getJson());
        JsonCommand command = JsonCommand.from(json, parsedCommand, this.fromApiJsonHelper, commandWrapper.getEntityName(),
                commandWrapper.getEntityId(), commandWrapper.getSubentityId(), commandWrapper.getGroupId(), commandWrapper.getClientId(),
                commandWrapper.getLoanId(), commandWrapper.getSavingsId(), commandWrapper.getTransactionId(), commandWrapper.getHref(),
                commandWrapper.getProductId(), commandWrapper.getCreditBureauId(), commandWrapper.getOrganisationCreditBureauId(),
                commandWrapper.getJobName());

        CommandProcessingResult result = processingService.executeCommand(commandWrapper, command, approvedByChecker);
        result.setCorrelationId(correlationId);
        transactionStatusService.endProcessing(correlationId);
        return result;
    }
}
