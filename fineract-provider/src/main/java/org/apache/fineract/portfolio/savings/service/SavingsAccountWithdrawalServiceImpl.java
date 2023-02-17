/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.savings.service;

import java.time.LocalDate;
import java.util.Optional;
import org.apache.fineract.portfolio.savings.WithdrawalFrequency;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsProduct;
import org.apache.fineract.portfolio.savings.domain.SavingsProductRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsWithdrawalSchedule;
import org.apache.fineract.portfolio.savings.domain.SavingsWithdrawalScheduleData;
import org.apache.fineract.portfolio.savings.domain.SavingsWithdrawalScheduleRepository;
import org.apache.fineract.portfolio.savings.exception.SavingsProductNotFoundException;
import org.apache.fineract.portfolio.savings.exception.SavingsScheduleFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SavingsAccountWithdrawalServiceImpl implements SavingsAccountWithdrawalService {

    private final SavingsWithdrawalScheduleRepository savingsWithdrawalScheduleRepository;
    private final SavingsProductRepository savingProductRepository;

    @Autowired
    public SavingsAccountWithdrawalServiceImpl(SavingsWithdrawalScheduleRepository savingsWithdrawalScheduleRepository,
            final SavingsProductRepository savingProductRepository) {
        this.savingsWithdrawalScheduleRepository = savingsWithdrawalScheduleRepository;
        this.savingProductRepository = savingProductRepository;
    }

    @Override
    public void updateNextWithdrawalDate(SavingsAccount savingsAccount, Integer withdrawalFrequency, LocalDate nextWithdrawalDate) {
        SavingsWithdrawalSchedule savingsWithdrawalSchedule;
        Optional<SavingsWithdrawalSchedule> savingsWithdrawalScheduleOptional = savingsWithdrawalScheduleRepository
                .findBySavingsAccountId(savingsAccount.getId());
        if (savingsWithdrawalScheduleOptional.isPresent()) {
            savingsWithdrawalSchedule = savingsWithdrawalScheduleOptional.get();
            savingsWithdrawalSchedule.setNextWithdrawalDate(nextWithdrawalDate);
        } else {
            savingsWithdrawalSchedule = new SavingsWithdrawalSchedule(savingsAccount, withdrawalFrequency, nextWithdrawalDate);
        }

        savingsWithdrawalScheduleRepository.saveAndFlush(savingsWithdrawalSchedule);
    }

    /**
     * Determines whether the current date is a next withdrawal date for the savings account, based on the savings
     * product's withdrawal frequency and the date of the most recent withdrawal. If the given date is not a next
     * withdrawal date, returns false.
     *
     * @param savingsAccount
     *            the savings account on witch We want to determine this
     * @return true if the given date is a next withdrawal date, false otherwise
     * @throws UnsupportedOperationException
     *             if the savings product's withdrawal frequency is not supported
     */
    @Override
    public boolean isTodayNextWithdrawalDate(SavingsAccount savingsAccount) {

        SavingsProduct savingsProduct = savingProductRepository.findById(savingsAccount.getSavingsProductId())
                .orElseThrow(() -> new SavingsProductNotFoundException(savingsAccount.getSavingsProductId()));
        SavingsWithdrawalSchedule withdrawalSchedule = savingsWithdrawalScheduleRepository.findBySavingsAccountId(savingsAccount.getId())
                .orElseThrow(() -> new SavingsScheduleFoundException(savingsAccount.getId()));

        WithdrawalFrequency withdrawalFrequency = WithdrawalFrequency.fromInt(savingsProduct.getWithdrawalFrequency());
        LocalDate originalDate = withdrawalSchedule.getNextWithdrawalDate();
        LocalDate candidateNextWithdrawalDate = originalDate;
        int originalDayOfMonth = originalDate.getDayOfMonth();

        // Iterate over the potential next withdrawal dates until the next
        // withdrawal date is after or equal to the current date
        LocalDate currentDate = LocalDate.now();
        while (candidateNextWithdrawalDate.isBefore(currentDate)) {
            switch (withdrawalFrequency) {
                case MONTHLY:
                    candidateNextWithdrawalDate = candidateNextWithdrawalDate.plusMonths(1);
                break;
                case QUARTERLY:
                    candidateNextWithdrawalDate = candidateNextWithdrawalDate.plusMonths(3);
                break;

                case BI_ANNUAL:
                    candidateNextWithdrawalDate = candidateNextWithdrawalDate.plusMonths(6);
                break;

                case ANNUAL:
                    candidateNextWithdrawalDate = candidateNextWithdrawalDate.plusYears(1);
                break;
                default:
                    throw new UnsupportedOperationException("Unsupported withdrawal frequency: " + withdrawalFrequency);
            }
        }

        // Check if the original date is before the current date and if the
        // candidate next withdrawal date is on or after the original date
        return originalDate.isBefore(currentDate) && !candidateNextWithdrawalDate.isAfter(currentDate)
                && candidateNextWithdrawalDate.getDayOfMonth() == originalDayOfMonth;
    }

    /**
     * Gets the next withdrawal date for this savings account, based on the savings account's withdrawal frequency and
     * the given start date. If the start date is not a valid next withdrawal date, returns the next valid withdrawal
     * date after the start date. If the withdrawal frequency is not supported, throws an UnsupportedOperationException.
     *
     * @param startDate
     *            the start date from which to calculate the next withdrawal date
     * @return the next withdrawal date
     * @throws UnsupportedOperationException
     *             if the savings account's withdrawal frequency is not supported
     */
    @Override
    public SavingsWithdrawalScheduleData findByWithdrawalFrequencyAndDate(Long savingsProductId, LocalDate startDate) {
        int originalDayOfMonth = startDate.getDayOfMonth();
        LocalDate candidateNextWithdrawalDate;
        SavingsProduct savingsProduct = savingProductRepository.findById(savingsProductId)
                .orElseThrow(() -> new SavingsProductNotFoundException(savingsProductId));
        WithdrawalFrequency withdrawalFrequencyEnum = WithdrawalFrequency.fromInt(savingsProduct.getWithdrawalFrequency());
        switch (withdrawalFrequencyEnum) {
            case MONTHLY:
                candidateNextWithdrawalDate = startDate.plusMonths(1);
            break;
            case QUARTERLY:
                candidateNextWithdrawalDate = startDate.plusMonths(3);
            break;

            case BI_ANNUAL:
                candidateNextWithdrawalDate = startDate.plusMonths(6);
            break;
            case ANNUAL:
                candidateNextWithdrawalDate = startDate.plusYears(1);
            break;
            default:
                throw new UnsupportedOperationException("Unsupported withdrawal frequency: " + savingsProduct.getWithdrawalFrequency());
        }

        int candidateDayOfMonth = candidateNextWithdrawalDate.getDayOfMonth();

        // If the candidate day of month is after the original day of month,
        // use the candidate date as the next withdrawal date. Otherwise, use
        // the same day of the original date in the next month as the next
        // withdrawal date.
        if (candidateDayOfMonth > originalDayOfMonth) {
            return new SavingsWithdrawalScheduleData(null, null, candidateNextWithdrawalDate);
        } else {
            return new SavingsWithdrawalScheduleData(null, null,
                    LocalDate.of(candidateNextWithdrawalDate.getYear(), candidateNextWithdrawalDate.getMonth(), originalDayOfMonth));
        }
    }

}
