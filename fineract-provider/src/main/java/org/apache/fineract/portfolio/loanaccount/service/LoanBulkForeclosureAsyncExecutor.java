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
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanBulkForeclosureAsyncExecutor {

    private final LoanBulkForeclosureTransactionalHelper transactionalHelper;
    private final BulkForeclosureJobRepository jobRepository;
    private final BulkForeclosureJobDetailRepository jobDetailRepository;
    private final LoanRepositoryWrapper loanRepositoryWrapper;

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

            // Fetch loan details for audit trail
            String loanAccountNo = null;
            String clientName = null;
            try {
                Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
                loanAccountNo = loan.getAccountNumber();
                if (loan.getClient() != null) {
                    clientName = loan.getClient().getDisplayName();
                } else if (loan.getGroup() != null) {
                    clientName = loan.getGroup().getName();
                }
            } catch (Exception e) {
                log.warn("Could not fetch loan details for loan {}: {}", loanId, e.getMessage());
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
                    batchedDetails.add(BulkForeclosureJobDetail.success(job, loanId, loanAccountNo, clientName));
                } else {
                    failed.incrementAndGet();
                    batchedDetails.add(BulkForeclosureJobDetail.failure(job, loanId, loanAccountNo, clientName, lastError));
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

    /**
     * Async method to retry failed foreclosure records.
     *
     * @param job
     *            the bulk foreclosure job
     * @param failedDetails
     *            list of failed detail records to retry
     * @param context
     *            the Fineract context
     */
    @Async
    public void retryFailedRecordsAsync(BulkForeclosureJob job, List<BulkForeclosureJobDetail> failedDetails, FineractContext context) {
        // Set full context for this async thread
        ThreadLocalContextUtil.init(context);

        log.info("Starting async retry for job {} with {} failed records", job.getJobId(), failedDetails.size());

        final AtomicInteger successful = new AtomicInteger(0);
        final AtomicInteger failed = new AtomicInteger(0);

        // Split into batches for controlled processing
        List<List<BulkForeclosureJobDetail>> batches = partitionList(failedDetails, BATCH_SIZE);
        log.info("Job {} retry: Split {} records into {} batches", job.getJobId(), failedDetails.size(), batches.size());

        // Use limited thread pool to control concurrency
        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_THREADS);

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (List<BulkForeclosureJobDetail> batch : batches) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    // Restore full context in each worker thread
                    ThreadLocalContextUtil.init(context);
                    try {
                        processRetryBatch(job, batch, successful, failed);
                    } finally {
                        ThreadLocalContextUtil.clearTenant();
                    }
                }, executor);
                futures.add(future);
            }

            // Wait for all batches with timeout
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("Error during async retry processing for job {}: {}", job.getJobId(), e.getMessage(), e);
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

        // Update job counters
        long totalSuccessful = jobDetailRepository.countSuccessfulByJob(job.getId());
        long totalFailed = jobDetailRepository.countFailedByJob(job.getId());
        job.setSuccessful((int) totalSuccessful);
        job.setFailed((int) totalFailed);

        // Update job status based on new totals
        if (totalFailed == 0) {
            job.setStatus("COMPLETED");
        } else if (totalSuccessful == 0) {
            job.setStatus("FAILED");
        } else {
            job.setStatus("COMPLETED"); // Partial success is still COMPLETED
        }
        jobRepository.saveAndFlush(job);

        log.info("Async retry completed for job {}: {} newly successful, {} still failed", job.getJobId(), successful.get(), failed.get());
    }

    /**
     * Process a batch of retry records.
     */
    private void processRetryBatch(BulkForeclosureJob job, List<BulkForeclosureJobDetail> details, AtomicInteger successful,
            AtomicInteger failed) {

        for (BulkForeclosureJobDetail detail : details) {
            if (!detail.canRetry()) {
                log.warn("Detail {} cannot be retried (status: {})", detail.getId(), detail.getStatus());
                continue;
            }

            boolean success = false;
            String lastError = null;

            // Retry strategy for transient failures
            for (int attempt = 1; attempt <= MAX_RETRIES && !success; attempt++) {
                try {
                    String validationError = transactionalHelper.forecloseSingleLoan(detail.getLoanId(), job.getForeclosureDate());
                    if (validationError != null) {
                        lastError = validationError;
                        // Validation errors are not retryable
                        break;
                    } else {
                        success = true;
                    }
                } catch (Exception e) {
                    lastError = e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                            : "Unknown error";

                    if (isRetryableError(e) && attempt < MAX_RETRIES) {
                        log.warn("Retryable error for loan {} retry (attempt {}/{}): {}", detail.getLoanId(), attempt, MAX_RETRIES,
                                lastError);
                        try {
                            Thread.sleep(RETRY_DELAY_MS * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        log.error("Non-retryable error for loan {} retry: {}", detail.getLoanId(), lastError, e);
                        break;
                    }
                }
            }

            // Update detail status
            if (success) {
                detail.markAsRetrySuccess();
                successful.incrementAndGet();
                log.info("Async retry successful for loan {} (detail {})", detail.getLoanId(), detail.getId());
            } else {
                detail.markAsRetryFailed(lastError);
                failed.incrementAndGet();
                log.warn("Async retry failed for loan {} (detail {}): {}", detail.getLoanId(), detail.getId(), lastError);
            }

            // Save the updated detail
            jobDetailRepository.save(detail);
        }
    }
}
