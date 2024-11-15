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
package org.apache.fineract.infrastructure.dataqueries.data;


import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;



@Data
public class LoanPortfolioRowData {
    @SerializedName("Loan Number")
    private String loanNumber;
    @SerializedName("Loan Officer")
    private String loanOfficer;
    @SerializedName("Client Name")
    private String clientName;
    @SerializedName("Client UUID")
    private String clientUUID;
    @SerializedName("Loan Product")
    private String loanProduct;
    @SerializedName("Purpose")
    private String purpose;
    @SerializedName("Department")
    private String department;
    @SerializedName("Strata")
    private String strata;
    @SerializedName("KIVA Loan ID")
    private String kivaLoanId;
    @SerializedName("Funder")
    private String funder;
    @SerializedName("Client ID")
    private String clientId;
    @SerializedName("Date of Birth")
    private LocalDate dateOfBirth;
    @SerializedName("KIVA Client ID")
    private String kivaClientId;
    @SerializedName("Cycle")
    private long cycle;
    @SerializedName("Cohort")
    private String cohort;
    @SerializedName("Gender")
    private String gender;
    @SerializedName("Province")
    private String province;
    @SerializedName("Sector")
    private String sector;
    @SerializedName("Cell")
    private String cell;
    @SerializedName("District")
    private String district;
    @SerializedName("Nationality")
    private String nationality;
    @SerializedName("Telephone")
    private String telephone;
    @SerializedName("Mobile No")
    private String mobileNo;
    @SerializedName("Submission Date ")
    private LocalDate submissionDate;
    @SerializedName("Approval Date")
    private LocalDate approvalDate;
    @SerializedName("Disbursement Date")
    private LocalDate disbursementDate;
    @SerializedName("Applied Amount")
    private BigDecimal appliedAmount;
    @SerializedName("Approved Amount")
    private BigDecimal approvedAmount;
    @SerializedName("Disbursed Amount")
    private BigDecimal disbursedAmount;
    @SerializedName("Difference")
    private BigDecimal difference;
    @SerializedName("Currency Type")
    private String currencyType;
    @SerializedName("Re-payment Term")
    private String repaymentTerm;
    @SerializedName("Loan Type")
    private String loanType;
    @SerializedName("Terms Duration")
    private short termsDuration;
    @SerializedName("Actual Payment Amount")
    private BigDecimal actualPaymentAmount;
    @SerializedName("Principal Paid")
    private BigDecimal principalPaid;
    @SerializedName("Interest Paid")
    private BigDecimal interestPaid;
    @SerializedName("Insurance fee Paid")
    private BigDecimal insuranceFeePaid;
    @SerializedName("Total Late Fees Paid")
    private BigDecimal totalLateFeesPaid;
    @SerializedName("Excess Amount Paid")
    private BigDecimal excessAmountPaid;
    @SerializedName("Current Balance")
    private BigDecimal currentBalance;
    @SerializedName("Principal Balance")
    private BigDecimal principalBalance;
    @SerializedName("Interest Balance")
    private BigDecimal interestBalance;
    @SerializedName("Fees Balance")
    private int feesBalance;
    @SerializedName("Amount Past Due")
    private BigDecimal amountPastDue;
    @SerializedName("Principal Past Due")
    private BigDecimal principalPastDue;
    @SerializedName("Interest Past Due")
    private BigDecimal interestPastDue;
    @SerializedName("Fees Past Due")
    private BigDecimal feesPastDue;
    @SerializedName("Scheduled Principal Amount")
    private BigDecimal scheduledPrincipalAmount;
    @SerializedName("Scheduled Interest Amount")
    private BigDecimal scheduledInterestAmount;
    @SerializedName("Scheduled Fees Amount")
    private BigDecimal scheduledFeesAmount;
    @SerializedName("Scheduled Payment Amount")
    private BigDecimal scheduledPaymentAmount;
    @SerializedName("Last Payment Amount")
    private BigDecimal lastPaymentAmount;
    @SerializedName("Last Principal Amount")
    private BigDecimal lastPrincipalAmount;
    @SerializedName("Last Interest Amount")
    private BigDecimal lastInterestAmount;
    @SerializedName("Last Fees Amount")
    private BigDecimal lastFeesAmount;
    @SerializedName("Last Late Fees Amount")
    private BigDecimal lastLateFeesAmount;
    @SerializedName("Last Excess Amount")
    private BigDecimal lastExcessAmount;
    @SerializedName("Days in Arrears")
    private int daysInArrears;
    @SerializedName("Installment in Arrears")
    private long installmentInArrears;
    @SerializedName("Last Payment Date")
    private LocalDate lastPaymentDate;
    @SerializedName("Next Payment Due")
    private LocalDate nextPaymentDue;
    @SerializedName("Final Payment Date")
    private LocalDate finalPaymentDate;
    @SerializedName("Date Closed")
    private LocalDate dateClosed;
    @SerializedName("Loan Status")
    private String loanStatus;
    @SerializedName("Business Description")
    private String businessDescription;
    @SerializedName("Industry/Sector of Activity")
    private String industrySectorOfActivity;
    @SerializedName("Business Sub-Sector")
    private String businessSubSector;
    @SerializedName("Loan Created On")
    private String createdOnUtc;
    @SerializedName("Loan Last Modified on")
    private String lastModifiedUtc;
}
