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
package org.apache.fineract.accounting.closure.exception;

import java.time.LocalDate;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformDomainRuleException;

/**
 * A {@link RuntimeException} thrown when a GL Closure is Invalid
 */
public class GLClosureInvalidException extends AbstractPlatformDomainRuleException {

    /*** enum of reasons for invalid Accounting Closure **/
    public enum GlClosureInvalidReason {

        FUTURE_DATE, ACCOUNTING_CLOSED, ACCRUAL_JOB_NOT_RUN, INTEREST_POSTING_JOB_NOT_RUN, ADD_ACCRUAL_TRANSACTIONS_JOB, ADD_ACCRUAL_TX_FOR_LOANS_WITH_INCOME_POSTED_AS_TX, ADD_PERIODIC_ACCRUALS_JOB, APPLY_ANNUAL_FEES_FOR_SAVINGS_JOB, APPLY_PENALTIES_FOR_LOANS_JOB, EXECUTE_STANDING_INSTRUCTIONS_JOB, TRANSFER_FEE_FOR_LOANS_FROM_SAVINGS_JOB, LOAN_ACCRUALS_NOT_POSTED, SAVINGS_ACCRUALS_NOT_POSTED;

        public String errorMessage() {
            if (name().equalsIgnoreCase("FUTURE_DATE")) {
                return "Accounting closures cannot be made for a future date";
            } else if (name().equalsIgnoreCase("ACCOUNTING_CLOSED")) {
                return "Accounting Closure for this branch has already been defined for a greater date";
            } else if (name().equalsIgnoreCase("ACCRUAL_JOB_NOT_RUN")) {
                return "[Post Accrual Interest for Savings] has not run for the selected period. Please run the accrual job before closing.";
            } else if (name().equalsIgnoreCase("INTEREST_POSTING_JOB_NOT_RUN")) {
                return "[Post Interest For Savings] job has not run for the selected period. Please run the interest posting job before closing.";
            } else if (name().equalsIgnoreCase("ADD_ACCRUAL_TRANSACTIONS_JOB")) {
                return "[Add Accrual Transactions] job has not run for the selected period. Please run the job before closing.";
            } else if (name().equalsIgnoreCase("ADD_ACCRUAL_TX_FOR_LOANS_WITH_INCOME_POSTED_AS_TX")) {
                return "[Add Accrual Transactions For Loans With Income Posted As Transactions] job has not run for the selected period. Please run the job before closing.";
            } else if (name().equalsIgnoreCase("ADD_PERIODIC_ACCRUALS_JOB")) {
                return "[Add Periodic Accruals Transactions] job has not run for the selected period. Please run the job before closing.";
            } else if (name().equalsIgnoreCase("APPLY_ANNUAL_FEES_FOR_SAVINGS_JOB")) {
                return "[Apply Annual Fees For Savings] job has not run for the selected period. Please run the job before closing.";
            } else if (name().equalsIgnoreCase("APPLY_PENALTIES_FOR_LOANS_JOB")) {
                return "[Apply penalty to overdue loans] job has not run for the selected period. Please run the job before closing.";
            } else if (name().equalsIgnoreCase("EXECUTE_STANDING_INSTRUCTIONS_JOB")) {
                return "[Execute Standing Instructions] job has not run for the selected period. Please run the job before closing.";
            } else if (name().equalsIgnoreCase("TRANSFER_FEE_FOR_LOANS_FROM_SAVINGS_JOB")) {
                return "[Transfer Fee For Loans From Savings] job has not run for the selected period. Please run the job before closing.";
            } else if (name().equalsIgnoreCase(SAVINGS_ACCRUALS_NOT_POSTED.toString())) {
                return "[SAVINGS ACCRUALS NOT POSTED] Saving Periodic Accruals Transactions not posted.";
            } else if (name().equalsIgnoreCase(LOAN_ACCRUALS_NOT_POSTED.toString())) {
                return "[LOAN ACCRUALS NOT POSTED] Loan Periodic Accruals Transactions not posted.";
            }
            return name();
        }

        public String errorCode() {
            if (name().equalsIgnoreCase("FUTURE_DATE")) {
                return "error.msg.glclosure.invalid.future.date";
            } else if (name().equalsIgnoreCase("ACCOUNTING_CLOSED")) {
                return "error.msg.glclosure.invalid.accounting.closed";
            } else if (name().equalsIgnoreCase("ACCRUAL_JOB_NOT_RUN")) {
                return "error.msg.glclosure.invalid.accrual.job.not.run";
            } else if (name().equalsIgnoreCase("INTEREST_POSTING_JOB_NOT_RUN")) {
                return "error.msg.glclosure.invalid.interest.posting.job.not.run";
            } else if (name().equalsIgnoreCase("ADD_ACCRUAL_TRANSACTIONS_JOB")) {
                return "error.msg.glclosure.invalid.add.accrual.transactions.job.not.run";
            } else if (name().equalsIgnoreCase("ADD_ACCRUAL_TX_FOR_LOANS_WITH_INCOME_POSTED_AS_TX")) {
                return "error.msg.glclosure.invalid.add.accrual.transactions.for.loans.with.income.posted.as.transactions.job.not.run";
            } else if (name().equalsIgnoreCase("ADD_PERIODIC_ACCRUALS_JOB")) {
                return "error.msg.glclosure.invalid.add.periodic.accruals.job.not.run";
            } else if (name().equalsIgnoreCase("APPLY_ANNUAL_FEES_FOR_SAVINGS_JOB")) {
                return "error.msg.glclosure.invalid.apply.annual.fees.for.savings.job.not.run";
            } else if (name().equalsIgnoreCase("APPLY_PENALTIES_FOR_LOANS_JOB")) {
                return "error.msg.glclosure.invalid.apply.penalties.for.loans.job.not.run";
            } else if (name().equalsIgnoreCase("EXECUTE_STANDING_INSTRUCTIONS_JOB")) {
                return "error.msg.glclosure.invalid.execute.standing.instructions.job.not.run";
            } else if (name().equalsIgnoreCase("TRANSFER_FEE_FOR_LOANS_FROM_SAVINGS_JOB")) {
                return "error.msg.glclosure.invalid.transfer.fee.for.loans.from.savings.job.not.run";
            } else if (name().equalsIgnoreCase(SAVINGS_ACCRUALS_NOT_POSTED.toString())) {
                return "error.msg.glclosure.invalid.add.accrual.transactions.job.not.run";
            } else if (name().equalsIgnoreCase(LOAN_ACCRUALS_NOT_POSTED.toString())) {
                return "error.msg.glclosure.invalid.add.accrual.transactions.job.not.run";
            }
            return name();
        }
    }

    public GLClosureInvalidException(final GlClosureInvalidReason reason, final LocalDate date) {
        super(reason.errorCode(), reason.errorMessage(), date);
    }
}
