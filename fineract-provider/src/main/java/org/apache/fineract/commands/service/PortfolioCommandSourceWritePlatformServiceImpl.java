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

import static org.apache.fineract.commands.CommandConstants.ASYNC_COMMAND_PREFIX;

import com.google.gson.JsonElement;
import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.domain.CommandSource;
import org.apache.fineract.commands.domain.CommandSourceRepository;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.exception.CommandNotAwaitingApprovalException;
import org.apache.fineract.commands.exception.CommandNotFoundException;
import org.apache.fineract.commands.exception.RollbackTransactionAsCommandIsNotApprovedByCheckerException;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.jobs.service.SchedulerJobRunnerReadService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PortfolioCommandSourceWritePlatformServiceImpl implements PortfolioCommandSourceWritePlatformService {

    private final PlatformSecurityContext context;
    private final CommandSourceRepository commandSourceRepository;
    private final FromJsonHelper fromApiJsonHelper;
    private final CommandProcessingService processAndLogCommandService;
    private final CommandProcessingService camelCommandProcessingService;
    private final SchedulerJobRunnerReadService schedulerJobRunnerReadService;

    @Value("${fineract.camel.frontend.enabled}")
    private boolean frontendEnabled;

    private static final Logger LOG = LoggerFactory.getLogger(PortfolioCommandSourceWritePlatformServiceImpl.class);
    private static final SecureRandom random = new SecureRandom();

    @Autowired
    public PortfolioCommandSourceWritePlatformServiceImpl(final PlatformSecurityContext context,
            final CommandSourceRepository commandSourceRepository, final FromJsonHelper fromApiJsonHelper,
            final CommandProcessingService processAndLogCommandService,
            @Qualifier("camelCommandProcessingService") CommandProcessingService camelCommandProcessingService,
            final SchedulerJobRunnerReadService schedulerJobRunnerReadService) {
        this.context = context;
        this.commandSourceRepository = commandSourceRepository;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.processAndLogCommandService = processAndLogCommandService;
        this.camelCommandProcessingService = camelCommandProcessingService;
        this.schedulerJobRunnerReadService = schedulerJobRunnerReadService;
    }

    @Override
    public CommandProcessingResult logCommandSource(final CommandWrapper wrapper) {

        boolean isApprovedByChecker = false;

        // check if is update of own account details
        if (wrapper.isUpdateOfOwnUserDetails(this.context.authenticatedUser(wrapper).getId())) {
            // then allow this operation to proceed.
            // maker checker doesnt mean anything here.
            isApprovedByChecker = true; // set to true in case permissions have
                                        // been maker-checker enabled by
                                        // accident.
        } else {
            // if not user changing their own details - check user has
            // permission to perform specific task.
            this.context.authenticatedUser(wrapper).validateHasPermissionTo(wrapper.getTaskPermissionName());
        }
        validateIsUpdateAllowed();

        CommandProcessingResult result = null;
        int numberOfRetries = 0;
        int maxNumberOfRetries = ThreadLocalContextUtil.getTenant().getConnection().getMaxRetriesOnDeadlock();
        int maxIntervalBetweenRetries = ThreadLocalContextUtil.getTenant().getConnection().getMaxIntervalBetweenRetries();

        final String json = wrapper.getJson();
        final JsonElement parsedCommand = this.fromApiJsonHelper.parse(json);
        JsonCommand command = JsonCommand.from(json, parsedCommand, this.fromApiJsonHelper, wrapper.getEntityName(), wrapper.getEntityId(),
                wrapper.getSubentityId(), wrapper.getGroupId(), wrapper.getClientId(), wrapper.getLoanId(), wrapper.getSavingsId(),
                wrapper.getTransactionId(), wrapper.getHref(), wrapper.getProductId(), wrapper.getCreditBureauId(),
                wrapper.getOrganisationCreditBureauId(), wrapper.getJobName());

        while (numberOfRetries <= maxNumberOfRetries) {
            try {
                result = this.getCommandProcessingService(wrapper).executeCommand(wrapper, command, isApprovedByChecker);
                numberOfRetries = maxNumberOfRetries + 1;
            } catch (CannotAcquireLockException | ObjectOptimisticLockingFailureException exception) {
                LOG.info("The following command {} has been retried  {} time(s)", command.json(), numberOfRetries);
                /***
                 * Fail if the transaction has been retired for maxNumberOfRetries
                 **/
                if (numberOfRetries >= maxNumberOfRetries) {
                    LOG.warn("The following command {} has been retried for the max allowed attempts of {} and will be rolled back",
                            command.json(), numberOfRetries);
                    throw exception;
                }
                /***
                 * Else sleep for a random time (between 1 to 10 seconds) and continue
                 **/
                try {
                    int randomNum = random.nextInt(maxIntervalBetweenRetries + 1);
                    Thread.sleep(1000 + (randomNum * 1000L));
                    numberOfRetries = numberOfRetries + 1;
                } catch (InterruptedException e) {
                    throw exception;
                }
            } catch (final RollbackTransactionAsCommandIsNotApprovedByCheckerException e) {
                numberOfRetries = maxNumberOfRetries + 1;
                result = this.getCommandProcessingService(wrapper).logCommand(e.getCommandSourceResult());
            }
        }

        return result;
    }

    private CommandProcessingService getCommandProcessingService(CommandWrapper wrapper) {
        if (frontendEnabled) {
            if (wrapper.actionName().startsWith(ASYNC_COMMAND_PREFIX)) {
                return this.camelCommandProcessingService;
            }
        }
        return this.processAndLogCommandService;
    }

    @Override
    public CommandProcessingResult approveEntry(final Long makerCheckerId) {

        final CommandSource commandSourceInput = validateMakerCheckerTransaction(makerCheckerId);
        validateIsUpdateAllowed();

        final CommandWrapper wrapper = CommandWrapper.fromExistingCommand(makerCheckerId, commandSourceInput.getActionName(),
                commandSourceInput.getEntityName(), commandSourceInput.resourceId(), commandSourceInput.subResourceId(),
                commandSourceInput.getResourceGetUrl(), commandSourceInput.getProductId(), commandSourceInput.getOfficeId(),
                commandSourceInput.getGroupId(), commandSourceInput.getClientId(), commandSourceInput.getLoanId(),
                commandSourceInput.getSavingsId(), commandSourceInput.getTransactionId(), commandSourceInput.getCreditBureauId(),
                commandSourceInput.getOrganisationCreditBureauId(), commandSourceInput.getIdempotencyKey());
        final JsonElement parsedCommand = this.fromApiJsonHelper.parse(commandSourceInput.getCommandJson());
        final JsonCommand command = JsonCommand.fromExistingCommand(makerCheckerId, commandSourceInput.getCommandJson(), parsedCommand,
                this.fromApiJsonHelper, commandSourceInput.getEntityName(), commandSourceInput.resourceId(),
                commandSourceInput.subResourceId(), commandSourceInput.getGroupId(), commandSourceInput.getClientId(),
                commandSourceInput.getLoanId(), commandSourceInput.getSavingsId(), commandSourceInput.getTransactionId(),
                commandSourceInput.getResourceGetUrl(), commandSourceInput.getProductId(), commandSourceInput.getCreditBureauId(),
                commandSourceInput.getOrganisationCreditBureauId(), commandSourceInput.getJobName());

        return this.processAndLogCommandService.executeCommand(wrapper, command, true);
    }

    @Transactional
    @Override
    public Long deleteEntry(final Long makerCheckerId) {

        validateMakerCheckerTransaction(makerCheckerId);
        validateIsUpdateAllowed();

        this.commandSourceRepository.deleteById(makerCheckerId);

        return makerCheckerId;
    }

    private CommandSource validateMakerCheckerTransaction(final Long makerCheckerId) {

        final CommandSource commandSourceInput = this.commandSourceRepository.findById(makerCheckerId)
                .orElseThrow(() -> new CommandNotFoundException(makerCheckerId));
        if (!commandSourceInput.isMarkedAsAwaitingApproval()) {
            throw new CommandNotAwaitingApprovalException(makerCheckerId);
        }

        this.context.authenticatedUser().validateHasCheckerPermissionTo(commandSourceInput.getPermissionCode());

        return commandSourceInput;
    }

    private void validateIsUpdateAllowed() {
        this.schedulerJobRunnerReadService.isUpdatesAllowed();
    }

    @Override
    public Long rejectEntry(final Long makerCheckerId) {
        final CommandSource commandSourceInput = validateMakerCheckerTransaction(makerCheckerId);
        validateIsUpdateAllowed();
        final AppUser maker = this.context.authenticatedUser();
        commandSourceInput.markAsRejected(maker);
        this.commandSourceRepository.save(commandSourceInput);
        return makerCheckerId;
    }
}
