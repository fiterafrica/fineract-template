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
import org.apache.fineract.accounting.closure.exception.GLClosureInvalidException;
import org.apache.fineract.infrastructure.jobs.domain.ScheduledJobDetail;
import org.apache.fineract.infrastructure.jobs.domain.ScheduledJobDetailRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobStatusValidationServiceImpl implements JobStatusValidationService {
    private final LoanTransactionRepository loanTransactionRepository;
    private final SavingsAccountTransactionRepository savingsAccountTransactionRepository;
    private final ScheduledJobDetailRepository schedulerDetailRepository;
    private final LoanRepository loanRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    @Autowired
    public JobStatusValidationServiceImpl(LoanTransactionRepository loanTransactionRepository,
                                          SavingsAccountTransactionRepository savingsAccountTransactionRepository,
                                          ScheduledJobDetailRepository schedulerDetailRepository,
                                          LoanRepository loanRepository,
                                          SavingsAccountRepository savingsAccountRepository) {
        this.loanTransactionRepository = loanTransactionRepository;
        this.savingsAccountTransactionRepository = savingsAccountTransactionRepository;
        this.schedulerDetailRepository = schedulerDetailRepository;
        this.loanRepository = loanRepository;
        this.savingsAccountRepository = savingsAccountRepository;
    }

    @Override
    public void validateJobsForClosure(Long officeId, LocalDate closureDate) {
        // List of job names to validate. Add more job names as needed.
        List<String> jobNames = List.of(
            "Post Accrual Interest for Savings",
            "Post Interest For Savings",
            "Add Accrual Transactions",
            "Add Accrual Transactions For Loans With Income Posted As Transactions",
            "Add Periodic Accrual Transactions",
            "Apply Annual Fee For Savings",
            "Apply penalty to overdue loans",
            "Execute Standing Instruction",
            "Execute Standing Instruction",
            "Transfer Fee For Loans From Savings"
        );

        for (String jobName : jobNames) {
            ScheduledJobDetail jobRan = schedulerDetailRepository.didJobRunSuccessfullyOnDate(jobName, closureDate);
            if (jobRan != null) {
                switch (jobName) {
                    case "Post Accrual Interest for Savings":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.ACCRUAL_JOB_NOT_RUN, closureDate);
                    case "Post Interest For Savings":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.INTEREST_POSTING_JOB_NOT_RUN, closureDate);
                    case "Add Accrual Transactions":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.ADD_ACCRUAL_TRANSACTIONS_JOB, closureDate);
                    case "Add Accrual Transactions For Loans With Income Posted As Transactions":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.ADD_ACCRUAL_TX_FOR_LOANS_WITH_INCOME_POSTED_AS_TX, closureDate);
                    case "Add Periodic Accrual Transactions":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.ADD_PERIODIC_ACCRUALS_JOB, closureDate);
                    case "Apply Annual Fee For Savings":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.APPLY_ANNUAL_FEES_FOR_SAVINGS_JOB, closureDate);
                    case "Apply penalty to overdue loans":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.APPLY_PENALTIES_FOR_LOANS_JOB, closureDate);
                    case "Execute Standing Instruction":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.EXECUTE_STANDING_INSTRUCTIONS_JOB, closureDate);
                    case "Transfer Fee For Loans From Savings":
                        throw new GLClosureInvalidException(GLClosureInvalidException.GlClosureInvalidReason.TRANSFER_FEE_FOR_LOANS_FROM_SAVINGS_JOB, closureDate);

                }
        }
    }
    }
}
