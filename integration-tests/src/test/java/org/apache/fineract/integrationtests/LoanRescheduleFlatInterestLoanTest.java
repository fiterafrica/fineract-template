package org.apache.fineract.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.CollateralManagementHelper;
import org.apache.fineract.integrationtests.common.LoanRescheduleRequestHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanRescheduleRequestTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integration test to ensure flat interest amount remains constant after rescheduling. */
@SuppressWarnings({"rawtypes"})
public class LoanRescheduleFlatInterestLoanTest {

    private static final Logger LOG = LoggerFactory.getLogger(LoanRescheduleFlatInterestLoanTest.class);

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private LoanTransactionHelper loanTransactionHelper;
    private LoanRescheduleRequestHelper loanRescheduleRequestHelper;
    private Integer clientId;
    private Integer loanProductId;
    private Integer loanId;
    private Integer loanRescheduleRequestId;
    private final String loanPrincipalAmount = "10000.00";
    private final String numberOfRepayments = "4";
    private final String interestRatePerPeriod = "2";
    private final String dateString = "04 September 2014";

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
        this.loanRescheduleRequestHelper = new LoanRescheduleRequestHelper(this.requestSpec, this.responseSpec);
        createRequiredEntities();
    }

    private void createRequiredEntities() {
        createClient();
        createLoanProduct();
        createLoan();
    }

    private void createClient() {
        this.clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, this.clientId);
    }

    private void createLoanProduct() {
        LOG.info("Creating loan product");
        final String loanProductJSON = new LoanProductTestBuilder().withPrincipal(loanPrincipalAmount)
                .withNumberOfRepayments(numberOfRepayments).withinterestRatePerPeriod(interestRatePerPeriod)
                .withRepaymentAfterEvery("1").withRepaymentTypeAsMonth()
                .withInterestRateFrequencyTypeAsMonths().withInterestTypeAsFlat().withInterestCalculationPeriodTypeAsDays()
                .withAmortizationTypeAsEqualInstallments().build(null);
        this.loanProductId = this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }

    private void createLoan() {
        LOG.info("Creating loan");
        List<HashMap> collaterals = new ArrayList<>();
        final Integer collateralId = CollateralManagementHelper.createCollateralProduct(this.requestSpec, this.responseSpec);
        final Integer clientCollateralId = CollateralManagementHelper.createClientCollateral(this.requestSpec, this.responseSpec,
                this.clientId.toString(), collateralId);
        collaterals.add(CollateralManagementHelper.createClientCollateralMap(clientCollateralId, "1"));
        final String loanApplicationJSON = new LoanApplicationTestBuilder().withPrincipal(loanPrincipalAmount)
                .withLoanTermFrequency(numberOfRepayments).withLoanTermFrequencyAsMonths().withNumberOfRepayments(numberOfRepayments)
                .withRepaymentEveryAfter("1").withRepaymentFrequencyTypeAsMonths()
                .withInterestCalculationPeriodTypeAsDays().withInterestRatePerPeriod(interestRatePerPeriod)
                .withInterestTypeAsFlatBalance().withSubmittedOnDate(dateString).withExpectedDisbursementDate(dateString)
                .withCollaterals(collaterals).build(this.clientId.toString(), this.loanProductId.toString(), null);
        this.loanId = this.loanTransactionHelper.getLoanId(loanApplicationJSON);
        this.loanTransactionHelper.approveLoan(this.dateString, this.loanId);
        String loanDetails = this.loanTransactionHelper.getLoanDetails(this.requestSpec, this.responseSpec, this.loanId);
        this.loanTransactionHelper.disburseLoan(this.dateString, this.loanId,
                io.restassured.path.json.JsonPath.from(loanDetails).get("netDisbursalAmount").toString());
    }

    @Test
    public void testInterestRemainsConstantAfterReschedule() {
        ArrayList<HashMap> schedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, this.loanId);
        Float expectedInterest = (Float) schedule.get(1).get("interestOriginalDue");

        String requestJSON = new LoanRescheduleRequestTestBuilder().updateGraceOnPrincipal(null).updateGraceOnInterest(null)
                .updateRecalculateInterest(true).updateRescheduleFromDate("04 December 2014")
                .updateAdjustedDueDate("04 February 2015").build(this.loanId.toString());
        this.loanRescheduleRequestId = this.loanRescheduleRequestHelper.createLoanRescheduleRequest(requestJSON);
        String approveJson = new LoanRescheduleRequestTestBuilder().getApproveLoanRescheduleRequestJSON();
        this.loanRescheduleRequestHelper.approveLoanRescheduleRequest(this.loanRescheduleRequestId, approveJson);

        ArrayList<HashMap> newSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, this.loanId);
        for (int i = 1; i < newSchedule.size(); i++) {
            Float interest = (Float) newSchedule.get(i).get("interestOriginalDue");
            assertEquals(expectedInterest, interest, "Interest changed for period " + i);
        }
    }
}
