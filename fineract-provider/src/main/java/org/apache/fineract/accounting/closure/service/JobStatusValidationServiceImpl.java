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
import org.apache.fineract.accounting.closure.exception.GLClosureInvalidException;
import org.apache.fineract.accounting.closure.exception.GLClosureInvalidException.GlClosureInvalidReason;
import org.apache.fineract.infrastructure.jobs.domain.ScheduledJobRunHistoryRepository;
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
    private final ScheduledJobRunHistoryRepository jobRunHistoryRepository;
    private final LoanRepository loanRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    @Autowired
    public JobStatusValidationServiceImpl(LoanTransactionRepository loanTransactionRepository,
                                          SavingsAccountTransactionRepository savingsAccountTransactionRepository,
                                          ScheduledJobRunHistoryRepository jobRunHistoryRepository,
                                          LoanRepository loanRepository,
                                          SavingsAccountRepository savingsAccountRepository) {
        this.loanTransactionRepository = loanTransactionRepository;
        this.savingsAccountTransactionRepository = savingsAccountTransactionRepository;
        this.jobRunHistoryRepository = jobRunHistoryRepository;
        this.loanRepository = loanRepository;
        this.savingsAccountRepository = savingsAccountRepository;
    }

    @Override
    public void validateJobsForClosure(Long officeId, LocalDate closureDate) {
        // Job keys for accrual and interest posting jobs (update these as per your job configuration)
        String POST_ACCRUAL_INTEREST_FOR_SAVINGSJobKey = "Post Accrual Interest for Savings";
        String POST_INTEREST_FOR_SAVINGSJobKey = "Post Interest For Savings";

        // 1. Check if jobs ran successfully on the closure date
        boolean accrualJobRan = jobRunHistoryRepository.didJobRunSuccessfullyOnDate(POST_ACCRUAL_INTEREST_FOR_SAVINGSJobKey, closureDate);
        boolean interestPostingJobRan = jobRunHistoryRepository.didJobRunSuccessfullyOnDate(POST_INTEREST_FOR_SAVINGSJobKey, closureDate);


        if (!accrualJobRan) {
            throw new GLClosureInvalidException(GlClosureInvalidReason.ACCRUAL_JOB_NOT_RUN, closureDate);
        }
        if (!interestPostingJobRan) {
            throw new GLClosureInvalidException(GlClosureInvalidReason.INTEREST_POSTING_JOB_NOT_RUN, closureDate);
        }

        for (var savings : savingsAccountRepository.findActiveOrMaturedAccountsByOffice(officeId)) {
            boolean hasTransaction = savingsAccountTransactionRepository.existsInterestPostingTransactionForOfficeAndDate(officeId, closureDate);
            boolean accrual = savingsAccountTransactionRepository.existsAccrualInterestPostingTransactionForOfficeAndDate(officeId, closureDate);
            if (!hasTransaction) {
                throw new GLClosureInvalidException(GlClosureInvalidReason.INTEREST_POSTING_JOB_NOT_RUN, closureDate);
            }
            if (!accrual) {
                throw new GLClosureInvalidException(GlClosureInvalidReason.ACCRUAL_JOB_NOT_RUN, closureDate);
            }
        }
    }
}
