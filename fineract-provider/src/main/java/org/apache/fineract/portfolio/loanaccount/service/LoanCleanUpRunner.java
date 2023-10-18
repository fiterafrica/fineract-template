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
import java.util.List;
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
        this.cleanUpLoans();
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
}
