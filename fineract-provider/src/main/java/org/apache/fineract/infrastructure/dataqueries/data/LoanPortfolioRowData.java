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
    //0
    @SerializedName("ID")
    private String id;
    //1
    @SerializedName("Loan Number")
    private String loanNumber;
    //2
    @SerializedName("Loan Officer")
    private String loanOfficer;
    //3
    @SerializedName("Client Name")
    private String clientName;
    //4
    @SerializedName("Client UUID")
    private String clientUUID;
    //5
    @SerializedName("Loan Product")
    private String loanProduct;
    //6
    @SerializedName("Purpose")
    private String purpose;
    //7
    @SerializedName("Department")
    private String department;
    //8
    @SerializedName("Strata")
    private String strata;
    //9
    @SerializedName("KIVA Loan ID")
    private String kivaLoanId;
    //10
    @SerializedName("Funder")
    private String funder;
    //11
    @SerializedName("Client ID")
    private String clientId;
    //12
    @SerializedName("Date of Birth")
    private LocalDate dateOfBirth;
    //13
    @SerializedName("KIVA Client ID")
    private String kivaClientId;
    //14
    @SerializedName("Cycle")
    private long cycle;
    //15
    @SerializedName("Cohort")
    private String cohort;
    //16
    @SerializedName("Gender")
    private String gender;
    //17
    @SerializedName("Province")
    private String province;
    //18
    @SerializedName("Sector")
    private String sector;
    //19
    @SerializedName("Cell")
    private String cell;
    //20
    @SerializedName("District")
    private String district;
    //21
    @SerializedName("Nationality")
    private String nationality;
    //22
    @SerializedName("Telephone")
    private String telephone;
    //23
    @SerializedName("Mobile No")
    private String mobileNo;
    //24
    @SerializedName("Submission Date ")
    private LocalDate submissionDate;
    //25
    @SerializedName("Approval Date")
    private LocalDate approvalDate;
    //26
    @SerializedName("Disbursement Date")
    private LocalDate disbursementDate;
    //27
    @SerializedName("Applied Amount")
    private BigDecimal appliedAmount;
    //28
    @SerializedName("Approved Amount")
    private BigDecimal approvedAmount;
    //29
    @SerializedName("Disbursed Amount")
    private BigDecimal disbursedAmount;
    //30
    @SerializedName("Difference")
    private BigDecimal difference;
    //31
    @SerializedName("Currency Type")
    private String currencyType;
    //32
    @SerializedName("Re-payment Term")
    private String repaymentTerm;
    //33
    @SerializedName("Loan Type")
    private String loanType;
    //34
    @SerializedName("Terms Duration")
    private short termsDuration;
    //35
    @SerializedName("Actual Payment Amount")
    private BigDecimal actualPaymentAmount;
    //36
    @SerializedName("Principal Paid")
    private BigDecimal principalPaid;
    //37
    @SerializedName("Interest Paid")
    private BigDecimal interestPaid;
    //38
    @SerializedName("Insurance fee Paid")
    private BigDecimal insuranceFeePaid;
    //39
    @SerializedName("Total Late Fees Paid")
    private BigDecimal totalLateFeesPaid;
    //40
    @SerializedName("Excess Amount Paid")
    private BigDecimal excessAmountPaid;
    //41
    @SerializedName("Interest Waived")
    private BigDecimal interestWaived;
    //42
    @SerializedName("Current Balance")
    private BigDecimal currentBalance;
    //43
    @SerializedName("Principal Balance")
    private BigDecimal principalBalance;
    //44
    @SerializedName("Interest Balance")
    private BigDecimal interestBalance;
    //45
    @SerializedName("Fees Balance")
    private BigDecimal feesBalance;
    //46
    @SerializedName("Amount Past Due")
    private BigDecimal amountPastDue;
    //47
    @SerializedName("Principal Past Due")
    private BigDecimal principalPastDue;
    //48
    @SerializedName("Interest Past Due")
    private BigDecimal interestPastDue;
    //49
    @SerializedName("Fees Past Due")
    private BigDecimal feesPastDue;
    //50
    @SerializedName("Scheduled Principal Amount")
    private BigDecimal scheduledPrincipalAmount;
    //51
    @SerializedName("Scheduled Interest Amount")
    private BigDecimal scheduledInterestAmount;
    //52
    @SerializedName("Scheduled Fees Amount")
    private BigDecimal scheduledFeesAmount;
    //53
    @SerializedName("Scheduled Payment Amount")
    private BigDecimal scheduledPaymentAmount;
    //54
    @SerializedName("Last Payment Amount")
    private BigDecimal lastPaymentAmount;
    //55
    @SerializedName("Last Principal Amount")
    private BigDecimal lastPrincipalAmount;
    //56
    @SerializedName("Last Interest Amount")
    private BigDecimal lastInterestAmount;
    //57
    @SerializedName("Last Fees Amount")
    private BigDecimal lastFeesAmount;
    //58
    @SerializedName("Last Late Fees Amount")
    private BigDecimal lastLateFeesAmount;
    //59
    @SerializedName("Last Excess Amount")
    private BigDecimal lastExcessAmount;
    //60
    @SerializedName("Days in Arrears")
    private int daysInArrears;
    //61
    @SerializedName("Installment in Arrears")
    private long installmentInArrears;
    //62
    @SerializedName("Last Payment Date")
    private LocalDate lastPaymentDate;
    //63
    @SerializedName("Next Payment Due")
    private LocalDate nextPaymentDue;
    //64
    @SerializedName("Final Payment Date")
    private LocalDate finalPaymentDate;
    //65
    @SerializedName("Date Closed")
    private LocalDate dateClosed;
    //66
    @SerializedName("Loan Status")
    private String loanStatus;
    //67
    @SerializedName("Business Description")
    private String businessDescription;
    //68
    @SerializedName("Industry/Sector of Activity")
    private String industrySectorOfActivity;
    //69
    @SerializedName("Business Sub-Sector")
    private String businessSubSector;
    //70
    @SerializedName("Loan Created On")
    private String createdOnUtc;
    //71
    @SerializedName("Loan Last Modified on")
    private String lastModifiedUtc;
}
