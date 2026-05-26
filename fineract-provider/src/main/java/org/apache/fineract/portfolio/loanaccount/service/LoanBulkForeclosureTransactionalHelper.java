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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanAccountDomainService;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryWritePlatformService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanBulkForeclosureTransactionalHelper {

    private final LoanAssembler loanAssembler;
    private final LoanAccountDomainService loanAccountDomainService;
    private final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService;

    @Transactional
    public String forecloseSingleLoan(Long loanId, LocalDate foreclosureDate) {
        final Loan loan = this.loanAssembler.assembleFrom(loanId);

        // Validate loan is active
        if (!loan.status().isActive()) {
            return "Loan is not in active state. Current status: " + loan.status().toString();
        }

        // Validate no undisbursed tranches before foreclosure date
        for (var dd : loan.getDisbursementDetails()) {
            if (!dd.expectedDisbursementDateAsLocalDate().isAfter(foreclosureDate) && dd.actualDisbursementDate() == null) {
                return "Loan has undisbursed tranche before foreclosure date";
            }
        }

        // Archive schedule
        this.loanScheduleHistoryWritePlatformService.createAndSaveLoanScheduleArchive(loan.getRepaymentScheduleInstallments(), loan, null);

        // Execute foreclosure
        this.loanAccountDomainService.foreCloseLoan(loan, foreclosureDate, "Bulk foreclosure");
        return null;
    }
}
