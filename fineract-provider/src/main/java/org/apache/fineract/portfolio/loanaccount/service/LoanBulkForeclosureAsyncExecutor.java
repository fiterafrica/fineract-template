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
package org.apache.fineract.portfolio.loanaccount.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.loanaccount.domain.BulkForeclosureJob;
import org.apache.fineract.portfolio.loanaccount.domain.BulkForeclosureJobDetail;
import org.apache.fineract.portfolio.loanaccount.domain.BulkForeclosureJobDetailRepository;
import org.apache.fineract.portfolio.loanaccount.domain.BulkForeclosureJobRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanBulkForeclosureAsyncExecutor {

    private final LoanBulkForeclosureTransactionalHelper transactionalHelper;
    private final BulkForeclosureJobRepository jobRepository;
    private final BulkForeclosureJobDetailRepository jobDetailRepository;

    // Configuration for batch processing
    private static final int BATCH_SIZE = 10;
    private static final int MAX_CONCURRENT_THREADS = 4;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @Async
    public void processLoansAsync(Long jobPkId, List<Long> loanIds, LocalDate foreclosureDate, FineractContext context) {
        // Set full context for this async thread (includes tenant, business dates, etc.)
        ThreadLocalContextUtil.init(context);

        log.info("Starting async bulk foreclosure for job {} with {} loans", jobPkId, loanIds.size());

        final BulkForeclosureJob job = jobRepository.findById(jobPkId).orElseThrow();
        job.setStatus("RUNNING");
        jobRepository.saveAndFlush(job);

        final AtomicInteger successful = new AtomicInteger(0);
        final AtomicInteger failed = new AtomicInteger(0);
        final List<BulkForeclosureJobDetail> batchedDetails = new ArrayList<>();

        // Split into batches for controlled processing
        List<List<Long>> batches = partitionList(loanIds, BATCH_SIZE);
        log.info("Job {}: Split {} loans into {} batches of size {}", job.getJobId(), loanIds.size(), batches.size(), BATCH_SIZE);

        // Use limited thread pool to control concurrency
        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_THREADS);

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (List<Long> batch : batches) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    // Restore full context in each worker thread
                    ThreadLocalContextUtil.init(context);
                    try {
                        processBatch(job, batch, foreclosureDate, successful, failed, batchedDetails);
                    } finally {
                        ThreadLocalContextUtil.clearTenant();
                    }
                }, executor);
                futures.add(future);
            }

            // Wait for all batches with timeout
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("Error during async batch processing for job {}: {}", job.getJobId(), e.getMessage(), e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Batch save all details
        synchronized (batchedDetails) {
            if (!batchedDetails.isEmpty()) {
                jobDetailRepository.saveAll(batchedDetails);
                jobDetailRepository.flush();
            }
        }

        // Update final job status
        job.setSuccessful(successful.get());
        job.setFailed(failed.get());
        job.setStatus(failed.get() == loanIds.size() ? "FAILED" : "COMPLETED");
        job.setCompletedOn(LocalDateTime.now(DateUtils.getDateTimeZoneOfTenant()));
        jobRepository.saveAndFlush(job);

        log.info("Job {} completed: {} successful, {} failed out of {} total loans", job.getJobId(), successful.get(), failed.get(),
                loanIds.size());
    }

    private void processBatch(BulkForeclosureJob job, List<Long> loanIds, LocalDate foreclosureDate, AtomicInteger successful,
            AtomicInteger failed, List<BulkForeclosureJobDetail> batchedDetails) {

        for (Long loanId : loanIds) {
            // Idempotency check
            if (jobDetailRepository.existsByJob_IdAndLoanId(job.getId(), loanId)) {
                log.info("Loan {} already processed for job {}, skipping", loanId, job.getJobId());
                continue;
            }

            boolean success = false;
            String lastError = null;

            // Retry strategy
            for (int attempt = 1; attempt <= MAX_RETRIES && !success; attempt++) {
                try {
                    String validationError = transactionalHelper.forecloseSingleLoan(loanId, foreclosureDate);
                    if (validationError != null) {
                        lastError = validationError;
                        break; // Validation errors are not retryable
                    } else {
                        success = true;
                    }
                } catch (Exception e) {
                    lastError = e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                            : "Unknown error";

                    if (isRetryableError(e) && attempt < MAX_RETRIES) {
                        log.warn("Retryable error for loan {} (attempt {}/{}): {}", loanId, attempt, MAX_RETRIES, lastError);
                        try {
                            Thread.sleep(RETRY_DELAY_MS * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        log.error("Non-retryable error for loan {}: {}", loanId, lastError, e);
                        break;
                    }
                }
            }

            synchronized (batchedDetails) {
                if (success) {
                    successful.incrementAndGet();
                    batchedDetails.add(BulkForeclosureJobDetail.success(job, loanId));
                } else {
                    failed.incrementAndGet();
                    batchedDetails.add(BulkForeclosureJobDetail.failure(job, loanId, lastError));
                    log.warn("Bulk foreclosure failed for loan {}: {}", loanId, lastError);
                }
            }
        }
    }

    private boolean isRetryableError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("deadlock") || message.contains("lock wait timeout") || message.contains("connection")
                || message.contains("timeout") || e.getCause() instanceof java.sql.SQLException;
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            // Create a copy of the sublist to avoid issues with list views in concurrent processing
            partitions.add(new ArrayList<>(list.subList(i, Math.min(i + batchSize, list.size()))));
        }
        return partitions;
    }
}
