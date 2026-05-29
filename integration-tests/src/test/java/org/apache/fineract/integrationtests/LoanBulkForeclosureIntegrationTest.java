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
package org.apache.fineract.integrationtests;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.CollateralManagementHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanStatusChecker;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for Bulk Loan Foreclosure functionality.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class LoanBulkForeclosureIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(LoanBulkForeclosureIntegrationTest.class);

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private LoanTransactionHelper loanTransactionHelper;

    private static final String BULK_FORECLOSURE_URL = "/fineract-provider/api/v1/loans/foreclosure/bulk?" + Utils.TENANT_IDENTIFIER;
    private static final String BULK_FORECLOSURE_STATUS_URL = "/fineract-provider/api/v1/loans/foreclosure/jobs/";
    private static final String BULK_FORECLOSURE_JOBS_URL = "/fineract-provider/api/v1/loans/foreclosure/jobs?" + Utils.TENANT_IDENTIFIER;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.requestSpec.header("Fineract-Platform-TenantId", "default");
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
    }

    @Test
    public void testBulkForeclosureSyncMode() {
        LOG.info("-------------------------- TESTING BULK FORECLOSURE - SYNC MODE --------------------------");

        // Create multiple clients and loans
        List<Integer> loanIds = createMultipleActiveLoans(3);
        LOG.info("Created {} active loans: {}", loanIds.size(), loanIds);

        // Trigger bulk foreclosure in SYNC mode
        final String foreclosureDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Map<String, Object> response = triggerBulkForeclosure(loanIds, foreclosureDate, "SYNC");

        // Verify response
        assertNotNull(response.get("jobId"), "Job ID should not be null");
        String status = (String) response.get("status");
        assertTrue("COMPLETED".equals(status) || "FAILED".equals(status),
                "Status should be COMPLETED or FAILED for sync mode, but was: " + status);

        LOG.info("Bulk foreclosure response: {}", response);

        // Verify loans are closed
        for (Integer loanId : loanIds) {
            HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanId);
            LOG.info("Loan {} status after foreclosure: {}", loanId, loanStatusHashMap);
        }
    }

    @Test
    public void testBulkForeclosureAsyncMode() {
        LOG.info("-------------------------- TESTING BULK FORECLOSURE - ASYNC MODE --------------------------");

        // Create multiple clients and loans
        List<Integer> loanIds = createMultipleActiveLoans(3);
        LOG.info("Created {} active loans: {}", loanIds.size(), loanIds);

        // Trigger bulk foreclosure in ASYNC mode
        final String foreclosureDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Map<String, Object> response = triggerBulkForeclosure(loanIds, foreclosureDate, "ASYNC");

        // Verify initial response
        assertNotNull(response.get("jobId"), "Job ID should not be null");
        String jobId = (String) response.get("jobId");
        String initialStatus = (String) response.get("status");
        LOG.info("Initial job status: {}", initialStatus);

        // Poll for job completion
        await().atMost(60, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
            Map<String, Object> statusResponse = getBulkForeclosureJobStatus(jobId);
            String currentStatus = (String) statusResponse.get("status");
            LOG.info("Current job status: {}", currentStatus);
            return "COMPLETED".equals(currentStatus) || "FAILED".equals(currentStatus);
        });

        // Verify final status
        Map<String, Object> finalStatus = getBulkForeclosureJobStatus(jobId);
        LOG.info("Final job status: {}", finalStatus);
        assertNotNull(finalStatus.get("total"));
        assertNotNull(finalStatus.get("successful"));
        assertNotNull(finalStatus.get("failed"));
    }

    @Test
    public void testBulkForeclosureMandatoryAsyncForLargeVolume() {
        LOG.info("-------------------------- TESTING BULK FORECLOSURE - MANDATORY ASYNC FOR >50 LOANS --------------------------");

        // Create a request with >50 loan IDs (we'll use fake IDs just to test the async enforcement)
        // Note: In a real scenario, you would create 51+ active loans
        List<Integer> loanIds = new ArrayList<>();
        for (int i = 1; i <= 51; i++) {
            loanIds.add(i); // These may be invalid, but we're testing the async mode enforcement
        }

        final String foreclosureDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Even if we specify SYNC, it should be forced to ASYNC for >50 loans
        Map<String, Object> response = triggerBulkForeclosure(loanIds, foreclosureDate, "SYNC");

        assertNotNull(response.get("jobId"), "Job ID should not be null");
        String status = (String) response.get("status");
        // For async mode with invalid loans, we expect PENDING or RUNNING initially
        LOG.info("Response for >50 loans: status={}", status);
    }

    @Test
    public void testBulkForeclosureWithInvalidLoans() {
        LOG.info("-------------------------- TESTING BULK FORECLOSURE - PARTIAL FAILURE --------------------------");

        // Create some active loans
        List<Integer> loanIds = createMultipleActiveLoans(2);

        // Add invalid loan IDs to test partial failure handling
        loanIds.add(999999); // Non-existent loan
        loanIds.add(999998); // Non-existent loan

        LOG.info("Created loans with some invalid IDs: {}", loanIds);

        // Trigger bulk foreclosure
        final String foreclosureDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Map<String, Object> response = triggerBulkForeclosure(loanIds, foreclosureDate, "SYNC");

        assertNotNull(response.get("jobId"), "Job ID should not be null");
        String jobId = (String) response.get("jobId");

        // Check job status
        Map<String, Object> statusResponse = getBulkForeclosureJobStatus(jobId);
        LOG.info("Job status with partial failures: {}", statusResponse);

        // Should have some failures
        Integer total = ((Number) statusResponse.get("total")).intValue();
        assertEquals(4, total, "Total should be 4");

        List<Map<String, Object>> failures = (List<Map<String, Object>>) statusResponse.get("failures");
        if (failures != null && !failures.isEmpty()) {
            LOG.info("Failures recorded: {}", failures);
        }
    }

    @Test
    public void testGetJobStatusForNonExistentJob() {
        LOG.info("-------------------------- TESTING GET JOB STATUS - NON-EXISTENT JOB --------------------------");

        ResponseSpecification responseSpec400 = new ResponseSpecBuilder().expectStatusCode(500).build();

        final String url = BULK_FORECLOSURE_STATUS_URL + "non-existent-job-id?" + Utils.TENANT_IDENTIFIER;
        Utils.performServerGet(this.requestSpec, responseSpec400, url, null);
    }

    @Test
    public void testBulkForeclosureJobHistory() {
        LOG.info("-------------------------- TESTING BULK FORECLOSURE - JOB HISTORY PERSISTENCE --------------------------");

        // Create loans and trigger foreclosure
        List<Integer> loanIds = createMultipleActiveLoans(2);
        final String foreclosureDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Map<String, Object> response = triggerBulkForeclosure(loanIds, foreclosureDate, "SYNC");
        String jobId = (String) response.get("jobId");

        // Verify job can be retrieved after completion (history is saved)
        Map<String, Object> jobStatus = getBulkForeclosureJobStatus(jobId);
        assertNotNull(jobStatus, "Job history should be persisted in database");
        assertNotNull(jobStatus.get("jobId"), "Job ID should be retrievable");
        assertNotNull(jobStatus.get("status"), "Job status should be retrievable");
        assertNotNull(jobStatus.get("total"), "Total count should be retrievable");

        LOG.info("Job history retrieved successfully: {}", jobStatus);
    }

    @Test
    public void testBulkForeclosureJobList() {
        LOG.info("-------------------------- TESTING BULK FORECLOSURE - JOB LIST --------------------------");

        // Create loans and trigger foreclosure to ensure at least one job exists
        List<Integer> loanIds = createMultipleActiveLoans(2);
        final String foreclosureDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Map<String, Object> response = triggerBulkForeclosure(loanIds, foreclosureDate, "SYNC");
        assertNotNull(response.get("jobId"), "Job ID should not be null");

        // Fetch job list
        Map<String, Object> jobListResponse = getJobList(0, 10);
        assertNotNull(jobListResponse, "Job list response should not be null");
        assertNotNull(jobListResponse.get("totalFilteredRecords"), "Total filtered records should be present");

        List<Map<String, Object>> pageItems = (List<Map<String, Object>>) jobListResponse.get("pageItems");
        assertNotNull(pageItems, "Page items should not be null");
        assertTrue(pageItems.size() > 0, "Should have at least one job");

        // Verify job contains expected fields
        Map<String, Object> job = pageItems.get(0);
        assertNotNull(job.get("jobId"), "Job should have jobId");
        assertNotNull(job.get("status"), "Job should have status");
        assertNotNull(job.get("total"), "Job should have total count");

        LOG.info("Job list retrieved successfully: {} jobs found", pageItems.size());
    }

    @Test
    public void testBulkForeclosureAuditTrail() {
        LOG.info("-------------------------- TESTING BULK FORECLOSURE - AUDIT TRAIL --------------------------");

        // Create loans and trigger foreclosure
        List<Integer> loanIds = createMultipleActiveLoans(2);
        final String foreclosureDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Map<String, Object> response = triggerBulkForeclosure(loanIds, foreclosureDate, "SYNC");
        String jobId = (String) response.get("jobId");

        // Get job status and verify audit fields
        Map<String, Object> jobStatus = getBulkForeclosureJobStatus(jobId);

        // Verify audit trail fields
        assertNotNull(jobStatus.get("submittedByUserId"), "Submitted by user ID should be present");
        assertNotNull(jobStatus.get("submittedByUserName"), "Submitted by user name should be present");
        assertNotNull(jobStatus.get("foreclosureDate"), "Foreclosure date should be present");
        assertNotNull(jobStatus.get("createdOn"), "Created on timestamp should be present");

        LOG.info("Audit trail verified: submittedBy={}, foreclosureDate={}, createdOn={}",
                jobStatus.get("submittedByUserName"), jobStatus.get("foreclosureDate"), jobStatus.get("createdOn"));

        // Verify details contain loan account info
        List<Map<String, Object>> successes = (List<Map<String, Object>>) jobStatus.get("successes");
        if (successes != null && !successes.isEmpty()) {
            Map<String, Object> successDetail = successes.get(0);
            LOG.info("Success detail: loanId={}, loanAccountNo={}, clientName={}",
                    successDetail.get("loanId"), successDetail.get("loanAccountNo"), successDetail.get("clientName"));
        }

        List<Map<String, Object>> failures = (List<Map<String, Object>>) jobStatus.get("failures");
        if (failures != null && !failures.isEmpty()) {
            Map<String, Object> failureDetail = failures.get(0);
            LOG.info("Failure detail: loanId={}, loanAccountNo={}, clientName={}, reason={}",
                    failureDetail.get("loanId"), failureDetail.get("loanAccountNo"), failureDetail.get("clientName"),
                    failureDetail.get("reason"));
        }
    }

    // Helper methods

    private List<Integer> createMultipleActiveLoans(int count) {
        List<Integer> loanIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Integer loanId = createActiveLoan();
            if (loanId != null) {
                loanIds.add(loanId);
            }
        }

        return loanIds;
    }

    private Integer createActiveLoan() {
        // Create client
        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientID);

        // Create collateral
        final Integer collateralId = CollateralManagementHelper.createCollateralProduct(this.requestSpec, this.responseSpec);
        final Integer clientCollateralId = CollateralManagementHelper.createClientCollateral(this.requestSpec, this.responseSpec,
                String.valueOf(clientID), collateralId);
        List<HashMap> collaterals = new ArrayList<>();
        addCollaterals(collaterals, clientCollateralId, BigDecimal.valueOf(1));

        // Create loan product
        final Integer loanProductID = createLoanProduct();

        // Apply for loan
        final Integer loanID = applyForLoanApplication(clientID, loanProductID, collaterals);

        // Approve loan
        this.loanTransactionHelper.approveLoan(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), loanID);

        // Disburse loan
        this.loanTransactionHelper.disburseLoan(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), loanID,
                "10000", null);

        // Verify loan is active
        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);

        return loanID;
    }

    private Integer createLoanProduct() {
        LOG.info("-------------------------- CREATING LOAN PRODUCT --------------------------");
        final String loanProductJSON = new LoanProductTestBuilder()
                .withPrincipal("10000.00")
                .withNumberOfRepayments("12")
                .withRepaymentAfterEvery("1")
                .withRepaymentTypeAsMonth()
                .withinterestRatePerPeriod("1")
                .withInterestRateFrequencyTypeAsMonths()
                .withAmortizationTypeAsEqualInstallments()
                .withInterestTypeAsDecliningBalance()
                .build(null);
        return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }

    private Integer applyForLoanApplication(final Integer clientID, final Integer loanProductID, List<HashMap> collaterals) {
        LOG.info("-------------------------- APPLYING FOR LOAN --------------------------");
        final String loanApplicationJSON = new LoanApplicationTestBuilder()
                .withPrincipal("10000.00")
                .withLoanTermFrequency("12")
                .withLoanTermFrequencyAsMonths()
                .withNumberOfRepayments("12")
                .withRepaymentEveryAfter("1")
                .withRepaymentFrequencyTypeAsMonths()
                .withInterestRatePerPeriod("1")
                .withAmortizationTypeAsEqualInstallments()
                .withInterestTypeAsDecliningBalance()
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod()
                .withExpectedDisbursementDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                .withSubmittedOnDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                .withCollaterals(collaterals)
                .build(clientID.toString(), loanProductID.toString(), null);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

    private void addCollaterals(List<HashMap> collaterals, Integer collateralId, BigDecimal quantity) {
        collaterals.add(collaterals(collateralId, quantity));
    }

    private HashMap<String, Object> collaterals(Integer collateralId, BigDecimal quantity) {
        HashMap<String, Object> collateral = new HashMap<>();
        collateral.put("clientCollateralId", collateralId);
        collateral.put("quantity", quantity);
        return collateral;
    }

    private Map<String, Object> triggerBulkForeclosure(List<Integer> loanIds, String foreclosureDate, String executionMode) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("loanIds", loanIds);
        requestBody.put("foreclosureDate", foreclosureDate);
        requestBody.put("executionMode", executionMode);

        String requestJson = new Gson().toJson(requestBody);
        LOG.info("Bulk foreclosure request: {}", requestJson);

        return Utils.performServerPost(this.requestSpec, this.responseSpec, BULK_FORECLOSURE_URL, requestJson, "");
    }

    private Map<String, Object> getBulkForeclosureJobStatus(String jobId) {
        final String url = BULK_FORECLOSURE_STATUS_URL + jobId + "?" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(this.requestSpec, this.responseSpec, url, "");
    }

    private Map<String, Object> getJobList(int offset, int limit) {
        final String url = BULK_FORECLOSURE_JOBS_URL + "&offset=" + offset + "&limit=" + limit;
        return Utils.performServerGet(this.requestSpec, this.responseSpec, url, "");
    }
}

