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
package org.apache.fineract.accounting.closure.service;

import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.closure.exception.GLClosureInvalidException;
import org.apache.fineract.infrastructure.jobs.domain.ScheduledJobDetail;
import org.apache.fineract.infrastructure.jobs.domain.ScheduledJobDetailRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobStatusValidationServiceImpl implements JobStatusValidationService {

    private final LoanTransactionRepository loanTransactionRepository;
    private final SavingsAccountTransactionRepository savingsAccountTransactionRepository;
    private final ScheduledJobDetailRepository schedulerDetailRepository;
    private final LoanRepository loanRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JobStatusValidationServiceImpl(LoanTransactionRepository loanTransactionRepository,
            SavingsAccountTransactionRepository savingsAccountTransactionRepository, ScheduledJobDetailRepository schedulerDetailRepository,
            LoanRepository loanRepository, SavingsAccountRepository savingsAccountRepository, JdbcTemplate jdbcTemplate) {
        this.loanTransactionRepository = loanTransactionRepository;
        this.savingsAccountTransactionRepository = savingsAccountTransactionRepository;
        this.schedulerDetailRepository = schedulerDetailRepository;
        this.loanRepository = loanRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void validateClosureReadnes(Long officeId, LocalDate closureDate) {
        log.info("Validating GL Closure readiness for officeId: {} and closureDate: {} hassaving {} hasLoan {}", officeId, closureDate,
                hasUnpostedSavingsAccruals(officeId, closureDate), hasUnpostedLoanAccruals(officeId, closureDate));
        if (hasUnpostedSavingsAccruals(officeId, closureDate)) {
            throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.SAVINGS_ACCRUALS_NOT_POSTED, closureDate);
        }

        if (hasUnpostedLoanAccruals(officeId, closureDate)) {
            throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.LOAN_ACCRUALS_NOT_POSTED, closureDate);
        }
    }

    public boolean hasUnpostedSavingsAccruals(Long officeId, LocalDate closureDate) {

        final String sql = """
                   SELECT EXISTS (
                       SELECT 1
                       FROM m_savings_account msa
                       INNER JOIN m_savings_product msp ON msp.id = msa.product_id
                       INNER JOIN m_client mc ON mc.id = msa.client_id
                       WHERE mc.office_id = ?
                         AND msa.status_enum = 300
                         AND msp.accounting_type = 3
                         AND (msa.nominal_annual_interest_rate > 0 OR msa.allow_overdraft = true)
                         AND msa.start_interest_accrual_calculation_date <= ?
                         AND (
                               msa.interest_posted_till_date IS NULL
                            OR msa.interest_posted_till_date < ?
                            OR msa.total_interest_earned_derived > msa.total_interest_posted_derived
                            OR NOT EXISTS (
                                   SELECT 1
                                   FROM acc_gl_journal_entry je
                                   WHERE je.entity_type_enum = 2
                                     AND je.entity_id = msa.id
                                     AND je.transaction_date <= ?
                                     AND je.reversed = false
                            )
                         )
                   ) AS has_unposted_interest
                """;

        Boolean hasUnpostedInterest = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getBoolean("has_unposted_interest"), officeId,
                closureDate, closureDate, closureDate);
        return Boolean.TRUE.equals(hasUnpostedInterest);

    }

    public boolean hasUnpostedLoanAccruals(Long officeId, LocalDate closureDate) {

        final String sql = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM m_loan ml
                        INNER JOIN m_client mc ON mc.id = ml.client_id
                        INNER JOIN m_product_loan mpl ON mpl.id = ml.product_id
                        INNER JOIN m_loan_repayment_schedule rs ON rs.loan_id = ml.id
                        WHERE mc.office_id = ?
                          AND ml.loan_status_id = 300
                          AND mpl.accounting_type = 3
                          AND rs.duedate <= ?
                          AND (
                                (rs.accrual_interest_derived
                               + rs.accrual_fee_charges_derived
                               + rs.accrual_penalty_charges_derived)
                                >
                                (rs.interest_waived_derived
                               + rs.fee_charges_waived_derived
                               + rs.penalty_charges_waived_derived)
                              )
                          AND (
                                NOT EXISTS (
                                    SELECT 1
                                    FROM m_loan_transaction lt
                                    WHERE lt.loan_id = ml.id
                                      AND lt.is_reversed = false
                                      AND lt.transaction_type_enum IN (2, 4, 6)
                                      AND lt.transaction_date <= ?
                                      AND lt.amount >=
                                          (rs.accrual_interest_derived
                                         + rs.accrual_fee_charges_derived
                                         + rs.accrual_penalty_charges_derived
                                         - rs.interest_waived_derived
                                         - rs.fee_charges_waived_derived
                                         - rs.penalty_charges_waived_derived)
                                )
                                OR NOT EXISTS (
                                    SELECT 1
                                    FROM acc_gl_journal_entry je
                                    WHERE je.entity_type_enum = 1
                                      AND je.entity_id = ml.id
                                      AND je.transaction_date <= ?
                                      AND je.reversed = false
                                )
                              )
                    )
                """;

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, officeId, closureDate, // rs.duedate
                closureDate, // lt.transaction_date
                closureDate // GL transaction_date
        ));
    }

    @Override
    public void validateJobsForClosure(Long officeId, LocalDate closureDate) {
        // List of job names to validate. Add more job names as needed.
        List<String> jobNames = List.of("Post Interest For Savings", "Add Accrual Transactions",
                "Add Accrual Transactions For Loans With Income Posted As Transactions", "Add Periodic Accrual Transactions",
                "Apply Annual Fee For Savings", "Apply penalty to overdue loans", "Execute Standing Instruction",
                "Post Accrual Interest for Savings", "Transfer Fee For Loans From Savings");

        for (String jobName : jobNames) {
            ScheduledJobDetail jobRan = schedulerDetailRepository.didJobRunSuccessfullyOnDate(jobName, closureDate);
            if (jobRan != null) {
                switch (jobName) {
                    case "Post Accrual Interest for Savings":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.ACCRUAL_JOB_NOT_RUN,
                                closureDate);
                    case "Post Interest For Savings":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.INTEREST_POSTING_JOB_NOT_RUN,
                                closureDate);
                    case "Add Accrual Transactions":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.ADD_ACCRUAL_TRANSACTIONS_JOB,
                                closureDate);
                    case "Add Accrual Transactions For Loans With Income Posted As Transactions":
                        throw new GLClosureInvalidException(
                                GLClosureInvalidException.GlClosureInvalidReason.ADD_ACCRUAL_TX_FOR_LOANS_WITH_INCOME_POSTED_AS_TX,
                                closureDate);
                    case "Add Periodic Accrual Transactions":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.ADD_PERIODIC_ACCRUALS_JOB,
                                closureDate);
                    case "Apply Annual Fee For Savings":
                        throw new GLClosureInvalidException(
                                GLClosureInvalidException.GlClosureInvalidReason.APPLY_ANNUAL_FEES_FOR_SAVINGS_JOB, closureDate);
                    case "Apply penalty to overdue loans":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.APPLY_PENALTIES_FOR_LOANS_JOB,
                                closureDate);
                    case "Execute Standing Instruction":
                        throw new GLClosureInvalidException(
                                GLClosureInvalidException.GlClosureInvalidReason.EXECUTE_STANDING_INSTRUCTIONS_JOB, closureDate);
                    case "Transfer Fee For Loans From Savings":
                        throw new GLClosureInvalidException(
                                GLClosureInvalidException.GlClosureInvalidReason.TRANSFER_FEE_FOR_LOANS_FROM_SAVINGS_JOB, closureDate);

                }
            }
        }
    }
}
