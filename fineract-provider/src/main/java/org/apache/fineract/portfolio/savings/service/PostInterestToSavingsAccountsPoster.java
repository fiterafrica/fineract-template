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
package org.apache.fineract.portfolio.savings.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.fineract.accounting.journalentry.exception.JournalEntryInvalidException;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class PostInterestToSavingsAccountsPoster implements Callable<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(PostInterestToSavingsAccountsPoster.class);
    private static final SecureRandom random = new SecureRandom();
    private List<Long> savingsAccountIds;
    private SavingsAccountWritePlatformService savingsAccountWritePlatformService;
    private SavingsAccountAssembler savingAccountAssembler;

    private FineractContext context;

    public void setContext(FineractContext context) {
        this.context = context;
    }

    public void setSavingsAccountIds(List<Long> savingsAccountIds) {
        this.savingsAccountIds = savingsAccountIds;
    }

    public void setSavingsAccountWritePlatformService(SavingsAccountWritePlatformService savingsAccountWritePlatformService) {
        this.savingsAccountWritePlatformService = savingsAccountWritePlatformService;
    }

    public void setSavingAccountAssembler(SavingsAccountAssembler savingAccountAssembler) {
        this.savingAccountAssembler = savingAccountAssembler;
    }

    @Override
    @SuppressFBWarnings(value = {
            "DMI_RANDOM_USED_ONLY_ONCE" }, justification = "False positive for random object created and used only once")
    public Void call() throws JobExecutionException {
        ThreadLocalContextUtil.init(this.context);
        Integer maxNumberOfRetries = this.context.getTenantContext().getConnection().getMaxRetriesOnDeadlock();
        Integer maxIntervalBetweenRetries = this.context.getTenantContext().getConnection().getMaxIntervalBetweenRetries();
        LocalDate jobRunDate = DateUtils.getLocalDateOfTenant();
        if (!savingsAccountIds.isEmpty()) {
            List<Throwable> errors = new ArrayList<>();
            for (Long savingAccountId : savingsAccountIds) {
                if (savingAccountId == 0) {
                    continue;
                }
                LOG.info("Processing Saving Account For Interest Posting: ID " + savingAccountId);
                Integer numberOfRetries = 0;
                String savingsAccountNumber = "";
                while (numberOfRetries <= maxNumberOfRetries) {
                    try {
                        final SavingsAccount savingAccount = this.savingAccountAssembler.assembleFrom(savingAccountId);
                        savingsAccountNumber = savingAccount.getAccountNumber();
                        checkClientOrGroupActive(savingAccount);
                        if (!savingAccount.isPostOverdraftInterestOnDeposit()) {
                            this.savingsAccountWritePlatformService.postInterest(savingAccount, false, jobRunDate);
                        }
                        numberOfRetries = maxNumberOfRetries + 1;
                    } catch (CannotAcquireLockException | ObjectOptimisticLockingFailureException exception) {
                        LOG.info("Post Interest For Savings job has been retried {} time(s)", numberOfRetries);
                        // Fail if the transaction has been retired for
                        // maxNumberOfRetries
                        if (numberOfRetries >= maxNumberOfRetries) {
                            LOG.error(
                                    "Post Interest For Savings job has been retried for the max allowed attempts of {} and will be rolled back",
                                    numberOfRetries);
                            errors.add(exception);
                            break;
                        }
                        // Else sleep for a random time (between 1 to 10
                        // seconds) and continue
                        try {
                            int randomNum = random.nextInt(maxIntervalBetweenRetries + 1);
                            Thread.sleep(1000 + (randomNum * 1000));
                            numberOfRetries = numberOfRetries + 1;
                        } catch (InterruptedException e) {
                            LOG.error("Post Interest For Savings failed for account " + savingsAccountNumber + "due to InterruptedException", e);
                            errors.add(e);
                            break;
                        }
                    } catch (Exception e) {
                        if (e instanceof JournalEntryInvalidException) {
                            Throwable realCause = e;
                            if (e.getCause() != null) {
                                realCause = e.getCause();
                            }
                            String message = realCause.getMessage();
                            if (message == null && realCause instanceof JournalEntryInvalidException) {
                                message = ((JournalEntryInvalidException) realCause).getDefaultUserMessage();
                            }
                            LOG.error("Post Interest For Savings failed for account " + savingsAccountNumber + " with message " + message);
                        } else {
                            LOG.error("Post Interest For Savings failed for account {}", savingsAccountNumber, e);
                        }
                        numberOfRetries = maxNumberOfRetries + 1;
                        errors.add(e);
                    }
                }
            }
            if (!errors.isEmpty()) {
                throw new JobExecutionException(errors);
            }
        }
        return null;
    }

    private void checkClientOrGroupActive(final SavingsAccount account) {
        final Client client = account.getClient();
        if (client != null) {
            if (client.isNotActive()) {
                throw new ClientNotActiveException(client.getId());
            }
        }
        final Group group = account.group();
        if (group != null) {
            if (group.isNotActive()) {
                throw new GroupNotActiveException(group.getId());
            }
        }
    }
}
