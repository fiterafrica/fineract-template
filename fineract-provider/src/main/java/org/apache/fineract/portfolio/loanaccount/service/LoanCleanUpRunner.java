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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallmentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.LoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.AprCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class LoanCleanUpRunner implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(LoanCleanUpRunner.class);
    private final FineractPlatformTenant tenant;

    private final JdbcTemplate jdbcTemplate;
    private final int threadId;
    private int offset;

    private final int maxLoansToProcess;
    private final LoanAssembler loanAssembler;
    private final AprCalculator aprCalculator;
    private final LoanRepaymentScheduleInstallmentRepository installmentRepository;
    private final LoanRepository loanRepository;

    public LoanCleanUpRunner(int threadId, FineractPlatformTenant tenant, JdbcTemplate jdbcTemplate, int offset, int maxLoansToProcess,
            LoanAssembler loanAssembler, AprCalculator aprCalculator, LoanRepaymentScheduleInstallmentRepository installmentRepository,
            LoanRepository loanRepository) {
        this.threadId = threadId;
        this.tenant = tenant;
        this.jdbcTemplate = jdbcTemplate;
        this.offset = offset;
        this.maxLoansToProcess = maxLoansToProcess;
        this.loanAssembler = loanAssembler;
        this.aprCalculator = aprCalculator;
        this.installmentRepository = installmentRepository;
        this.loanRepository = loanRepository;
    }

    @Override
    public void run() {
        ThreadLocalContextUtil.setTenant(this.tenant);
        this.populateOverdueInstallmentChargeFix();
    }

    public void start() {
        LOG.info("Starting LoanCleanUpRunner {}...", threadId);
        Thread executorThread = new Thread(this);
        executorThread.start();
    }

    public void cleanUpLoans() {
        final int limit = 1000;
        int processed = 0;
        final String sql = "SELECT l.id FROM m_loan l JOIN loanaccount_v17 la ON l.external_id = la.encodedkey ORDER BY l.id LIMIT ? OFFSET ?";
        List<Long> loanIds = this.jdbcTemplate.queryForList(sql, Long.class, limit, this.offset);
        do {
            for (Long loanId : loanIds) {
                try {
                    Loan loan = this.loanAssembler.assembleFrom(loanId);
                    // 1. update the annual_nominal_interest rate
                    loan.getLoanProductRelatedDetail().updateInterestRateDerivedFields(this.aprCalculator);

                    // 3. update the loan schedule
                    int num = 1;
                    LocalDate date = loan.getDisbursementDate();
                    for (LoanRepaymentScheduleInstallment installment : loan.getRepaymentScheduleInstallments()) {
                        installment.setInstallmentNumber(num);
                        installment.setFromDate(date);
                        date = installment.getDueDate();// this will be the fromDate of the next installment
                        num += 1;
                        this.installmentRepository.saveAndFlush(installment);
                    }

                    final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = loan
                            .getTransactionProcessorFactory().determineProcessor(loan.getLoanProduct().getRepaymentStrategy());

                    // 3. clean up the loan transactions
                    for (LoanTransaction loanTransaction : loan.getLoanTransactions()) {
                        if (loanTransaction.isNotRepaymentType() && !loanTransaction.isPayoff() && loanTransaction.isNotWaiver()
                                && loanTransaction.isNotRecoveryRepayment()) {
                            continue; // skip
                        }
                        loanTransaction.resetDerivedComponents();
                        loanRepaymentScheduleTransactionProcessor.handleTransaction(loanTransaction, loan.getCurrency(),
                                loan.getRepaymentScheduleInstallments(), loan.charges());
                    }
                    loan.updateLoanSummaryDerivedFields();
                    this.loanRepository.saveAndFlush(loan);
                } catch (Exception e) {
                    LOG.error("Error processing loan {}", loanId, e);
                }
                LOG.info("Processed loan {}, which is {} of {} ", loanId, processed, this.maxLoansToProcess);
                processed += 1;
            }
            this.offset += limit;
            loanIds = this.jdbcTemplate.queryForList(sql, Long.class, limit, this.offset);
        } while (processed < this.maxLoansToProcess);
        LOG.info("Finished LoanCleanUpRunner {}...", threadId);
    }

    public void populateOverdueInstallmentCharge() {
        // 1. get all loans with overdue installments
        List<Long> loanIds = this.jdbcTemplate.queryForList("SELECT DISTINCT loan_id FROM m_loan_charge\n"
                + "WHERE id NOT IN (SELECT loan_charge_id FROM m_loan_overdue_installment_charge)\n"
                + "AND charge_id IN (SELECT id FROM m_charge WHERE charge_time_enum = 9)", Long.class);
        // 2. for each loan, get the overdue installments
        for (Long loanId : loanIds) {
            Loan loan = this.loanAssembler.assembleFrom(loanId);
            Integer arrearTolorence = loan.loanProduct().getLoanProductRelatedDetail().getGraceOnArrearsAgeing();
            if (arrearTolorence == null) {
                arrearTolorence = 0;
            }
            List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
            int index = 0;
            Map<Long, BigDecimal> installmentPenalties = new HashMap<>();
            for (LoanRepaymentScheduleInstallment installment : installments) {
                List<Map<String, Object>> results;
                LocalDate endDate = index < installments.size() - 1 ? installments.get(index + 1).getDueDate().plusDays(arrearTolorence)
                        : null;
                String sql = "SELECT * FROM m_loan_charge WHERE loan_id = ? AND id NOT IN (SELECT loan_charge_id FROM m_loan_overdue_installment_charge) AND due_for_collection_as_of_date >= ? ";
                if (endDate != null) {
                    sql += " AND due_for_collection_as_of_date <= ?";
                    results = this.jdbcTemplate.queryForList(sql, loanId, installment.getDueDate().plusDays(arrearTolorence), endDate);
                } else {
                    results = this.jdbcTemplate.queryForList(sql, loanId, installment.getDueDate().plusDays(arrearTolorence));
                }
                // for each result, create a loan_overdue_installment_charge
                int frequencyNumber = 1;
                BigDecimal totalChargeAmount = BigDecimal.ZERO;
                for (Map<String, Object> result : results) {
                    // insert the overdue installment charge
                    this.jdbcTemplate.update(
                            "INSERT INTO m_loan_overdue_installment_charge (loan_charge_id, loan_schedule_id, frequency_number) VALUES (?, ?, ?)",
                            result.get("id"), installment.getId(), frequencyNumber);
                    frequencyNumber += 1;
                    totalChargeAmount = totalChargeAmount.add((BigDecimal) result.get("amount"));
                }

                installmentPenalties.put(installment.getId(), totalChargeAmount);
                index += 1;
            }
            loan = this.loanAssembler.assembleFrom(loanId);
            for (LoanRepaymentScheduleInstallment installment : loan.getRepaymentScheduleInstallments()) {
                installment.setPenaltyCharges(installmentPenalties.get(installment.getId()));
            }
            loan.updateLoanSummaryDerivedFields();
            this.loanRepository.saveAndFlush(loan);
        }
    }

    public void populateOverdueInstallmentChargeFix() {
        // 1. get all loans with overdue installments
        // List<Long> loanIds = this.jdbcTemplate.queryForList("SELECT DISTINCT loan_id FROM m_loan_charge\n"
        // + "WHERE id NOT IN (SELECT loan_charge_id FROM m_loan_overdue_installment_charge)\n"
        // + "AND charge_id IN (SELECT id FROM m_charge WHERE charge_time_enum = 9)", Long.class);
        List<Long> loanIds = List.of(8406019L);
        // 2. for each loan, get the overdue installments
        for (Long loanId : loanIds) {
            Loan loan = this.loanAssembler.assembleFrom(loanId);
            List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
            for (LoanRepaymentScheduleInstallment installment : installments) {
                List<Map<String, Object>> results;
                String sql = "SELECT * FROM m_loan_charge WHERE loan_id = ? AND id IN (SELECT loan_charge_id FROM m_loan_overdue_installment_charge WHERE loan_schedule_id = ?) ";
                results = this.jdbcTemplate.queryForList(sql, loanId, installment.getId());
                // for each result, create a loan_overdue_installment_charge
                BigDecimal totalChargeAmount = BigDecimal.ZERO;
                for (Map<String, Object> result : results) {
                    // insert the overdue installment charge
                    totalChargeAmount = totalChargeAmount.add((BigDecimal) result.get("amount"));
                }
                installment.setPenaltyCharges(totalChargeAmount);
            }
            loan.updateLoanSummaryDerivedFields();
            this.loanRepository.saveAndFlush(loan);
        }
    }
}
