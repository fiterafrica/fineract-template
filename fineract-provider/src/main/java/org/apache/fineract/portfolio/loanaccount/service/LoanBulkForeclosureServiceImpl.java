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

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.data.BulkForeclosureJobData;
import org.apache.fineract.portfolio.loanaccount.data.BulkForeclosureJobData.BulkForeclosureFailureData;
import org.apache.fineract.portfolio.loanaccount.data.BulkForeclosureRetryResultData;
import org.apache.fineract.portfolio.loanaccount.domain.BulkForeclosureJob;
import org.apache.fineract.portfolio.loanaccount.domain.BulkForeclosureJobDetail;
import org.apache.fineract.portfolio.loanaccount.domain.BulkForeclosureJobDetailRepository;
import org.apache.fineract.portfolio.loanaccount.domain.BulkForeclosureJobRepository;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final AppUserRepository appUserRepository;

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
                    batchedDetails.add(BulkForeclosureJobDetail.success(job, loanId, loanAccountNo, clientName));
                } else {
                    failed.incrementAndGet();
                    batchedDetails.add(BulkForeclosureJobDetail.failure(job, loanId, loanAccountNo, clientName, lastError));
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

        BigDecimal totalPayoff = BigDecimal.ZERO;

        List<BulkForeclosureJobData.BulkForeclosureSuccessData> successes = new ArrayList<>();
        for (BulkForeclosureJobDetail d : details) {
            if (!"SUCCESS".equals(d.getStatus())) {
                continue;
            }

            String loanAccountNo = d.getLoanAccountNo();
            String clientName = d.getClientName();
            BigDecimal payoffAmount = BigDecimal.ZERO;

            // Fetch loan details and payoff amount from loan
            try {
                Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(d.getLoanId());
                if (isNullOrEmpty(loanAccountNo)) {
                    loanAccountNo = loan.getAccountNumber();
                }
                if (isNullOrEmpty(clientName)) {
                    if (loan.getClient() != null) {
                        clientName = loan.getClient().getDisplayName();
                    } else if (loan.getGroup() != null) {
                        clientName = loan.getGroup().getName();
                    }
                }
                // Get payoff amount: the foreclosure transaction amount (last repayment that closed the loan)
                payoffAmount = getForeclosureTransactionAmount(loan);
            } catch (Exception e) {
                log.warn("Could not fetch loan details for loan {}: {}", d.getLoanId(), e.getMessage());
            }

            totalPayoff = totalPayoff.add(payoffAmount);
            successes.add(new BulkForeclosureJobData.BulkForeclosureSuccessData(String.valueOf(d.getLoanId()), loanAccountNo, clientName,
                    payoffAmount, d.getProcessedOn()));
        }

        List<BulkForeclosureFailureData> failures = details.stream().filter(d -> "FAILED".equals(d.getStatus())).map(d -> {
            String loanAccountNo = d.getLoanAccountNo();
            String clientName = d.getClientName();

            // If loan account number or client name is missing, fetch from loan
            if (isNullOrEmpty(loanAccountNo) || isNullOrEmpty(clientName)) {
                try {
                    Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(d.getLoanId());
                    if (isNullOrEmpty(loanAccountNo)) {
                        loanAccountNo = loan.getAccountNumber();
                    }
                    if (isNullOrEmpty(clientName)) {
                        if (loan.getClient() != null) {
                            clientName = loan.getClient().getDisplayName();
                        } else if (loan.getGroup() != null) {
                            clientName = loan.getGroup().getName();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch loan details for loan {}: {}", d.getLoanId(), e.getMessage());
                }
            }

            return new BulkForeclosureFailureData(d.getId(), String.valueOf(d.getLoanId()), loanAccountNo, clientName, d.getFailureReason(),
                    d.getRetryCount());
        }).collect(Collectors.toList());

        String submittedByUserName = null;
        if (job.getSubmittedByUserId() != null) {
            submittedByUserName = appUserRepository.findById(job.getSubmittedByUserId()).map(AppUser::getUsername).orElse(null);
        }

        BulkForeclosureJobData data = new BulkForeclosureJobData();
        data.setJobId(job.getJobId());
        data.setStatus(job.getStatus());
        data.setTotal(job.getTotalLoans());
        data.setSuccessful(job.getSuccessful());
        data.setFailed(job.getFailed());
        data.setCreatedOn(job.getSubmittedOn());
        data.setCompletedOn(job.getCompletedOn());
        data.setSubmittedByUserId(job.getSubmittedByUserId());
        data.setSubmittedByUserName(submittedByUserName);
        data.setForeclosureDate(job.getForeclosureDate());
        data.setTotalPayoff(totalPayoff);
        data.setSuccesses(successes);
        data.setFailures(failures);
        return data;
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Get the foreclosure transaction amount from a loan. The foreclosure payment is the last non-reversed repayment
     * transaction that closed the loan (principal + interest + fees + penalties paid at foreclosure time).
     */
    private BigDecimal getForeclosureTransactionAmount(Loan loan) {
        if (loan.getLoanTransactions() == null || loan.getLoanTransactions().isEmpty()) {
            return BigDecimal.ZERO;
        }
        // Find the last non-reversed repayment transaction (the foreclosure payment)
        return loan.getLoanTransactions().stream().filter(t -> !t.isReversed() && t.isRepayment()).reduce((first, second) -> second) // get
                                                                                                                                     // last
                .map(t -> t.getAmount(loan.getCurrency()).getAmount()).orElse(BigDecimal.ZERO);
    }

    private BulkForeclosureJobData toJobDataSummary(BulkForeclosureJob job) {
        String submittedByUserName = null;
        if (job.getSubmittedByUserId() != null) {
            submittedByUserName = appUserRepository.findById(job.getSubmittedByUserId()).map(AppUser::getUsername).orElse(null);
        }

        BulkForeclosureJobData data = new BulkForeclosureJobData();
        data.setJobId(job.getJobId());
        data.setStatus(job.getStatus());
        data.setTotal(job.getTotalLoans());
        data.setSuccessful(job.getSuccessful());
        data.setFailed(job.getFailed());
        data.setCreatedOn(job.getSubmittedOn());
        data.setCompletedOn(job.getCompletedOn());
        data.setSubmittedByUserId(job.getSubmittedByUserId());
        data.setSubmittedByUserName(submittedByUserName);
        data.setForeclosureDate(job.getForeclosureDate());
        return data;
    }

    @Override
    public Response downloadJobReport(String jobId, String reportType) {
        this.context.authenticatedUser();

        final BulkForeclosureJob job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Bulk foreclosure job not found: " + jobId));

        List<BulkForeclosureJobDetail> details = jobDetailRepository.findByJob(job);

        // Filter based on report type
        List<BulkForeclosureJobDetail> filteredDetails;
        String sheetName;
        String fileName;

        if ("failed".equalsIgnoreCase(reportType)) {
            filteredDetails = details.stream().filter(d -> "FAILED".equals(d.getStatus())).collect(Collectors.toList());
            sheetName = "Failed Loans";
            fileName = "bulk_foreclosure_failed_" + jobId + ".xlsx";
        } else if ("success".equalsIgnoreCase(reportType)) {
            filteredDetails = details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).collect(Collectors.toList());
            sheetName = "Successful Loans";
            fileName = "bulk_foreclosure_success_" + jobId + ".xlsx";
        } else {
            filteredDetails = details;
            sheetName = "All Loans";
            fileName = "bulk_foreclosure_report_" + jobId + ".xlsx";
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = { "Loan ID", "Loan Account No", "Client Name", "Status", "Total Payoff", "Failure Reason", "Processed On" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            BigDecimal grandTotalPayoff = BigDecimal.ZERO;
            int rowNum = 1;
            for (BulkForeclosureJobDetail detail : filteredDetails) {
                String loanAccountNo = detail.getLoanAccountNo();
                String clientName = detail.getClientName();
                BigDecimal payoffAmount = BigDecimal.ZERO;

                // Fetch loan details
                try {
                    Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(detail.getLoanId());
                    if (isNullOrEmpty(loanAccountNo)) {
                        loanAccountNo = loan.getAccountNumber();
                    }
                    if (isNullOrEmpty(clientName)) {
                        if (loan.getClient() != null) {
                            clientName = loan.getClient().getDisplayName();
                        } else if (loan.getGroup() != null) {
                            clientName = loan.getGroup().getName();
                        }
                    }
                    // Get payoff amount: the foreclosure transaction amount
                    if ("SUCCESS".equals(detail.getStatus())) {
                        payoffAmount = getForeclosureTransactionAmount(loan);
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch loan details for loan {}: {}", detail.getLoanId(), e.getMessage());
                }

                if ("SUCCESS".equals(detail.getStatus())) {
                    grandTotalPayoff = grandTotalPayoff.add(payoffAmount);
                }

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(detail.getLoanId());
                row.createCell(1).setCellValue(loanAccountNo != null ? loanAccountNo : "");
                row.createCell(2).setCellValue(clientName != null ? clientName : "");
                row.createCell(3).setCellValue(detail.getStatus());
                row.createCell(4).setCellValue("SUCCESS".equals(detail.getStatus()) ? payoffAmount.doubleValue() : 0);
                row.createCell(5).setCellValue(detail.getFailureReason() != null ? detail.getFailureReason() : "");
                row.createCell(6).setCellValue(detail.getProcessedOn() != null ? detail.getProcessedOn().format(dateFormatter) : "");
            }

            // Add summary sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            Row summaryRow1 = summarySheet.createRow(0);
            summaryRow1.createCell(0).setCellValue("Job ID:");
            summaryRow1.createCell(1).setCellValue(job.getJobId());

            Row summaryRow2 = summarySheet.createRow(1);
            summaryRow2.createCell(0).setCellValue("Status:");
            summaryRow2.createCell(1).setCellValue(job.getStatus());

            Row summaryRow3 = summarySheet.createRow(2);
            summaryRow3.createCell(0).setCellValue("Foreclosure Date:");
            summaryRow3.createCell(1).setCellValue(job.getForeclosureDate() != null ? job.getForeclosureDate().toString() : "");

            Row summaryRow4 = summarySheet.createRow(3);
            summaryRow4.createCell(0).setCellValue("Total Loans:");
            summaryRow4.createCell(1).setCellValue(job.getTotalLoans());

            Row summaryRow5 = summarySheet.createRow(4);
            summaryRow5.createCell(0).setCellValue("Successful:");
            summaryRow5.createCell(1).setCellValue(job.getSuccessful());

            Row summaryRow6 = summarySheet.createRow(5);
            summaryRow6.createCell(0).setCellValue("Failed:");
            summaryRow6.createCell(1).setCellValue(job.getFailed());

            Row summaryRow7 = summarySheet.createRow(6);
            summaryRow7.createCell(0).setCellValue("Submitted On:");
            summaryRow7.createCell(1).setCellValue(job.getSubmittedOn() != null ? job.getSubmittedOn().format(dateFormatter) : "");

            Row summaryRow8 = summarySheet.createRow(7);
            summaryRow8.createCell(0).setCellValue("Completed On:");
            summaryRow8.createCell(1).setCellValue(job.getCompletedOn() != null ? job.getCompletedOn().format(dateFormatter) : "");

            String submittedByUserName = null;
            if (job.getSubmittedByUserId() != null) {
                submittedByUserName = appUserRepository.findById(job.getSubmittedByUserId()).map(AppUser::getUsername).orElse(null);
            }
            Row summaryRow9 = summarySheet.createRow(8);
            summaryRow9.createCell(0).setCellValue("Submitted By:");
            summaryRow9.createCell(1).setCellValue(submittedByUserName != null ? submittedByUserName : "");

            Row summaryRow10 = summarySheet.createRow(9);
            summaryRow10.createCell(0).setCellValue("Total Payoff:");
            summaryRow10.createCell(1).setCellValue(grandTotalPayoff.doubleValue());

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            return Response.ok(outputStream.toByteArray()).type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"").build();
        } catch (Exception e) {
            log.error("Error generating Excel report for job {}: {}", jobId, e.getMessage(), e);
            throw new IllegalStateException("Failed to generate Excel report: " + e.getMessage());
        }
    }

    @Override
    public BulkForeclosureRetryResultData retryFailedRecords(String jobId, List<Long> detailIds, String executionMode) {
        final Long userId = this.context.authenticatedUser().getId();

        // Find the job
        final BulkForeclosureJob job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Bulk foreclosure job not found: " + jobId));

        // Job must be in COMPLETED or FAILED status to retry
        if (!"COMPLETED".equals(job.getStatus()) && !"FAILED".equals(job.getStatus())) {
            throw new IllegalStateException(
                    "Cannot retry records for job in status: " + job.getStatus() + ". Job must be COMPLETED or FAILED.");
        }

        // Find the failed detail records to retry
        List<BulkForeclosureJobDetail> failedDetails;
        if (detailIds == null || detailIds.isEmpty()) {
            // If no specific IDs provided, retry all failed records
            failedDetails = jobDetailRepository.findAllFailedByJob(job.getId());
        } else {
            // Find only the specified failed records
            failedDetails = jobDetailRepository.findFailedByIdsAndJob(detailIds, job.getId());
        }

        if (failedDetails.isEmpty()) {
            throw new IllegalArgumentException("No failed records found to retry for job: " + jobId);
        }

        log.info("Retrying {} failed records for job {}", failedDetails.size(), jobId);

        final String finalExecutionMode = failedDetails.size() > 50 ? "ASYNC" : (executionMode != null ? executionMode : "SYNC");

        if ("ASYNC".equalsIgnoreCase(finalExecutionMode)) {
            // Capture context for async processing
            final FineractContext context = ThreadLocalContextUtil.getContext();
            asyncExecutor.retryFailedRecordsAsync(job, failedDetails, context);

            // Return pending status for async
            BulkForeclosureRetryResultData result = BulkForeclosureRetryResultData.pending(jobId, failedDetails.size(), userId);
            result.setStatus("PENDING");
            return result;
        } else {
            // Synchronous retry
            return processRetrySync(job, failedDetails, userId);
        }
    }

    /**
     * Process retry synchronously.
     */
    private BulkForeclosureRetryResultData processRetrySync(BulkForeclosureJob job, List<BulkForeclosureJobDetail> failedDetails,
            Long userId) {
        List<BulkForeclosureRetryResultData.RetryDetailResult> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;

        for (BulkForeclosureJobDetail detail : failedDetails) {
            if (!detail.canRetry()) {
                log.warn("Detail {} cannot be retried (status: {})", detail.getId(), detail.getStatus());
                continue;
            }

            String previousStatus = detail.getStatus();
            String newStatus;
            String failureReason = null;

            try {
                String validationError = transactionalHelper.forecloseSingleLoan(detail.getLoanId(), job.getForeclosureDate());
                if (validationError != null) {
                    newStatus = "FAILED";
                    failureReason = validationError;
                    detail.markAsRetryFailed(validationError);
                    failed++;
                    log.warn("Retry failed for loan {} (detail {}): {}", detail.getLoanId(), detail.getId(), validationError);
                } else {
                    newStatus = "SUCCESS";
                    detail.markAsRetrySuccess();
                    successful++;
                    log.info("Retry successful for loan {} (detail {})", detail.getLoanId(), detail.getId());
                }
            } catch (Exception e) {
                newStatus = "FAILED";
                failureReason = e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                        : "Unknown error";
                detail.markAsRetryFailed(failureReason);
                failed++;
                log.error("Retry error for loan {} (detail {}): {}", detail.getLoanId(), detail.getId(), failureReason, e);
            }

            // Save the updated detail
            jobDetailRepository.save(detail);

            // Build result entry
            BulkForeclosureRetryResultData.RetryDetailResult retryResult = new BulkForeclosureRetryResultData.RetryDetailResult();
            retryResult.setDetailId(detail.getId());
            retryResult.setLoanId(detail.getLoanId());
            retryResult.setLoanAccountNo(detail.getLoanAccountNo());
            retryResult.setClientName(detail.getClientName());
            retryResult.setPreviousStatus(previousStatus);
            retryResult.setNewStatus(newStatus);
            retryResult.setFailureReason(failureReason);
            retryResult.setRetryCount(detail.getRetryCount());
            results.add(retryResult);
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
        jobRepository.save(job);

        // Get user name
        String userName = null;
        if (userId != null) {
            userName = appUserRepository.findById(userId).map(AppUser::getUsername).orElse(null);
        }

        // Build and return result
        BulkForeclosureRetryResultData result = new BulkForeclosureRetryResultData();
        result.setJobId(job.getJobId());
        result.setStatus("COMPLETED");
        result.setTotalRetried(failedDetails.size());
        result.setSuccessful(successful);
        result.setFailed(failed);
        result.setRetriedOn(LocalDateTime.now(DateUtils.getDateTimeZoneOfTenant()));
        result.setRetriedByUserId(userId);
        result.setRetriedByUserName(userName);
        result.setResults(results);

        log.info("Retry completed for job {}: {} successful, {} failed out of {} total", job.getJobId(), successful, failed,
                failedDetails.size());

        return result;
    }
}
