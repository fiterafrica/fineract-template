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

import static org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType.ACTIVE;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.journalentry.exception.JournalEntryInvalidException;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.exception.ExceptionHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.data.JobDetailHistoryData;
import org.apache.fineract.infrastructure.jobs.domain.ScheduledJobDetail;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobExecuter;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.jobs.service.JobRunner;
import org.apache.fineract.infrastructure.jobs.service.SchedulerJobRunnerReadService;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.SavingsPostingInterestPeriodType;
import org.apache.fineract.portfolio.savings.domain.FixedDepositAccount;
import org.apache.fineract.portfolio.savings.domain.RecurringDepositAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsProduct;
import org.apache.fineract.portfolio.savings.domain.SavingsProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SavingsSchedularServiceImpl implements SavingsSchedularService {

    private final SavingsAccountAssembler savingAccountAssembler;
    private final SavingsAccountWritePlatformService savingsAccountWritePlatformService;
    private final SavingsAccountReadPlatformService savingAccountReadPlatformService;
    private final SavingsAccountRepositoryWrapper savingsAccountRepository;
    private final SavingsProductRepository savingsProductRepository;
    private final JobExecuter jobExecuter;
    private final SchedulerJobRunnerReadService schedulerJobRunnerReadService;
    private final SavingsAccountTransactionRepository savingsAccountTransactionRepository;
    private final JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(SavingsSchedularServiceImpl.class);

    @Override
    @CronTarget(jobName = JobName.UPDATE_SAVINGS_DORMANT_ACCOUNTS)
    public void updateSavingsDormancyStatus() {
        LocalDate tenantLocalDate = DateUtils.getBusinessLocalDate();
        List<Long> savingsPendingInactive = savingAccountReadPlatformService.retrieveSavingsIdsPendingInactive(tenantLocalDate);
        if (savingsPendingInactive != null && !savingsPendingInactive.isEmpty()) {
            for (Long savingsId : savingsPendingInactive) {
                this.savingsAccountWritePlatformService.setSubStatusInactive(savingsId);
            }
        }
        List<Long> savingsPendingDormant = savingAccountReadPlatformService.retrieveSavingsIdsPendingDormant(tenantLocalDate);
        if (savingsPendingDormant != null && !savingsPendingDormant.isEmpty()) {
            for (Long savingsId : savingsPendingDormant) {
                this.savingsAccountWritePlatformService.setSubStatusDormant(savingsId);
            }
        }
        List<Long> savingsPendingEscheat = savingAccountReadPlatformService.retrieveSavingsIdsPendingEscheat(tenantLocalDate);
        if (savingsPendingEscheat != null && !savingsPendingEscheat.isEmpty()) {
            for (Long savingsId : savingsPendingEscheat) {
                this.savingsAccountWritePlatformService.escheat(savingsId);
            }
        }
    }

    @Override
    @CronTarget(jobName = JobName.UPDATE_SAVINGS_INTEREST_POSTING_QUALIFY_CONFIG)
    public void updateSavingsInterestPostingQualifyConfig() {
        List<SavingsProduct> products = this.savingsProductRepository.findAll();
        log.info("Reading Savings Account Data!");
        for (SavingsProduct product : products) {
            List<SavingsAccount> savingsAccounts = this.savingsAccountRepository.findByProductIdAndStatus(product.getId(),
                    ACTIVE.getValue(), product.getNumOfCreditTransaction(), product.getNumOfDebitTransaction(),
                    product.minBalanceForInterestCalculation());
            if (!savingsAccounts.isEmpty()) {
                if (product.isInterestPostingUpdate()) {
                    for (SavingsAccount sav : savingsAccounts) {
                        sav.setNumOfCreditTransaction(product.getNumOfCreditTransaction());
                        sav.setNumOfDebitTransaction(product.getNumOfDebitTransaction());
                        sav.setMinBalanceForInterestCalculation(product.minBalanceForInterestCalculation());
                        this.savingsAccountRepository.saveAndFlush(sav);
                        log.info("Successfully Updates Savings Account Data! number is {}", sav.getId());
                    }
                }
            }
        }
    }

    @Override
    @CronTarget(jobName = JobName.POST_INTEREST_FOR_SAVINGS)
    public void postInterestForAccountsThreaded(Map<String, String> jobParameters) throws JobExecutionException {
        LocalDate today = DateUtils.getLocalDateOfTenant();
        SearchParameters searchParameters = SearchParameters.from(null, null, null, null, null);

        // Check if interest posting job has already run successfully today to prevent duplicates
        ScheduledJobDetail interestPostingJobDetails = schedulerJobRunnerReadService
                .findJobDetail(JobName.POST_INTEREST_FOR_SAVINGS.toString());
        if (interestPostingJobDetails != null) {
            List<JobDetailHistoryData> interestPostingHistory = schedulerJobRunnerReadService
                    .retrieveJobHistory(interestPostingJobDetails.getId(), searchParameters).getPageItems();
            JobDetailHistoryData latestInterestPostingHistory = interestPostingHistory.stream().sorted((h1, h2) -> {
                if (h1.getJobRunEndTime() == null && h2.getJobRunEndTime() == null) return 0;
                if (h1.getJobRunEndTime() == null) return 1;
                if (h2.getJobRunEndTime() == null) return -1;
                return h2.getJobRunEndTime().compareTo(h1.getJobRunEndTime());
            }).findFirst().orElse(null);
            if (latestInterestPostingHistory != null && latestInterestPostingHistory.getJobRunEndTime() != null) {
                LocalDate lastRunDate = latestInterestPostingHistory.getJobRunEndTime().toInstant().atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
                if (lastRunDate.equals(today) && "success".equalsIgnoreCase(latestInterestPostingHistory.getStatus())) {
                    String errorMsg = "POST_INTEREST_FOR_SAVINGS has already run successfully today. Skipping to prevent duplicate interest posting.";
                    log.warn(errorMsg);
                    throw new JobExecutionException(Collections.singletonList(new Exception(errorMsg)));
                }
            }
        }

        // Check if accrual job has run for today
        ScheduledJobDetail jobDetails = schedulerJobRunnerReadService.findJobDetail(JobName.POST_ACCRUAL_INTEREST_FOR_SAVINGS.toString());

        if (jobDetails == null) {
            String errorMsg = "Accrual job not found. Skipping interest posting.";
            log.error(errorMsg);
            throw new JobExecutionException(Collections.singletonList(new Exception(errorMsg)));
        }
        // Check job history for today

        List<JobDetailHistoryData> history = schedulerJobRunnerReadService.retrieveJobHistory(jobDetails.getId(), searchParameters)
                .getPageItems();
        JobDetailHistoryData latestHistory = history.stream().sorted((h1, h2) -> {
            if (h1.getJobRunEndTime() == null && h2.getJobRunEndTime() == null) return 0;
            if (h1.getJobRunEndTime() == null) return 1;
            if (h2.getJobRunEndTime() == null) return -1;
            return h2.getJobRunEndTime().compareTo(h1.getJobRunEndTime());
        }).findFirst().orElse(null);
        boolean accrualRunToday = false;
        if (latestHistory != null && latestHistory.getJobRunEndTime() != null) {
            LocalDate endDate = latestHistory.getJobRunEndTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            accrualRunToday = endDate.equals(today) && "success".equalsIgnoreCase(latestHistory.getStatus());
        }
        if (!accrualRunToday) {
            String errorMsg = "POST_ACCRUAL_INTEREST_FOR_SAVINGS has not run for today. Skipping interest posting.";
            log.error(errorMsg);
            throw new JobExecutionException(Collections.singletonList(new Exception(errorMsg)));
        }
        try {
            Thread nonInterestRecalculationThread = new Thread(new SavingsInterestRunnable());
            nonInterestRecalculationThread.start();
            nonInterestRecalculationThread.join();
        } catch (InterruptedException e) {
            logger.error("Thread Interrupted for Post  : {}", e.getMessage(), e);
            throw new JobExecutionException(Collections.singletonList(e));
        }
    }

    private class SavingsInterestRunnable implements Runnable {

        final FineractPlatformTenant tenant;
        final Authentication auth;
        final Map<String, Object> jobParams;
        final LocalDate jobRunDate;

        public SavingsInterestRunnable() {
            this.tenant = ThreadLocalContextUtil.getTenant();
            if (SecurityContextHolder.getContext() == null) {
                this.auth = null;
            } else {
                this.auth = SecurityContextHolder.getContext().getAuthentication();
            }
            this.jobParams = ThreadLocalContextUtil.getJobParams();
            this.jobRunDate = DateUtils.getLocalDateOfTenant();
        }

        @Override
        public void run() {
            ThreadLocalContextUtil.setTenant(tenant);
            ThreadLocalContextUtil.setJobParams(jobParams);
            if (this.auth != null) {
                SecurityContextHolder.getContext().setAuthentication(this.auth);
            }
            final List<Long> activeSavingsAccounts = savingAccountReadPlatformService.retrieveActiveSavingAccountsWithZeroInterest();
            activeSavingsAccounts.addAll(savingAccountReadPlatformService.retrieveActiveOverdraftSavingAccounts());
            JobRunner<List<Long>> runner = new SavingsInterestJobRunner(jobRunDate);
            jobExecuter.executeJob(activeSavingsAccounts, runner);
        }
    }

    private class SavingsInterestJobRunner implements JobRunner<List<Long>> {

        final int maxNumberOfRetries;
        final int maxIntervalBetweenRetries;
        final LocalDate jobRunDate;

        public SavingsInterestJobRunner(final LocalDate jobRunDate) {
            this.jobRunDate = jobRunDate;
            maxNumberOfRetries = ThreadLocalContextUtil.getTenant().getConnection().getMaxRetriesOnDeadlock();
            maxIntervalBetweenRetries = ThreadLocalContextUtil.getTenant().getConnection().getMaxIntervalBetweenRetries();
        }

        @Override
        public void runJob(final List<Long> savingIds, StringBuilder sb) {
            postInterest(sb, this.maxNumberOfRetries, this.maxIntervalBetweenRetries, savingIds, this.jobRunDate);
        }
    }

    private void postInterest(final StringBuilder sb, int maxNumberOfRetries, int maxIntervalBetweenRetries, List<Long> savingIds,
            LocalDate jobRunDate) {
        final String errorMessage = "Post Interest failed for account:";
        for (Long savingAccountId : savingIds) {
            if (savingAccountId == 0) {
                continue;
            }
            logger.info("Interest Saving ID {} which is {} of {}", savingAccountId, savingIds.indexOf(savingAccountId), savingIds.size());
            int numberOfRetries = 0;
            String savingsAccountNumber = "";
            while (numberOfRetries <= maxNumberOfRetries) {
                try {
                    final SavingsAccount savingAccount = this.savingAccountAssembler.assembleFrom(savingAccountId);
                    savingsAccountNumber = savingAccount.getAccountNumber();
                    checkClientOrGroupActive(savingAccount);

                    // Skip posting for Fixed Deposit and Recurring Deposit accounts when
                    // interest posting period is TENURE (At-Maturity) unless today is the maturity date
                    if (savingAccount instanceof FixedDepositAccount || savingAccount instanceof RecurringDepositAccount) {
                        Integer postingPeriodType = savingAccount.getInterestPostingPeriodType();
                        if (Objects.equals(SavingsPostingInterestPeriodType.TENURE.getValue(), postingPeriodType)) {
                            LocalDate maturityDate = null;
                            if (savingAccount instanceof FixedDepositAccount) {
                                maturityDate = ((FixedDepositAccount) savingAccount).maturityDate();
                            } else if (savingAccount instanceof RecurringDepositAccount) {
                                maturityDate = ((RecurringDepositAccount) savingAccount).maturityDate();
                            }
                            if (!jobRunDate.equals(maturityDate)) {
                                // Do not post interest today for At-Maturity posting period
                                log.info("Skipping At-Maturity interest posting for account {} (id={}) - maturityDate={} (jobRunDate={})",
                                        savingsAccountNumber, savingAccount.getId(), maturityDate, jobRunDate);
                                break; // exit retry loop for this account
                            }
                        }
                    }

                    if (!savingAccount.isPostOverdraftInterestOnDeposit()) {
                        this.savingsAccountWritePlatformService.postInterest(savingAccount, false, jobRunDate);
                    }
                    break;
                } catch (CannotAcquireLockException | ObjectOptimisticLockingFailureException exception) {
                    logger.info("Recalulate interest job has been retried  {} time(s)", numberOfRetries);
                    // Fail if the transaction has been retried for maxNumberOfRetries
                    if (numberOfRetries >= maxNumberOfRetries) {
                        logger.warn("Post interest job has been retried for the max allowed attempts of {} and will be rolled back.",
                                numberOfRetries);
                        sb.append("Post interest job has been retried for the max allowed attempts of " + numberOfRetries
                                + " and will be rolled back. ");
                        break;
                    }
                    // Else sleep for a random time (between 1 to 10 seconds) and continue
                    try {
                        Random random = new Random();
                        int randomNum = random.nextInt(maxIntervalBetweenRetries + 1);
                        Thread.sleep(1000L + (randomNum * 1000L));
                        numberOfRetries = numberOfRetries + 1;
                    } catch (InterruptedException e) {
                        sb.append("Post interest for savings failed " + exception.getMessage());
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
                        sb.append(" Failed to post interest for Savings with id " + savingsAccountNumber + " with message " + message);
                    } else {
                        ExceptionHelper.handleExceptions(e, sb, errorMessage, savingAccountId, logger);
                    }
                    numberOfRetries = maxNumberOfRetries + 1;
                }
            }
        }
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

    @CronTarget(jobName = JobName.REVERSAL_PREMATURE_INTEREST_POSTING)
    @Override
    public void recalculatePrematureInterestPostings() throws JobExecutionException {
        log.info("Running REVERSAL_PREMATURE_INTEREST_POSTING job");
        // Find deposit accounts (Fixed + Recurring) with posting period TENURE and maturity date not null
        final String sql = "SELECT da.id FROM m_savings_account da INNER JOIN m_deposit_account_term_and_preclosure dat ON dat.savings_account_id = da.id "
                + "WHERE da.interest_posting_period_enum = 8 AND dat.maturity_date IS NOT NULL and da.status_enum = 300";
        List<Long> accountIds = this.jdbcTemplate.queryForList(sql, Long.class);
        log.info("Found {} deposit accounts with TENURE posting to inspect", accountIds.size());
        for (Long accountId : accountIds) {
            try {
                SavingsAccount account = this.savingsAccountRepository.findOneWithNotFoundDetection(accountId);
                // Only process Fixed or Recurring
                if (!(account instanceof FixedDepositAccount || account instanceof RecurringDepositAccount)) {
                    continue;
                }
                LocalDate maturityDate = null;
                if (account instanceof FixedDepositAccount) {
                    maturityDate = ((FixedDepositAccount) account).maturityDate();
                } else if (account instanceof RecurringDepositAccount) {
                    maturityDate = ((RecurringDepositAccount) account).maturityDate();
                }
                if (maturityDate == null) {
                    continue;
                }

                // Find interest posting transactions that occurred before maturity
                List<SavingsAccountTransaction> txns = this.savingsAccountTransactionRepository.getTransactionsByAccountIdAndType(accountId,
                        SavingsAccountTransactionType.INTEREST_POSTING.getValue());
                boolean shouldRepost = false;
                for (SavingsAccountTransaction txn : txns) {
                    if (!txn.isNotReversed() || txn.isReversalTransaction()) {
                        continue;
                    }
                    if (txn.transactionLocalDate().isBefore(maturityDate)) {
                        // reverse this transaction
                        txn.reverse();
                        this.savingsAccountTransactionRepository.save(txn);
                        log.info("Reversed premature interest posting txn id {} for account {}", txn.getId(), accountId);
                        shouldRepost = true;
                    }
                }
                if (shouldRepost) {
                    // reload account to ensure transactions collection reflects reversal
                    account = this.savingsAccountRepository.findOneWithNotFoundDetection(accountId);
                    // Post maturity interest using domain method
                    if (account instanceof FixedDepositAccount) {
                        ((FixedDepositAccount) account).postMaturityInterest(false, null);
                    } else if (account instanceof RecurringDepositAccount) {
                        ((RecurringDepositAccount) account).postMaturityInterest(false, null, maturityDate, false);
                    }
                    // save account
                    this.savingsAccountRepository.saveAndFlush(account);
                    log.info("Reposted maturity interest for account {}", accountId);
                }
            } catch (Exception e) {
                log.error("Error fixing premature interest postings for account {}: {}", accountId, e.getMessage(), e);
            }
        }
        log.info("FIX_PREMATURE_INTEREST_POSTING job completed");
    }
}
