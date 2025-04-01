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
package org.apache.fineract.portfolio.savings.domain.interest;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.fineract.infrastructure.core.domain.LocalDateInterval;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.savings.SavingsCompoundingInterestPeriodType;
import org.apache.fineract.portfolio.savings.SavingsInterestCalculationType;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountFloatingInterestRateData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountFloatingInterestRate;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsProductFloatingInterestRate;

@Slf4j
public final class PostingPeriod {

    private final LocalDateInterval periodInterval;
    private final MonetaryCurrency currency;
    private final SavingsCompoundingInterestPeriodType interestCompoundingType;
    private BigDecimal interestRateAsFraction;
    private final long daysInYear;
    private final List<CompoundingPeriod> compoundingPeriods;

    // interest posting details
    private final LocalDate dateOfPostingTransaction;
    private List<BigDecimal> interestEarnedUnroundeds;
    private List<Money> interestEarnedRoundeds;
    private BigDecimal interestEarnedUnrounded;
    private Money interestEarnedRounded;

    // opening/closing details
    private final Money openingBalance;
    private final Money closingBalance;
    private final SavingsInterestCalculationType interestCalculationType;

    // include in compounding interest
    private boolean interestTransfered = false;
    private boolean isUserPosting = false;

    // minimum balance for interest calculation
    private final Money minBalanceForInterestCalculation;
    private BigDecimal overdraftInterestRateAsFraction;
    private Money minOverdraftForInterestCalculation;

    private Integer financialYearBeginningMonth;

    public static PostingPeriod createFrom(final LocalDateInterval periodInterval, final Money periodStartingBalance,
                                           final List<SavingsAccountTransaction> orderedListOfTransactions, final MonetaryCurrency currency,
                                           final SavingsCompoundingInterestPeriodType interestCompoundingPeriodType,
                                           final SavingsInterestCalculationType interestCalculationType, final BigDecimal interestRateAsFraction, final long daysInYear,
                                           final LocalDate upToInterestCalculationDate, Collection<Long> interestPostTransactions, boolean isInterestTransfer,
                                           final Money minBalanceForInterestCalculation, final boolean isSavingsInterestPostingAtCurrentPeriodEnd,
                                           final boolean isUserPosting, Integer financialYearBeginningMonth, SavingsAccount savingsAccount, final MathContext mc) {

        final BigDecimal overdraftInterestRateAsFraction = BigDecimal.ZERO;
        final Money minOverdraftForInterestCalculation = Money.zero(currency);

        return createFrom(periodInterval, periodStartingBalance, orderedListOfTransactions, currency, interestCompoundingPeriodType,
                interestCalculationType, interestRateAsFraction, daysInYear, upToInterestCalculationDate, interestPostTransactions,
                isInterestTransfer, minBalanceForInterestCalculation, isSavingsInterestPostingAtCurrentPeriodEnd,
                overdraftInterestRateAsFraction, minOverdraftForInterestCalculation, isUserPosting, financialYearBeginningMonth,
                savingsAccount, mc);
    }

    // isInterestTransfer boolean is to identify newly created transaction is
    // interest transfer
    public static PostingPeriod createFrom(final LocalDateInterval periodInterval, final Money periodStartingBalance,
                                           final List<SavingsAccountTransaction> orderedListOfTransactions, final MonetaryCurrency currency,
                                           final SavingsCompoundingInterestPeriodType interestCompoundingPeriodType,
                                           final SavingsInterestCalculationType interestCalculationType, final BigDecimal interestRateAsFraction, final long daysInYear,
                                           final LocalDate upToInterestCalculationDate, Collection<Long> interestPostTransactions, boolean isInterestTransfer,
                                           final Money minBalanceForInterestCalculation, final boolean isSavingsInterestPostingAtCurrentPeriodEnd,
                                           final BigDecimal overdraftInterestRateAsFraction, final Money minOverdraftForInterestCalculation, boolean isUserPosting,
                                           int financialYearBeginningMonth, SavingsAccount savingsAccount, final MathContext mc) {

        final List<EndOfDayBalance> accountEndOfDayBalances = new ArrayList<>();
        boolean interestTransfered = false;
        Money openingDayBalance = periodStartingBalance;
        Money closeOfDayBalance = openingDayBalance;

        for (final SavingsAccountTransaction transaction : orderedListOfTransactions) {

            if (transaction.fallsWithin(periodInterval)) {
                // the balance of the transaction falls entirely within this
                // period so no need to do any cropping/bounding
                final EndOfDayBalance endOfDayBalance = transaction.toEndOfDayBalance(openingDayBalance);
                accountEndOfDayBalances.add(endOfDayBalance);

                openingDayBalance = endOfDayBalance.closingBalance();

            } else if (transaction.spansAnyPortionOf(periodInterval)) {
                final EndOfDayBalance endOfDayBalance = transaction.toEndOfDayBalanceBoundedBy(openingDayBalance, periodInterval);
                accountEndOfDayBalances.add(endOfDayBalance);

                closeOfDayBalance = endOfDayBalance.closingBalance();
                openingDayBalance = closeOfDayBalance;
            }

            // this check is to make sure to add interest if withdrawal is
            // happened for already
            if (transaction.occursOn(periodInterval.endDate().plusDays(1))) {
                if (transaction.getId() == null) {
                    interestTransfered = isInterestTransfer;
                } else if (interestPostTransactions.contains(transaction.getId())) {
                    interestTransfered = true;
                }
            }

        }

        if (accountEndOfDayBalances.isEmpty()) {
            LocalDate balanceStartDate = periodInterval.startDate();
            LocalDate balanceEndDate = periodInterval.endDate();
            Integer numberOfDaysOfBalance = periodInterval.daysInPeriodInclusiveOfEndDate();

            if (balanceEndDate.isAfter(upToInterestCalculationDate)) {
                balanceEndDate = upToInterestCalculationDate;
                final LocalDateInterval spanOfBalance = LocalDateInterval.create(balanceStartDate, balanceEndDate);
                numberOfDaysOfBalance = spanOfBalance.daysInPeriodInclusiveOfEndDate();
            }

            final EndOfDayBalance endOfDayBalance = EndOfDayBalance.from(balanceStartDate, openingDayBalance, closeOfDayBalance,
                    numberOfDaysOfBalance);

            accountEndOfDayBalances.add(endOfDayBalance);

            closeOfDayBalance = endOfDayBalance.closingBalance();
            openingDayBalance = closeOfDayBalance;
        }

        final List<CompoundingPeriod> compoundingPeriods = compoundingPeriodsInPostingPeriod(periodInterval, interestCompoundingPeriodType,
                accountEndOfDayBalances, upToInterestCalculationDate, financialYearBeginningMonth);

        return new PostingPeriod(periodInterval, currency, periodStartingBalance, openingDayBalance, interestCompoundingPeriodType,
                interestCalculationType, interestRateAsFraction, daysInYear, compoundingPeriods, interestTransfered,
                minBalanceForInterestCalculation, isSavingsInterestPostingAtCurrentPeriodEnd, overdraftInterestRateAsFraction,
                minOverdraftForInterestCalculation, isUserPosting, financialYearBeginningMonth, savingsAccount, mc);
    }

    public static PostingPeriod createFromDTO(final LocalDateInterval periodInterval, final Money periodStartingBalance,
                                              final List<SavingsAccountTransactionData> orderedListOfTransactions, final MonetaryCurrency currency,
                                              final SavingsCompoundingInterestPeriodType interestCompoundingPeriodType,
                                              final SavingsInterestCalculationType interestCalculationType, final BigDecimal interestRateAsFraction, final long daysInYear,
                                              final LocalDate upToInterestCalculationDate, Collection<Long> interestPostTransactions, boolean isInterestTransfer,
                                              final Money minBalanceForInterestCalculation, final boolean isSavingsInterestPostingAtCurrentPeriodEnd,
                                              final BigDecimal overdraftInterestRateAsFraction, final Money minOverdraftForInterestCalculation, boolean isUserPosting,
                                              int financialYearBeginningMonth, final boolean isAllowOverdraft, final SavingsAccountData savingsAccountData,
                                              final MathContext mc) {

        final List<EndOfDayBalance> accountEndOfDayBalances = new ArrayList<>();
        boolean interestTransfered = false;
        Money openingDayBalance = periodStartingBalance;
        Money closeOfDayBalance = openingDayBalance;

        for (final SavingsAccountTransactionData transaction : orderedListOfTransactions) {

            if (transaction.fallsWithin(periodInterval)) {
                // the balance of the transaction falls entirely within this
                // period so no need to do any cropping/bounding
                final EndOfDayBalance endOfDayBalance = transaction.toEndOfDayBalance(openingDayBalance);
                accountEndOfDayBalances.add(endOfDayBalance);

                openingDayBalance = endOfDayBalance.closingBalance();

            } else if (transaction.spansAnyPortionOf(periodInterval)) {
                final EndOfDayBalance endOfDayBalance = transaction.toEndOfDayBalanceBoundedBy(openingDayBalance, periodInterval,
                        isAllowOverdraft);
                accountEndOfDayBalances.add(endOfDayBalance);

                closeOfDayBalance = endOfDayBalance.closingBalance();
                openingDayBalance = closeOfDayBalance;
            }

            // this check is to make sure to add interest if withdrawal is
            // happened for already
            if (transaction.occursOn(periodInterval.endDate().plusDays(1))) {
                if (transaction.getId() == null) {
                    interestTransfered = isInterestTransfer;
                } else if (interestPostTransactions.contains(transaction.getId())) {
                    interestTransfered = true;
                }
            }

        }

        if (accountEndOfDayBalances.isEmpty()) {
            LocalDate balanceStartDate = periodInterval.startDate();
            LocalDate balanceEndDate = periodInterval.endDate();
            Integer numberOfDaysOfBalance = periodInterval.daysInPeriodInclusiveOfEndDate();

            if (balanceEndDate.isAfter(upToInterestCalculationDate)) {
                balanceEndDate = upToInterestCalculationDate;
                final LocalDateInterval spanOfBalance = LocalDateInterval.create(balanceStartDate, balanceEndDate);
                numberOfDaysOfBalance = spanOfBalance.daysInPeriodInclusiveOfEndDate();
            }

            final EndOfDayBalance endOfDayBalance = EndOfDayBalance.from(balanceStartDate, openingDayBalance, closeOfDayBalance,
                    numberOfDaysOfBalance);

            accountEndOfDayBalances.add(endOfDayBalance);

            closeOfDayBalance = endOfDayBalance.closingBalance();
            openingDayBalance = closeOfDayBalance;
        }

        final List<CompoundingPeriod> compoundingPeriods = compoundingPeriodsInPostingPeriod(periodInterval, interestCompoundingPeriodType,
                accountEndOfDayBalances, upToInterestCalculationDate, financialYearBeginningMonth);

        return new PostingPeriod(periodInterval, currency, periodStartingBalance, openingDayBalance, interestCompoundingPeriodType,
                interestCalculationType, interestRateAsFraction, daysInYear, compoundingPeriods, interestTransfered,
                minBalanceForInterestCalculation, isSavingsInterestPostingAtCurrentPeriodEnd, overdraftInterestRateAsFraction,
                minOverdraftForInterestCalculation, isUserPosting, financialYearBeginningMonth, savingsAccountData, mc);
    }

    private PostingPeriod(final LocalDateInterval periodInterval, final MonetaryCurrency currency, final Money openingBalance,
                          final Money closingBalance, final SavingsCompoundingInterestPeriodType interestCompoundingType,
                          final SavingsInterestCalculationType interestCalculationType, BigDecimal interestRateAsFraction, final long daysInYear,
                          final List<CompoundingPeriod> compoundingPeriods, boolean interestTransfered, final Money minBalanceForInterestCalculation,
                          final boolean isSavingsInterestPostingAtCurrentPeriodEnd, final BigDecimal overdraftInterestRateAsFraction,
                          final Money minOverdraftForInterestCalculation, boolean isUserPosting, Integer financialYearBeginningMonth,
                          SavingsAccount savingsAccount, final MathContext mc) {
        this.periodInterval = periodInterval;
        this.currency = currency;
        this.openingBalance = openingBalance;
        this.closingBalance = closingBalance;
        this.interestCompoundingType = interestCompoundingType;
        this.interestCalculationType = interestCalculationType;
        this.interestRateAsFraction = interestRateAsFraction;
        this.daysInYear = daysInYear;
        this.compoundingPeriods = compoundingPeriods;

        if (isSavingsInterestPostingAtCurrentPeriodEnd) {
            this.dateOfPostingTransaction = periodInterval.endDate();
        } else {
            this.dateOfPostingTransaction = periodInterval.endDate().plusDays(1);
        }
        this.interestTransfered = interestTransfered;
        this.minBalanceForInterestCalculation = minBalanceForInterestCalculation;
        this.overdraftInterestRateAsFraction = overdraftInterestRateAsFraction;
        this.minOverdraftForInterestCalculation = minOverdraftForInterestCalculation;
        this.isUserPosting = isUserPosting;
        this.financialYearBeginningMonth = financialYearBeginningMonth;

        // finding out floating interest rate if applicable
        if (savingsAccount.getUseFloatingInterestRate() != null && Boolean.TRUE.equals(savingsAccount.getUseFloatingInterestRate())) {
            if (!CollectionUtils.isEmpty(savingsAccount.getSavingsAccountFloatingInterestRates())) {// this should not
                // be the case
                BigDecimal floatingInterestRateAsFraction = BigDecimal.ZERO;
                Set<SavingsAccountFloatingInterestRate> savingsAccountFloatingInterestRates = savingsAccount
                        .getSavingsAccountFloatingInterestRates();
                Set<SavingsProductFloatingInterestRate> savingsProductFloatingInterestRates = savingsAccount.savingsProduct()
                        .getSavingsProductFloatingInterestRates();
                TreeSet<SavingsProductFloatingInterestRate> sortedSavingsAccountFloatingInterestRates = new TreeSet(
                        savingsProductFloatingInterestRates);

                // set last floating rate interest rate end date as 200 years from now if not set
                SavingsProductFloatingInterestRate lastElement = sortedSavingsAccountFloatingInterestRates.last();
                if (lastElement.getEndDate() == null) {
                    LocalDate endDateFrom200YearsFromFromDate = lastElement.getFromDate().plusYears(200);
                    lastElement.setEndDate(endDateFrom200YearsFromFromDate);
                }

                // set endDate of period with next period fromDate -1 if endDate of period is not set
                for (SavingsProductFloatingInterestRate currentElement : sortedSavingsAccountFloatingInterestRates) {
                    if (currentElement.getEndDate() == null) {
                        SavingsProductFloatingInterestRate nextElement = sortedSavingsAccountFloatingInterestRates.higher(currentElement);
                        currentElement.setEndDate(nextElement.getFromDate().minusDays(1));
                    }
                }

                for (SavingsProductFloatingInterestRate savingsAccountFloatingInterestRate : sortedSavingsAccountFloatingInterestRates) {
                    if (savingsAccountFloatingInterestRate.isApplicableFloatingInterestRateForDate(periodInterval)) {
                        BigDecimal selectedFloatingInterestRate = savingsAccountFloatingInterestRate.getFloatingInterestRate();
                        floatingInterestRateAsFraction = selectedFloatingInterestRate.divide(BigDecimal.valueOf(100L), mc);
                    }
                }

                // if floating interest is used, it should found the correct from above step
                // else it means floating interest rates are not defined correctly, and it will 0
                this.interestRateAsFraction = floatingInterestRateAsFraction;
            }
        }
        //
    }

    private PostingPeriod(final LocalDateInterval periodInterval, final MonetaryCurrency currency, final Money openingBalance,
                          final Money closingBalance, final SavingsCompoundingInterestPeriodType interestCompoundingType,
                          final SavingsInterestCalculationType interestCalculationType, BigDecimal interestRateAsFraction, final long daysInYear,
                          final List<CompoundingPeriod> compoundingPeriods, boolean interestTransfered, final Money minBalanceForInterestCalculation,
                          final boolean isSavingsInterestPostingAtCurrentPeriodEnd, final BigDecimal overdraftInterestRateAsFraction,
                          final Money minOverdraftForInterestCalculation, boolean isUserPosting, Integer financialYearBeginningMonth,
                          SavingsAccountData savingsAccountData, final MathContext mc) {
        this.periodInterval = periodInterval;
        this.currency = currency;
        this.openingBalance = openingBalance;
        this.closingBalance = closingBalance;
        this.interestCompoundingType = interestCompoundingType;
        this.interestCalculationType = interestCalculationType;
        this.interestRateAsFraction = interestRateAsFraction;
        this.daysInYear = daysInYear;
        this.compoundingPeriods = compoundingPeriods;
        if (isSavingsInterestPostingAtCurrentPeriodEnd) {
            this.dateOfPostingTransaction = periodInterval.endDate();
        } else {
            this.dateOfPostingTransaction = periodInterval.endDate().plusDays(1);
        }
        this.interestTransfered = interestTransfered;
        this.minBalanceForInterestCalculation = minBalanceForInterestCalculation;
        this.overdraftInterestRateAsFraction = overdraftInterestRateAsFraction;
        this.minOverdraftForInterestCalculation = minOverdraftForInterestCalculation;
        this.isUserPosting = isUserPosting;
        this.financialYearBeginningMonth = financialYearBeginningMonth;

        // finding out floating interest rate if applicable
        if (Boolean.TRUE.equals(savingsAccountData.getUseFloatingInterestRate())) {
            if (!CollectionUtils.isEmpty(savingsAccountData.getFloatingInterestRates())) {
                BigDecimal floatingInterestRateAsFraction = BigDecimal.ZERO;
                Collection<SavingsAccountFloatingInterestRateData> savingsAccountFloatingInterestRates = savingsAccountData
                        .getFloatingInterestRates();
                TreeSet<SavingsAccountFloatingInterestRateData> sortedSavingsAccountFloatingInterestRates = new TreeSet(
                        savingsAccountFloatingInterestRates);

                // set last floating rate interest rate end date as 200 years from now if not set
                SavingsAccountFloatingInterestRateData lastElement = sortedSavingsAccountFloatingInterestRates.last();
                if (lastElement.getEndDate() == null) {
                    LocalDate endDateFrom200YearsFromFromDate = lastElement.getFromDate().plusYears(200);
                    lastElement.setEndDate(endDateFrom200YearsFromFromDate);
                }

                // set endDate of period with next period fromDate -1 if endDate of period is not set
                for (SavingsAccountFloatingInterestRateData currentElement : sortedSavingsAccountFloatingInterestRates) {
                    if (currentElement.getEndDate() == null) {
                        SavingsAccountFloatingInterestRateData nextElement = sortedSavingsAccountFloatingInterestRates
                                .higher(currentElement);
                        currentElement.setEndDate(nextElement.getFromDate().minusDays(1));
                    }
                }
                for (SavingsAccountFloatingInterestRateData savingsAccountFloatingInterestRate : sortedSavingsAccountFloatingInterestRates) {
                    if (savingsAccountFloatingInterestRate.isApplicableFloatingInterestRateForDate(periodInterval)) {
                        BigDecimal selectedFloatingInterestRate = savingsAccountFloatingInterestRate.getFloatingInterestRate();
                        floatingInterestRateAsFraction = selectedFloatingInterestRate.divide(BigDecimal.valueOf(100L), mc);
                    }
                }

                // this conditions should be always true if floating interest is used
                // else it means floating interest rates are not defined correctly
                this.interestRateAsFraction = floatingInterestRateAsFraction;
            }
        }
        //
    }

    public List<Money> interests() {
        return this.interestEarnedRoundeds;
    }

    public Money interest() {
        return this.interestEarnedRounded;
    }

    public LocalDate dateOfPostingTransaction() {
        return this.dateOfPostingTransaction;
    }

    public Money closingBalance() {
        return this.closingBalance;
    }

    public Money openingBalance() {
        return this.openingBalance;
    }

    public BigDecimal calculateInterest(final CompoundInterestValues compoundInterestValues) {
        BigDecimal interestEarned = BigDecimal.ZERO;
        log.info("calculateInterest - - - -->"+compoundInterestValues);
        // for each compounding period accumulate the amount of interest
        // to be applied to the balanced for interest calculation
        for (final CompoundingPeriod compoundingPeriod : this.compoundingPeriods) {

            // The critical part: when calling calculateInterest on the compounding period,
            // if we're using NONE compounding, we should pass the ORIGINAL PRINCIPAL,
            // not the compounded interest
            final BigDecimal interestUnrounded;
            if (SavingsCompoundingInterestPeriodType.NONE.equals(this.interestCompoundingType)) {
                // For NONE compounding, always calculate interest on the original principal (zero compounded interest)
                interestUnrounded = compoundingPeriod.calculateInterest(this.interestCompoundingType,
                        this.interestCalculationType, BigDecimal.ZERO, this.interestRateAsFraction,
                        this.daysInYear, this.minBalanceForInterestCalculation.getAmount(), this.overdraftInterestRateAsFraction,
                        this.minOverdraftForInterestCalculation.getAmount());
                log.info("NONE compounding: Interest calculated on original principal: {}", interestUnrounded);
            } else {
                // Normal compounding behavior - use compounded interest
                interestUnrounded = compoundingPeriod.calculateInterest(this.interestCompoundingType,
                        this.interestCalculationType, compoundInterestValues.getcompoundedInterest(), this.interestRateAsFraction,
                        this.daysInYear, this.minBalanceForInterestCalculation.getAmount(), this.overdraftInterestRateAsFraction,
                        this.minOverdraftForInterestCalculation.getAmount());
            }

            // Calculate uncompounded interest
            BigDecimal unCompoundedInterest = compoundInterestValues.getuncompoundedInterest().add(interestUnrounded);
            compoundInterestValues.setuncompoundedInterest(unCompoundedInterest);

            // Check if this is NONE compounding type - in this case we never compound the interest
            if (SavingsCompoundingInterestPeriodType.NONE.equals(this.interestCompoundingType)) {
                // For NONE compounding, we track the interest but never add it to the principal
                // DO NOT reset uncompoundedInterest - we want it to accumulate, but not compound
                log.debug("NONE compounding: Interest calculated: {}", interestUnrounded);
            } else {
                // Normal compounding behavior for other types
                LocalDate compoundingPeriodEndDate = compoundingPeriod.getPeriodInterval().endDate();
                if (!SavingsCompoundingInterestPeriodType.DAILY.equals(this.interestCompoundingType)) {
                    compoundingPeriodEndDate = determineInterestPeriodEndDateFrom(compoundingPeriod.getPeriodInterval().startDate(),
                            this.interestCompoundingType, compoundingPeriod.getPeriodInterval().endDate(),
                            this.getFinancialYearBeginningMonth());
                }

                if (compoundingPeriodEndDate.equals(compoundingPeriod.getPeriodInterval().endDate())) {
                    BigDecimal interestCompounded = compoundInterestValues.getcompoundedInterest().add(unCompoundedInterest);
                    compoundInterestValues.setcompoundedInterest(interestCompounded);
                    compoundInterestValues.setZeroForInterestToBeUncompounded();
                }
            }

            interestEarned = interestEarned.add(interestUnrounded);
        }

        this.interestEarnedUnrounded = interestEarned;
        this.interestEarnedRounded = Money.of(this.currency, this.interestEarnedUnrounded);

        log.info("Final interest calculated: {}", interestEarned);

        return interestEarned;

    }

    public List<Money> getInterestEarneds() {
        return this.interestEarnedRoundeds;
    }

    public Money getInterestEarned() {
        return this.interestEarnedRounded;
    }

    private static List<CompoundingPeriod> compoundingPeriodsInPostingPeriod(final LocalDateInterval postingPeriodInterval,
                                                                             final SavingsCompoundingInterestPeriodType interestPeriodType, final List<EndOfDayBalance> allEndOfDayBalances,
                                                                             final LocalDate upToInterestCalculationDate, int financialYearBeginningMonth) {

        final List<CompoundingPeriod> compoundingPeriods = new ArrayList<>();

        CompoundingPeriod compoundingPeriod = null;
        switch (interestPeriodType) {
            case INVALID:
                break;
            case DAILY:
                compoundingPeriod = DailyCompoundingPeriod.create(postingPeriodInterval, allEndOfDayBalances, upToInterestCalculationDate);
                compoundingPeriods.add(compoundingPeriod);
                break;
            case MONTHLY:

                final LocalDate postingPeriodEndDate = postingPeriodInterval.endDate();

                LocalDate periodStartDate = postingPeriodInterval.startDate();
                LocalDate periodEndDate = periodStartDate;

                while (!periodStartDate.isAfter(postingPeriodEndDate) && !periodEndDate.isAfter(postingPeriodEndDate)) {

                    periodEndDate = determineInterestPeriodEndDateFrom(periodStartDate, interestPeriodType, upToInterestCalculationDate,
                            financialYearBeginningMonth);
                    if (periodEndDate.isAfter(postingPeriodEndDate)) {
                        periodEndDate = postingPeriodEndDate;
                    }

                    final LocalDateInterval compoundingPeriodInterval = LocalDateInterval.create(periodStartDate, periodEndDate);
                    if (postingPeriodInterval.contains(compoundingPeriodInterval)) {

                        compoundingPeriod = MonthlyCompoundingPeriod.create(compoundingPeriodInterval, allEndOfDayBalances,
                                upToInterestCalculationDate);
                        compoundingPeriods.add(compoundingPeriod);
                    }

                    // move periodStartDate forward to day after this period
                    periodStartDate = periodEndDate.plusDays(1);
                }
                break;
            // case WEEKLY:
            // break;
            // case BIWEEKLY:
            // break;
            case QUATERLY:
                final LocalDate qPostingPeriodEndDate = postingPeriodInterval.endDate();

                periodStartDate = postingPeriodInterval.startDate();
                periodEndDate = periodStartDate;

                while (!periodStartDate.isAfter(qPostingPeriodEndDate) && !periodEndDate.isAfter(qPostingPeriodEndDate)) {

                    periodEndDate = determineInterestPeriodEndDateFrom(periodStartDate, interestPeriodType, upToInterestCalculationDate,
                            financialYearBeginningMonth);
                    if (periodEndDate.isAfter(qPostingPeriodEndDate)) {
                        periodEndDate = qPostingPeriodEndDate;
                    }

                    final LocalDateInterval compoundingPeriodInterval = LocalDateInterval.create(periodStartDate, periodEndDate);
                    if (postingPeriodInterval.contains(compoundingPeriodInterval)) {

                        compoundingPeriod = QuarterlyCompoundingPeriod.create(compoundingPeriodInterval, allEndOfDayBalances,
                                upToInterestCalculationDate);
                        compoundingPeriods.add(compoundingPeriod);
                    }

                    // move periodStartDate forward to day after this period
                    periodStartDate = periodEndDate.plusDays(1);
                }
                break;
            case BI_ANNUAL:
                final LocalDate bPostingPeriodEndDate = postingPeriodInterval.endDate();

                periodStartDate = postingPeriodInterval.startDate();
                periodEndDate = periodStartDate;

                while (!periodStartDate.isAfter(bPostingPeriodEndDate) && !periodEndDate.isAfter(bPostingPeriodEndDate)) {

                    periodEndDate = determineInterestPeriodEndDateFrom(periodStartDate, interestPeriodType, upToInterestCalculationDate,
                            financialYearBeginningMonth);
                    if (periodEndDate.isAfter(bPostingPeriodEndDate)) {
                        periodEndDate = bPostingPeriodEndDate;
                    }

                    final LocalDateInterval compoundingPeriodInterval = LocalDateInterval.create(periodStartDate, periodEndDate);
                    if (postingPeriodInterval.contains(compoundingPeriodInterval)) {

                        compoundingPeriod = BiAnnualCompoundingPeriod.create(compoundingPeriodInterval, allEndOfDayBalances,
                                upToInterestCalculationDate);
                        compoundingPeriods.add(compoundingPeriod);
                    }

                    // move periodStartDate forward to day after this period
                    periodStartDate = periodEndDate.plusDays(1);
                }
                break;
            case ANNUAL:
                final LocalDate aPostingPeriodEndDate = postingPeriodInterval.endDate();

                periodStartDate = postingPeriodInterval.startDate();
                periodEndDate = periodStartDate;

                while (!periodStartDate.isAfter(aPostingPeriodEndDate) && !periodEndDate.isAfter(aPostingPeriodEndDate)) {

                    periodEndDate = determineInterestPeriodEndDateFrom(periodStartDate, interestPeriodType, upToInterestCalculationDate,
                            financialYearBeginningMonth);
                    if (periodEndDate.isAfter(aPostingPeriodEndDate)) {
                        periodEndDate = aPostingPeriodEndDate;
                    }

                    final LocalDateInterval compoundingPeriodInterval = LocalDateInterval.create(periodStartDate, periodEndDate);
                    if (postingPeriodInterval.contains(compoundingPeriodInterval)) {

                        compoundingPeriod = AnnualCompoundingPeriod.create(compoundingPeriodInterval, allEndOfDayBalances,
                                upToInterestCalculationDate);
                        compoundingPeriods.add(compoundingPeriod);
                    }

                    // move periodStartDate forward to day after this period
                    periodStartDate = periodEndDate.plusDays(1);
                }
                break;
            case AT_MATURITY:
                final LocalDate atPostingPeriodEndDate = postingPeriodInterval.endDate();

                periodStartDate = postingPeriodInterval.startDate();
                periodEndDate = periodStartDate;

                while (!periodStartDate.isAfter(atPostingPeriodEndDate) && !periodEndDate.isAfter(atPostingPeriodEndDate)) {

                    periodEndDate = postingPeriodInterval.endDate();
                    if (periodEndDate.isAfter(atPostingPeriodEndDate)) {
                        periodEndDate = atPostingPeriodEndDate;
                    }

                    final LocalDateInterval compoundingPeriodInterval = LocalDateInterval.create(periodStartDate, periodEndDate);
                    if (postingPeriodInterval.contains(compoundingPeriodInterval)) {

                        compoundingPeriod = AnnualCompoundingPeriod.create(compoundingPeriodInterval, allEndOfDayBalances,
                                upToInterestCalculationDate);
                        compoundingPeriods.add(compoundingPeriod);
                    }

                    // move periodStartDate forward to day after this period
                    periodStartDate = periodEndDate.plusDays(1);
                }
                break;
            case NONE:
                final LocalDate nonePostingPeriodEndDate = postingPeriodInterval.endDate();
                periodStartDate = postingPeriodInterval.startDate();
                periodEndDate = nonePostingPeriodEndDate;

                if (periodEndDate.isAfter(upToInterestCalculationDate)) {
                    periodEndDate = upToInterestCalculationDate;
                }

                final LocalDateInterval compoundingPeriodInterval = LocalDateInterval.create(periodStartDate, periodEndDate);
                if (postingPeriodInterval.contains(compoundingPeriodInterval)) {
                    // Use the same approach as AT_MATURITY for the period creation,
                    // but interest won't be compounded in calculations
                    compoundingPeriod = AnnualCompoundingPeriod.create(compoundingPeriodInterval, allEndOfDayBalances,
                            upToInterestCalculationDate);
                    compoundingPeriods.add(compoundingPeriod);
                }

                break;
        }
        return compoundingPeriods;
    }

    private static LocalDate determineInterestPeriodEndDateFrom(final LocalDate periodStartDate,
                                                                final SavingsCompoundingInterestPeriodType interestPeriodType, final LocalDate upToInterestCalculationDate,
                                                                int financialYearBeginningMonth) {

        LocalDate periodEndDate = upToInterestCalculationDate;
        int previousMonth = financialYearBeginningMonth - 1;
        if (previousMonth == 0) {
            previousMonth = 12;
        }
        int periodsInMonth = 1;
        switch (interestPeriodType) {
            case INVALID:
                break;
            case DAILY:
                periodEndDate = periodStartDate;
                break;
            case MONTHLY:
                // produce period end date on last day of current month
                periodEndDate = periodStartDate.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case QUATERLY:
                periodsInMonth = 4;
                periodEndDate = getPeriodEndDate(periodEndDate, previousMonth, periodsInMonth, periodStartDate);
                break;
            case BI_ANNUAL:
                periodsInMonth = 2;
                periodEndDate = getPeriodEndDate(periodEndDate, previousMonth, periodsInMonth, periodStartDate);

                break;
            case ANNUAL:
                periodEndDate = periodStartDate.withMonth(previousMonth);
                periodEndDate = periodEndDate.with(TemporalAdjusters.lastDayOfMonth());
                if (periodEndDate.isBefore(periodStartDate)) {
                    periodEndDate = periodEndDate.plusYears(1);
                }
                break;
            case AT_MATURITY:
                break;
            case NONE:
                periodEndDate = upToInterestCalculationDate;
                log.info("Interest Compounding Period NONE {}", periodEndDate);
                break;
        }

        return periodEndDate;
    }

    private static LocalDate getPeriodEndDate(LocalDate periodEndDate, int previousMonth, int periodsInMonth, LocalDate periodStartDate) {
        int year = periodStartDate.get(ChronoField.YEAR_OF_ERA);
        int monthofYear = periodStartDate.getMonthValue();
        LocalDate date = DateUtils.getBusinessLocalDate();
        TreeSet<Integer> monthSet = new TreeSet<>();
        date = date.withMonth(previousMonth);
        monthSet.add(date.getMonthValue());
        int count = 0;
        while (count < (periodsInMonth - 1)) {
            date = date.plusMonths((12 / periodsInMonth));
            monthSet.add(date.getMonthValue());
            count++;
        }
        boolean notInRange = true;
        /*
         * if strt date is 2016-10-05 if financial year set to 4 then applicable month will be march and september that
         * is (3,9) if start date fall in month of oct,nov or dec then month will be 10, 11 or 12 so period end date
         * should be taken from next year for march month
         */

        for (Integer month : monthSet) {
            if (monthofYear <= month.intValue()) {
                periodEndDate = LocalDate.of(year, month, DateUtils.getBusinessLocalDate().withMonth(month).lengthOfMonth());
                notInRange = false;
                break;
            }
        }
        if (notInRange) {
            periodEndDate = LocalDate.of(year + 1, monthSet.first(),
                    DateUtils.getBusinessLocalDate().withMonth(monthSet.first()).lengthOfMonth());
        }
        return periodEndDate;
    }

    public boolean isInterestTransfered() {
        return this.interestTransfered;
    }

    public LocalDateInterval getPeriodInterval() {
        return this.periodInterval;
    }

    public boolean isUserPosting() {
        return this.isUserPosting;
    }

    public Integer getFinancialYearBeginningMonth() {
        return this.financialYearBeginningMonth;
    }

    // isInterestTransfer boolean is to identify newly created transaction is
    // interest transfer
    public static PostingPeriod createFrom(final LocalDateInterval periodInterval, final Money periodStartingBalance,
                                           final List<SavingsAccountTransaction> orderedListOfTransactions, final MonetaryCurrency currency,
                                           final SavingsCompoundingInterestPeriodType interestCompoundingPeriodType,
                                           final SavingsInterestCalculationType interestCalculationType, BigDecimal interestRateAsFraction, final long daysInYear,
                                           final LocalDate upToInterestCalculationDate, Collection<Long> interestPostTransactions, boolean isInterestTransfer,
                                           final Money minBalanceForInterestCalculation, final boolean isSavingsInterestPostingAtCurrentPeriodEnd,
                                           final BigDecimal overdraftInterestRateAsFraction, final Money minOverdraftForInterestCalculation, boolean isUserPosting,
                                           int financialYearBeginningMonth, final Boolean includePostingAndWithHoldTax, final SavingsAccount savingsAccount,
                                           final MathContext mc) {

        List<EndOfDayBalance> accountEndOfDayBalances = new ArrayList<>();
        boolean interestTransfered = false;
        Money openingDayBalance = periodStartingBalance;
        Money closeOfDayBalance = openingDayBalance;
        for (final SavingsAccountTransaction transaction : orderedListOfTransactions) {

            if (transaction.fallsWithin(periodInterval)) {
                // the balance of the transaction falls entirely within this
                // period so no need to do any cropping/bounding
                final EndOfDayBalance endOfDayBalance = transaction.toEndOfDayBalance(openingDayBalance, includePostingAndWithHoldTax);
                accountEndOfDayBalances.add(endOfDayBalance);

                openingDayBalance = endOfDayBalance.closingBalance();

            } else if (transaction.spansAnyPortionOf(periodInterval)) {
                final EndOfDayBalance endOfDayBalance = transaction.toEndOfDayBalanceBoundedBy(openingDayBalance, periodInterval,
                        includePostingAndWithHoldTax);
                accountEndOfDayBalances.add(endOfDayBalance);

                closeOfDayBalance = endOfDayBalance.closingBalance();
                openingDayBalance = closeOfDayBalance;
            }

            // this check is to make sure to add interest if withdrawal is
            // happened for already
            if (transaction.occursOn(periodInterval.endDate().plusDays(1))) {
                if (transaction.getId() == null) {
                    interestTransfered = isInterestTransfer;
                } else if (interestPostTransactions.contains(transaction.getId())) {
                    interestTransfered = true;
                }
            }

        }

        if (accountEndOfDayBalances.isEmpty()) {
            LocalDate balanceStartDate = periodInterval.startDate();
            LocalDate balanceEndDate = periodInterval.endDate();
            Integer numberOfDaysOfBalance = periodInterval.daysInPeriodInclusiveOfEndDate();

            if (balanceEndDate.isAfter(upToInterestCalculationDate)) {
                balanceEndDate = upToInterestCalculationDate;
                final LocalDateInterval spanOfBalance = LocalDateInterval.create(balanceStartDate, balanceEndDate);
                numberOfDaysOfBalance = spanOfBalance.daysInPeriodInclusiveOfEndDate();
            }

            final EndOfDayBalance endOfDayBalance = EndOfDayBalance.from(balanceStartDate, openingDayBalance, closeOfDayBalance,
                    numberOfDaysOfBalance);

            accountEndOfDayBalances.add(endOfDayBalance);

            closeOfDayBalance = endOfDayBalance.closingBalance();
            openingDayBalance = closeOfDayBalance;
        } else if (!orderedListOfTransactions.isEmpty()) {
            SavingsAccountTransaction firstTransaction = orderedListOfTransactions.get(0);
            if (firstTransaction.transactionLocalDate().isAfter(periodInterval.startDate())) {
                LocalDate balanceStartDate = periodInterval.startDate();
                LocalDate balanceEndDate = firstTransaction.transactionLocalDate();
                LocalDateInterval spanOfBalance = LocalDateInterval.create(balanceStartDate, balanceEndDate);
                Integer numberOfDaysOfBalance = spanOfBalance.daysBetween();

                final EndOfDayBalance endOfDayBalance = EndOfDayBalance.from(balanceStartDate, periodStartingBalance, periodStartingBalance,
                        numberOfDaysOfBalance);

                accountEndOfDayBalances.add(0, endOfDayBalance);
            }
        }
        accountEndOfDayBalances = flattenEndOfDayBalances(accountEndOfDayBalances, periodInterval);

        final List<CompoundingPeriod> compoundingPeriods = compoundingPeriodsInPostingPeriod(periodInterval, interestCompoundingPeriodType,
                accountEndOfDayBalances, upToInterestCalculationDate, financialYearBeginningMonth);

        return new PostingPeriod(periodInterval, currency, periodStartingBalance, openingDayBalance, interestCompoundingPeriodType,
                interestCalculationType, interestRateAsFraction, daysInYear, compoundingPeriods, interestTransfered,
                minBalanceForInterestCalculation, isSavingsInterestPostingAtCurrentPeriodEnd, overdraftInterestRateAsFraction,
                minOverdraftForInterestCalculation, isUserPosting, financialYearBeginningMonth, savingsAccount, mc);
    }

    private static List<EndOfDayBalance> flattenEndOfDayBalances(List<EndOfDayBalance> accountEndOfDayBalances,
                                                                 LocalDateInterval periodInterval) {
        List<EndOfDayBalance> endOfDayBalances = new ArrayList<>();
        if (!accountEndOfDayBalances.isEmpty()) {
            EndOfDayBalance endOfDayBalance = accountEndOfDayBalances.get(0);
            for (int i = 1; i < accountEndOfDayBalances.size(); i++) {
                EndOfDayBalance balance = accountEndOfDayBalances.get(i);
                if (endOfDayBalance.date().isEqual(balance.date())) {
                    endOfDayBalance.setNumberOfDays(endOfDayBalance.getNumberOfDays() + balance.getNumberOfDays());
                    endOfDayBalance.setEndOfDayBalance(balance.closingBalance());
                } else {
                    endOfDayBalances.add(endOfDayBalance);
                    endOfDayBalance = balance;
                }
            }
            endOfDayBalances.add(endOfDayBalance);
            for (int i = 0; i < endOfDayBalances.size() - 1; i++) {
                EndOfDayBalance balance = endOfDayBalances.get(i);
                EndOfDayBalance balance2 = endOfDayBalances.get(i + 1);
                LocalDateInterval spanOfBalance = LocalDateInterval.create(balance.date(), balance2.date());
                balance.setNumberOfDays(spanOfBalance.daysBetween());
            }
            EndOfDayBalance lastBalance = endOfDayBalances.get(endOfDayBalances.size() - 1);
            LocalDateInterval spanOfBalance = LocalDateInterval.create(lastBalance.date(), periodInterval.endDate());
            lastBalance.setNumberOfDays(spanOfBalance.daysInPeriodInclusiveOfEndDate());
        }

        return endOfDayBalances;
    }

    public List<BigDecimal> calculateInterests(final CompoundInterestValues compoundInterestValues) {
        List<BigDecimal> interestEarned = new ArrayList<>();
        log.info("** calculateInterests ** : {}", this.periodInterval);
        // for each compounding period accumulate the amount of interest
        // to be applied to the balanced for interest calculation
        for (final CompoundingPeriod compoundingPeriod : this.compoundingPeriods) {
            // The critical part: when calling calculateInterests on the compounding period,
            // if we're using NONE compounding, we should pass the ORIGINAL PRINCIPAL,
            // not the compounded interest
            final List<BigDecimal> interestUnrounded;
            if (SavingsCompoundingInterestPeriodType.NONE.equals(this.interestCompoundingType)) {
                // For NONE compounding, always calculate interest on the original principal (zero compounded interest)
                interestUnrounded = compoundingPeriod.calculateInterests(this.interestCompoundingType,
                        this.interestCalculationType, BigDecimal.ZERO, this.interestRateAsFraction,
                        this.daysInYear, this.minBalanceForInterestCalculation.getAmount(), this.overdraftInterestRateAsFraction,
                        this.minOverdraftForInterestCalculation.getAmount());
                log.info("NONE compounding: Interest calculated on original principal: {}", interestUnrounded);
            } else {
                // Normal compounding behavior - use compounded interest
                interestUnrounded = compoundingPeriod.calculateInterests(this.interestCompoundingType,
                        this.interestCalculationType, compoundInterestValues.getcompoundedInterest(), this.interestRateAsFraction,
                        this.daysInYear, this.minBalanceForInterestCalculation.getAmount(), this.overdraftInterestRateAsFraction,
                        this.minOverdraftForInterestCalculation.getAmount());
            }

            BigDecimal unCompoundedInterest = compoundInterestValues.getuncompoundedInterest();
            for (BigDecimal interest : interestUnrounded) {
                unCompoundedInterest = unCompoundedInterest.add(interest);
            }
            compoundInterestValues.setuncompoundedInterest(unCompoundedInterest);

            // Check if this is NONE compounding type - in this case we never compound the interest
            if (SavingsCompoundingInterestPeriodType.NONE.equals(this.interestCompoundingType)) {
                // For NONE compounding, we track the interest but never add it to the principal
                // DO NOT reset uncompoundedInterest - we want it to accumulate, but not compound
                log.debug("NONE compounding: Interest calculated but not compounded");
            } else {
                // Normal compounding behavior for other types
                LocalDate compoundingPeriodEndDate = compoundingPeriod.getPeriodInterval().endDate();
                if (!SavingsCompoundingInterestPeriodType.DAILY.equals(this.interestCompoundingType)) {
                    compoundingPeriodEndDate = determineInterestPeriodEndDateFrom(compoundingPeriod.getPeriodInterval().startDate(),
                            this.interestCompoundingType, compoundingPeriod.getPeriodInterval().endDate(),
                            this.getFinancialYearBeginningMonth());
                }

                if (compoundingPeriodEndDate.equals(compoundingPeriod.getPeriodInterval().endDate())) {
                    BigDecimal interestCompounded = compoundInterestValues.getcompoundedInterest().add(unCompoundedInterest);
                    compoundInterestValues.setcompoundedInterest(interestCompounded);
                    compoundInterestValues.setZeroForInterestToBeUncompounded();
                }
            }

            interestEarned.addAll(interestUnrounded);
        }

        this.interestEarnedUnroundeds = interestEarned;
        List<Money> interestRounded = new ArrayList<Money>();
        for (BigDecimal interest : this.interestEarnedUnroundeds) {
            interestRounded.add(Money.of(this.currency, interest));
        }
        this.interestEarnedRoundeds = interestRounded;

        log.info("Final interest list calculated: {}", interestEarned);

        return interestEarned;
    }

    public void setInterestEarned(List<Money> interestEarnedRounded) {
        this.interestEarnedRoundeds = interestEarnedRounded;
    }

    public List<CompoundingPeriod> getCompoundingPeriods() {
        return compoundingPeriods;
    }
}
