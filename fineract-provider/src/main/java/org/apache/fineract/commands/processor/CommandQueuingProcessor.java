/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.fineract.commands.processor;

import com.google.gson.JsonElement;
import com.jano7.executor.BoundedStrategy;
import com.jano7.executor.KeyRunnable;
import com.jano7.executor.KeySequentialBoundedExecutor;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import org.apache.camel.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.fineract.commands.CommandConstants;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.route.CamelEndpoints;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * This a proof of concept implementation of using the disruptor to process savings account transactions.
 */
@Component
@ConditionalOnProperty("fineract.camel.backend.enabled")
public class CommandQueuingProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CommandQueuingProcessor.class);

    private final TenantDetailsService tenantDetailsService;

    private final CommandProcessingService processingService;

    private final FromJsonHelper fromApiJsonHelper;

    private final AppUserRepository appUserRepository;

    private final ProducerTemplate producerTemplate;

    private final TransactionStatusService transactionStatusService;

    @Value("${fineract.camel.backend.thread.pool.size}")
    private Integer threadPoolSize;

    @Value("${fineract.camel.backend.thread.pool.queue.size}")
    private Integer threadPoolQueueSize;

    private ExecutorService underlyingExecutor;
    private KeySequentialBoundedExecutor keyExecutor;

    @Autowired
    public CommandQueuingProcessor(TenantDetailsService tenantDetailsService,
            @Qualifier("synchronousCommandProcessingService") CommandProcessingService processingService, FromJsonHelper fromApiJsonHelper,
            AppUserRepository appUserRepository, ProducerTemplate producerTemplate, TransactionStatusService transactionStatusService) {
        this.tenantDetailsService = tenantDetailsService;
        this.processingService = processingService;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.appUserRepository = appUserRepository;
        this.producerTemplate = producerTemplate;
        this.transactionStatusService = transactionStatusService;
    }

    public void process(@Header(CommandConstants.FINERACT_HEADER_AUTH_TOKEN) String authToken,
            @Header(CommandConstants.FINERACT_HEADER_RUN_AS) String username,
            @Header(CommandConstants.FINERACT_HEADER_TENANT_ID) String tenantId,
            @Header(CommandConstants.FINERACT_HEADER_APPROVED_BY_CHECKER) Boolean approvedByChecker, @Headers Map<String, Object> headers,
            @Body CommandWrapper commandWrapper) {

        // TODO: this looks awefully redundant... but let's keep it that way for now
        final String json = commandWrapper.getJson();
        final JsonElement parsedCommand = this.fromApiJsonHelper.parse(commandWrapper.getJson());
        JsonCommand command = JsonCommand.from(json, parsedCommand, this.fromApiJsonHelper, commandWrapper.getEntityName(),
                commandWrapper.getEntityId(), commandWrapper.getSubentityId(), commandWrapper.getGroupId(), commandWrapper.getClientId(),
                commandWrapper.getLoanId(), commandWrapper.getSavingsId(), commandWrapper.getTransactionId(), commandWrapper.getHref(),
                commandWrapper.getProductId(), commandWrapper.getCreditBureauId(), commandWrapper.getOrganisationCreditBureauId());

        executeCommand(tenantId, authToken, username, approvedByChecker, commandWrapper, command, headers);
    }

    private void executeCommand(String tenantId, String authToken, String username, Boolean approvedByChecker,
            CommandWrapper commandWrapper, JsonCommand command, Map<String, Object> headers) {

        String savingsId = generateKey(commandWrapper);

        KeyRunnable<String> runnable = new KeyRunnable<>(savingsId, () -> {

            final FineractPlatformTenant tenant = this.tenantDetailsService.loadTenantById(tenantId);
            ThreadLocalContextUtil.setTenant(tenant);

            ThreadLocalContextUtil.setAuthToken(authToken);

            final AppUser user = appUserRepository.findAppUserByName(username);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword()));

            String correlationId = headers.get(CommandConstants.FINERACT_HEADER_CORRELATION_ID) + "";

            try {
                CommandProcessingResult result = processingService.processAndLogCommand(commandWrapper, command, approvedByChecker);
                this.transactionStatusService.endProcessing(correlationId);
                result.setCorrelationId(correlationId);

                producerTemplate.sendBodyAndHeaders(CamelEndpoints.RMQ_FINERACT_RESULT, ExchangePattern.InOnly, result, headers);
            } catch (Throwable t) {
                LOG.error("Error processing command {}", command, t);
                headers.put(CommandConstants.FINERACT_HEADER_ERROR_MESSAGE, t.getMessage());

                Map<String, String> fineractHeaderErrorMessage = Map.of("errorMessage", t.getMessage(), "correlationId", correlationId,
                        "status", "FAILED");

                producerTemplate.sendBodyAndHeaders(CamelEndpoints.RMQ_FINERACT_ERROR, ExchangePattern.InOnly, fineractHeaderErrorMessage,
                        headers);

            }

        });

        keyExecutor.execute(runnable);
    }

    private String generateKey(CommandWrapper cmd) {
        String entityName = cmd.entityName();
        Long entityId = ObjectUtils.firstNonNull(cmd.getSavingsId(), cmd.getLoanId(), cmd.getClientId(), cmd.getGroupId(),
                cmd.getEntityId());
        return entityName + "-" + entityId;
    }

    @PostConstruct
    public void init() {
        LOG.info("ThreadPool Size: {}", this.threadPoolSize);
        LOG.info("ThreadPool Queue Size: {}", this.threadPoolQueueSize);

        var td = new ThreadFactory() {

            private final AtomicInteger threadNumber = new AtomicInteger(0);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread t = new Thread(r);
                t.setName("be-thread-" + threadNumber.incrementAndGet());
                t.setDaemon(false);
                return t;
            }
        };

        this.underlyingExecutor = Executors.newFixedThreadPool(this.threadPoolSize, td);
        this.keyExecutor = new KeySequentialBoundedExecutor(threadPoolQueueSize, BoundedStrategy.BLOCK, underlyingExecutor);
    }
}
