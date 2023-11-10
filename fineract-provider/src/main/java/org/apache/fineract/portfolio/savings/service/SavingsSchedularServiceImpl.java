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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobExecuter;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsProduct;
import org.apache.fineract.portfolio.savings.domain.SavingsProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class SavingsSchedularServiceImpl implements SavingsSchedularService {

    private final int queueSize = 1;

    private final SavingsAccountAssembler savingAccountAssembler;
    private final SavingsAccountWritePlatformService savingsAccountWritePlatformService;
    private final SavingsAccountReadPlatformService savingAccountReadPlatformService;
    private final SavingsAccountRepositoryWrapper savingsAccountRepository;
    private final ApplicationContext applicationContext;
    private final ConfigurationDomainService configurationDomainService;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private Queue<List<SavingsAccountData>> queue = new ArrayDeque<>();
    private final SavingsAccountRepositoryWrapper savingAccountRepositoryWrapper;

    private final SavingsProductRepository savingsProductRepository;
    private final JobExecuter jobExecuter;

    private static final Logger logger = LoggerFactory.getLogger(SavingsSchedularServiceImpl.class);

    @Override
    @CronTarget(jobName = JobName.UPDATE_SAVINGS_DORMANT_ACCOUNTS)
    public void updateSavingsDormancyStatus() throws JobExecutionException {
        LocalDate tenantLocalDate = DateUtils.getBusinessLocalDate();

        List<Long> savingsPendingInactive = savingAccountReadPlatformService.retrieveSavingsIdsPendingInactive(tenantLocalDate);
        if (null != savingsPendingInactive && savingsPendingInactive.size() > 0) {
            for (Long savingsId : savingsPendingInactive) {
                this.savingsAccountWritePlatformService.setSubStatusInactive(savingsId);
            }
        }

        List<Long> savingsPendingDormant = savingAccountReadPlatformService.retrieveSavingsIdsPendingDormant(tenantLocalDate);
        if (null != savingsPendingDormant && savingsPendingDormant.size() > 0) {
            for (Long savingsId : savingsPendingDormant) {
                this.savingsAccountWritePlatformService.setSubStatusDormant(savingsId);
            }
        }

        List<Long> savingsPendingEscheat = savingAccountReadPlatformService.retrieveSavingsIdsPendingEscheat(tenantLocalDate);
        if (null != savingsPendingEscheat && savingsPendingEscheat.size() > 0) {
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
            if (savingsAccounts.size() > 0) {
                if (product.isInterestPostingUpdate()) {
                    for (SavingsAccount sav : savingsAccounts) {
                        sav.setNumOfCreditTransaction(product.getNumOfCreditTransaction());
                        sav.setNumOfDebitTransaction(product.getNumOfDebitTransaction());
                        sav.setMinBalanceForInterestCalculation(product.minBalanceForInterestCalculation());
                        this.savingsAccountRepository.saveAndFlush(sav);
                        log.info("Successfully Updates Savings Account Data! number is" + sav.getId());
                    }
                }
            }
        }
    }

    @Override
    @CronTarget(jobName = JobName.POST_INTEREST_FOR_SAVINGS)
    public void postInterestForSavings(Map<String, String> jobParameters) throws JobExecutionException {

        final Queue<List<Long>> queue = new ArrayDeque<>();
        final ApplicationContext applicationContext;
        final int threadPoolSize = Integer.parseInt(jobParameters.get("thread-pool-size"));
        final int batchSize = Integer.parseInt(jobParameters.get("batch-size"));
        final int pageSize = batchSize * threadPoolSize;
        Long maxSavingsIdInList = 0L;
        final List<Long> activeSavingsAccounts = savingAccountReadPlatformService
                .retrieveActiveSavingAccountsForInterestPosting(maxSavingsIdInList, pageSize);
        final ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        if (activeSavingsAccounts != null && !activeSavingsAccounts.isEmpty()) {
            queue.add(activeSavingsAccounts.stream().toList());
            if (!CollectionUtils.isEmpty(queue)) {
                do {
                    log.info("Starting Job Post Interest For Savings Accounts");
                    List<Long> queueElement = queue.element();
                    maxSavingsIdInList = queueElement.get(queueElement.size() - 1);
                    postInterestForSavings(queue.remove(), queue, threadPoolSize, executorService, pageSize, maxSavingsIdInList);
                } while (!CollectionUtils.isEmpty(queue));
            }
            // shutdown the executor when done
            executorService.shutdownNow();
        }
    }

    private void postInterestForSavings(List<Long> activeSavingsAccounts, Queue<List<Long>> queue, int threadPoolSize,
            ExecutorService executorService, int pageSize, Long maxSavingsIdInList) {
        List<Callable<Void>> posters = new ArrayList<>();
        int fromIndex = 0;
        int size = activeSavingsAccounts.size();
        int batchSize = (int) Math.ceil((double) size / threadPoolSize);
        if (batchSize == 0) {
            return;
        }
        int toIndex = (batchSize > size - 1) ? size : batchSize;
        while (toIndex < size && activeSavingsAccounts.get(toIndex - 1).equals(activeSavingsAccounts.get(toIndex))) {
            toIndex++;
        }
        boolean lastBatch = false;
        int loopCount = size / batchSize + 1;

        FineractContext context = ThreadLocalContextUtil.getContext();

        Callable<Void> fetchData = () -> {
            ThreadLocalContextUtil.init(context);
            Long maxId = maxSavingsIdInList;
            if (!queue.isEmpty()) {
                maxId = Math.max(maxSavingsIdInList, queue.element().get(queue.element().size() - 1));
            }
            while (queue.size() <= queueSize) {
                log.info("Fetching while threads are running!");
                final List<Long> savingsAccounts = savingAccountReadPlatformService.retrieveActiveSavingAccountsForInterestPosting(maxId,
                        pageSize);
                if (savingsAccounts.isEmpty()) {
                    break;
                }
                maxId = savingsAccounts.get(savingsAccounts.size() - 1);
                queue.add(savingsAccounts);
            }
            return null;
        };
        posters.add(fetchData);

        for (long i = 0; i < loopCount; i++) {
            List<Long> subList = safeSubList(activeSavingsAccounts, fromIndex, toIndex);
            PostInterestToSavingsAccountsPoster poster = (PostInterestToSavingsAccountsPoster) applicationContext
                    .getBean("postInterestToSavingsAccountsPoster");
            poster.setSavingsAccountIds(subList);
            poster.setSavingsAccountWritePlatformService(this.savingsAccountWritePlatformService);
            poster.setSavingAccountAssembler(this.savingAccountAssembler);
            poster.setContext(ThreadLocalContextUtil.getContext());

            posters.add(poster);
            if (lastBatch) {
                break;
            }
            if (toIndex + batchSize > size - 1) {
                lastBatch = true;
            }
            fromIndex = fromIndex + (toIndex - fromIndex);
            toIndex = (toIndex + batchSize > size - 1) ? size : toIndex + batchSize;
            while (toIndex < size && activeSavingsAccounts.get(toIndex - 1).equals(activeSavingsAccounts.get(toIndex))) {
                toIndex++;
            }
        }
        try {
            List<Future<Void>> responses = executorService.invokeAll(posters);
            Long maxId = maxSavingsIdInList;
            if (!queue.isEmpty()) {
                maxId = Math.max(maxSavingsIdInList, queue.element().get(queue.element().size() - 1));
            }
            while (queue.size() <= queueSize) {
                log.info("Fetching while threads are running!..:: this is not supposed to run........");
                activeSavingsAccounts = savingAccountReadPlatformService.retrieveActiveSavingAccountsForInterestPosting(maxId, pageSize);

                if (activeSavingsAccounts.isEmpty()) {
                    break;
                }
                maxId = activeSavingsAccounts.get(activeSavingsAccounts.size() - 1);
                log.info("Add to the Queue");
                queue.add(activeSavingsAccounts);
            }
            checkTaskCompletion(responses);
            log.info("Queue size {}", queue.size());
        } catch (InterruptedException e1) {
            log.error("Interrupted while AddPenalty", e1);
        }
    }

    private <T> List<T> safeSubList(List<T> list, int fromIndex, int toIndex) {
        int size = list.size();
        if (fromIndex >= size || toIndex <= 0 || fromIndex >= toIndex) {
            return Collections.emptyList();
        }

        fromIndex = Math.max(0, fromIndex);
        toIndex = Math.min(size, toIndex);

        return list.subList(fromIndex, toIndex);
    }

    private void checkTaskCompletion(List<Future<Void>> responses) {
        try {
            for (Future<Void> f : responses) {
                f.get();
            }
            boolean allThreadsExecuted;
            int noOfThreadsExecuted = 0;
            for (Future<Void> future : responses) {
                if (future.isDone()) {
                    noOfThreadsExecuted++;
                }
            }
            allThreadsExecuted = noOfThreadsExecuted == responses.size();
            if (!allThreadsExecuted) {
                log.error("All threads could not execute.");
            }
        } catch (InterruptedException e1) {
            log.error("Interrupted while interest posting entries", e1);
        } catch (ExecutionException e2) {
            log.error("Execution exception while interest posting entries", e2);
        }
    }
}
