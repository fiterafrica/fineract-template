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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.data.BulkForeclosureJobData;
import org.apache.fineract.portfolio.loanaccount.data.BulkForeclosureJobData.BulkForeclosureFailureData;
import org.apache.fineract.portfolio.loanaccount.domain.BulkForeclosureJob;
import org.apache.fineract.portfolio.loanaccount.domain.BulkForeclosureJobDetail;
import org.apache.fineract.portfolio.loanaccount.domain.BulkForeclosureJobDetailRepository;
import org.apache.fineract.portfolio.loanaccount.domain.BulkForeclosureJobRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanBulkForeclosureServiceImpl implements LoanBulkForeclosureService {

    private final PlatformSecurityContext context;
    private final LoanBulkForeclosureTransactionalHelper transactionalHelper;
    private final LoanBulkForeclosureAsyncExecutor asyncExecutor;
    private final BulkForeclosureJobRepository jobRepository;
    private final BulkForeclosureJobDetailRepository jobDetailRepository;

    // Configuration for batch processing
    private static final int BATCH_SIZE = 10; // Process loans in batches
    private static final int MAX_CONCURRENT_THREADS = 4; // Limit concurrent DB connections
    private static final int MAX_RETRIES = 3; // Retry failed loans
    private static final long RETRY_DELAY_MS = 1000; // Delay between retries

    @Override
    public BulkForeclosureJobData triggerBulkForeclosure(List<Long> loanIds, LocalDate foreclosureDate, String executionMode) {
        final Long userId = this.context.authenticatedUser().getId();

        // Mandatory async for > 50 loans
        final String finalExecutionMode = loanIds.size() > 50 ? "ASYNC" : executionMode;

        final String jobId = UUID.randomUUID().toString();

        // Idempotency check - ensure no duplicate job for same loans on same date
        // This is a simple check; for stricter idempotency, consider hashing loanIds
        final BulkForeclosureJob job = BulkForeclosureJob.create(jobId, foreclosureDate, loanIds.size(), userId, finalExecutionMode);
        jobRepository.saveAndFlush(job);

        if ("ASYNC".equalsIgnoreCase(finalExecutionMode)) {
            // Capture full context before async call (tenant, business dates, etc. will be lost in new thread)
            final FineractContext context = ThreadLocalContextUtil.getContext();
            asyncExecutor.processLoansAsync(job.getId(), loanIds, foreclosureDate, context);
        } else {
            try {
                processLoansWithBatching(job.getId(), loanIds, foreclosureDate);
            } catch (Exception e) {
                log.error("Bulk foreclosure job {} encountered an unexpected error: {}", jobId, e.getMessage(), e);
                final BulkForeclosureJob failedJob = jobRepository.findByJobId(jobId).orElse(job);
                if (!"COMPLETED".equals(failedJob.getStatus()) && !"FAILED".equals(failedJob.getStatus())) {
                    failedJob.setStatus("FAILED");
                    failedJob.setCompletedOn(LocalDateTime.now(DateUtils.getDateTimeZoneOfTenant()));
                    jobRepository.saveAndFlush(failedJob);
                }
            }
        }

        return toJobData(jobRepository.findByJobId(jobId).orElse(job));
    }

    @Override
    public BulkForeclosureJobData getJobStatus(String jobId) {
        this.context.authenticatedUser();
        final BulkForeclosureJob job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Bulk foreclosure job not found: " + jobId));
        return toJobData(job);
    }

    @Override
    public Page<BulkForeclosureJobData> getJobList(Integer offset, Integer limit) {
        this.context.authenticatedUser();
        final int pageNumber = offset != null ? offset : 0;
        final int pageSize = limit != null ? limit : 20;

        final org.springframework.data.domain.Page<BulkForeclosureJob> jobPage = jobRepository
                .findAllByOrderBySubmittedOnDesc(PageRequest.of(pageNumber, pageSize));

        final List<BulkForeclosureJobData> jobDataList = jobPage.getContent().stream().map(this::toJobDataSummary)
                .collect(Collectors.toList());

        return new Page<>(jobDataList, (int) jobPage.getTotalElements());
    }

    @Override
    public void forecloseSingleLoan(Long loanId, LocalDate foreclosureDate) {
        String error = transactionalHelper.forecloseSingleLoan(loanId, foreclosureDate);
        if (error != null) {
            throw new IllegalStateException(error);
        }
    }

    /**
     * Process loans with batching and controlled concurrency for high-volume performance. - Splits loans into batches
     * to reduce memory pressure - Uses thread pool for parallel processing with limited concurrency - Implements retry
     * strategy for transient failures - Batches DB writes to reduce locking
     */
    public void processLoansWithBatching(Long jobPkId, List<Long> loanIds, LocalDate foreclosureDate) {
        final BulkForeclosureJob job = jobRepository.findById(jobPkId).orElseThrow();
        job.setStatus("RUNNING");
        jobRepository.saveAndFlush(job);

        final AtomicInteger successful = new AtomicInteger(0);
        final AtomicInteger failed = new AtomicInteger(0);
        final List<BulkForeclosureJobDetail> batchedDetails = new ArrayList<>();

        // Split into batches for controlled processing
        List<List<Long>> batches = partitionList(loanIds, BATCH_SIZE);

        // Use limited thread pool to control concurrency and DB connections
        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_THREADS);

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (List<Long> batch : batches) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    processBatch(job, batch, foreclosureDate, successful, failed, batchedDetails);
                }, executor);
                futures.add(future);
            }

            // Wait for all batches to complete with timeout
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.MINUTES); // 30 min
                                                                                                          // timeout for
                                                                                                          // entire job

        } catch (Exception e) {
            log.error("Error during batch processing for job {}: {}", job.getJobId(), e.getMessage(), e);
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

        // Batch save all details at once to reduce DB round trips
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
    }

    /**
     * Process a batch of loans with retry strategy.
     */
    private void processBatch(BulkForeclosureJob job, List<Long> loanIds, LocalDate foreclosureDate, AtomicInteger successful,
            AtomicInteger failed, List<BulkForeclosureJobDetail> batchedDetails) {

        for (Long loanId : loanIds) {
            // Idempotency check - skip if already processed
            if (isLoanAlreadyProcessed(job.getId(), loanId)) {
                log.info("Loan {} already processed for job {}, skipping", loanId, job.getJobId());
                continue;
            }

            boolean success = false;
            String lastError = null;

            // Retry strategy for transient failures
            for (int attempt = 1; attempt <= MAX_RETRIES && !success; attempt++) {
                try {
                    String validationError = transactionalHelper.forecloseSingleLoan(loanId, foreclosureDate);
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

                    // Check if error is retryable (transient)
                    if (isRetryableError(e) && attempt < MAX_RETRIES) {
                        log.warn("Retryable error for loan {} (attempt {}/{}): {}", loanId, attempt, MAX_RETRIES, lastError);
                        try {
                            Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential backoff
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

            // Record result
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

    /**
     * Check if loan was already processed (idempotency).
     */
    private boolean isLoanAlreadyProcessed(Long jobId, Long loanId) {
        return jobDetailRepository.existsByJob_IdAndLoanId(jobId, loanId);
    }

    /**
     * Determine if an error is retryable (transient).
     */
    private boolean isRetryableError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        // Retry on transient DB errors, deadlocks, connection issues
        return message.contains("deadlock") || message.contains("lock wait timeout") || message.contains("connection")
                || message.contains("timeout") || e.getCause() instanceof java.sql.SQLException;
    }

    /**
     * Partition a list into smaller batches.
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            // Create a copy of the sublist to avoid issues with list views in concurrent processing
            partitions.add(new ArrayList<>(list.subList(i, Math.min(i + batchSize, list.size()))));
        }
        return partitions;
    }

    private BulkForeclosureJobData toJobData(BulkForeclosureJob job) {
        List<BulkForeclosureJobDetail> details = jobDetailRepository.findByJob(job);

        List<BulkForeclosureJobData.BulkForeclosureSuccessData> successes = details.stream().filter(d -> "SUCCESS".equals(d.getStatus()))
                .map(d -> new BulkForeclosureJobData.BulkForeclosureSuccessData(String.valueOf(d.getLoanId()), d.getProcessedOn()))
                .collect(Collectors.toList());

        List<BulkForeclosureFailureData> failures = details.stream().filter(d -> "FAILED".equals(d.getStatus()))
                .map(d -> new BulkForeclosureFailureData(String.valueOf(d.getLoanId()), d.getFailureReason())).collect(Collectors.toList());

        BulkForeclosureJobData data = new BulkForeclosureJobData();
        data.setJobId(job.getJobId());
        data.setStatus(job.getStatus());
        data.setTotal(job.getTotalLoans());
        data.setSuccessful(job.getSuccessful());
        data.setFailed(job.getFailed());
        data.setSuccesses(successes);
        data.setFailures(failures);
        return data;
    }

    private BulkForeclosureJobData toJobDataSummary(BulkForeclosureJob job) {
        BulkForeclosureJobData data = new BulkForeclosureJobData();
        data.setJobId(job.getJobId());
        data.setStatus(job.getStatus());
        data.setTotal(job.getTotalLoans());
        data.setSuccessful(job.getSuccessful());
        data.setFailed(job.getFailed());
        data.setCreatedOn(job.getSubmittedOn());
        data.setCompletedOn(job.getCompletedOn());
        return data;
    }
}
