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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.data;

import java.time.LocalDate;
import java.util.Collection;

import lombok.Getter;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;

/**
 * Immutable data object representing loan reschedule request data.
 **/
@Getter
public final class LoanRescheduleRequestData {

    private final Long id;
    private final Long loanId;
    private final Long clientId;
    private final String clientName;
    private final String loanAccountNumber;
    private final LoanRescheduleRequestStatusEnumData statusEnum;
    private final Integer rescheduleFromInstallment;
    private final LocalDate rescheduleFromDate;
    private final Boolean recalculateInterest;
    private final Boolean adjustFuturePayments;
    private final CodeValueData rescheduleReasonCodeValue;
    private final LoanRescheduleRequestTimelineData timeline;
    private final String rescheduleReasonComment;
    private LoanTransactionData loanTransactionData;
    private CodeValueData codeValueData;
    @SuppressWarnings("unused")
    private final Collection<CodeValueData> rescheduleReasons;
    @SuppressWarnings("unused")
    private final Collection<LoanTermVariationsData> loanTermVariationsData;
    private final Collection<ChargeData> availableCarryForwardCharges;

    private final Collection<CodeValueData> overdueChargeHandlingOptions;

    private final CodeValueData overdueChargeHandling;

    private final Long carryForwardChargeId;

    /**
     * LoanRescheduleRequestData constructor
     *
     * @param loanTermVariationsData       TODO
     * @param loanTransactionData
     * @param adjustFuturePayments
     * @param availableCarryForwardCharges
     **/
    private LoanRescheduleRequestData(Long id, Long loanId, LoanRescheduleRequestStatusEnumData statusEnum,
                                      Integer rescheduleFromInstallment, LocalDate rescheduleFromDate, CodeValueData rescheduleReasonCodeValue,
                                      String rescheduleReasonComment, LoanRescheduleRequestTimelineData timeline, final String clientName,
                                      final String loanAccountNumber, final Long clientId, final Boolean recalculateInterest,
                                      Collection<CodeValueData> rescheduleReasons, final Collection<LoanTermVariationsData> loanTermVariationsData,
                                      LoanTransactionData loanTransactionData, boolean adjustFuturePayments, Collection<ChargeData> availableCarryForwardCharges, Collection<CodeValueData> overdueChargeHandlingOptions, CodeValueData overdueChargeHandling, Long carryForwardChargeId) {

        this.id = id;
        this.loanId = loanId;
        this.statusEnum = statusEnum;
        this.rescheduleFromInstallment = rescheduleFromInstallment;
        this.rescheduleFromDate = rescheduleFromDate;
        this.rescheduleReasonCodeValue = rescheduleReasonCodeValue;
        this.rescheduleReasonComment = rescheduleReasonComment;
        this.timeline = timeline;
        this.clientName = clientName;
        this.loanAccountNumber = loanAccountNumber;
        this.clientId = clientId;
        this.recalculateInterest = recalculateInterest;
        this.rescheduleReasons = rescheduleReasons;
        this.loanTermVariationsData = loanTermVariationsData;
        this.loanTransactionData = loanTransactionData;
        this.adjustFuturePayments = adjustFuturePayments;
        this.availableCarryForwardCharges = availableCarryForwardCharges;
        this.overdueChargeHandlingOptions = overdueChargeHandlingOptions;
        this.overdueChargeHandling = overdueChargeHandling;
        this.carryForwardChargeId = carryForwardChargeId;
    }

    /**
     * @param loanTermVariationsData
     *            TODO
     * @return an instance of the LoanRescheduleRequestData class
     **/
    public static LoanRescheduleRequestData instance(Long id, Long loanId, LoanRescheduleRequestStatusEnumData statusEnum,
                                                     Integer rescheduleFromInstallment, LocalDate rescheduleFromDate, CodeValueData rescheduleReasonCodeValue,
                                                     String rescheduleReasonComment, LoanRescheduleRequestTimelineData timeline, final String clientName,
                                                     final String loanAccountNumber, final Long clientId, final Boolean recalculateInterest,
                                                     Collection<CodeValueData> rescheduleReasons, final Collection<LoanTermVariationsData> loanTermVariationsData,
                                                     final LoanTransactionData loanTransactionData, final boolean adjustFuturePayments,
                                                     Collection<ChargeData> availableCarryForwardCharges,
                                                     Collection<CodeValueData> overdueChargeHandlingOptions,
                                                     CodeValueData overdueChargeHandling,
                                                     Long carryForwardChargeId) {

        return new LoanRescheduleRequestData(id, loanId, statusEnum, rescheduleFromInstallment, rescheduleFromDate,
                rescheduleReasonCodeValue, rescheduleReasonComment, timeline, clientName, loanAccountNumber, clientId, recalculateInterest,
                rescheduleReasons, loanTermVariationsData, loanTransactionData, adjustFuturePayments,
                availableCarryForwardCharges, overdueChargeHandlingOptions, overdueChargeHandling, carryForwardChargeId);
    }

    /**
     * LoanRescheduleRequestData constructor
     *
     **/
    private LoanRescheduleRequestData(Long id, Long loanId, LoanRescheduleRequestStatusEnumData statusEnum, final String clientName,
                                      final String loanAccountNumber, final Long clientId, final LocalDate rescheduleFromDate,
                                      final CodeValueData rescheduleReasonCodeValue) {

        this.id = id;
        this.loanId = loanId;
        this.statusEnum = statusEnum;
        this.clientName = clientName;
        this.loanAccountNumber = loanAccountNumber;
        this.clientId = clientId;
        this.rescheduleFromDate = rescheduleFromDate;
        this.rescheduleReasonCodeValue = rescheduleReasonCodeValue;
        this.availableCarryForwardCharges = null;
        this.overdueChargeHandlingOptions = null;
        this.overdueChargeHandling = null;
        this.carryForwardChargeId = null;
        this.rescheduleFromInstallment = null;
        this.rescheduleReasonComment = null;
        this.timeline = null;
        this.recalculateInterest = null;
        this.rescheduleReasons = null;
        this.loanTermVariationsData = null;
        this.loanTransactionData = null;
        this.adjustFuturePayments = null;
    }

    /**
     * @return an instance of the LoanRescheduleRequestData class
     **/
    public static LoanRescheduleRequestData instance(Long id, Long loanId, LoanRescheduleRequestStatusEnumData statusEnum,
            final String clientName, final String loanAccountNumber, final Long clientId, final LocalDate rescheduleFromDate,
            final CodeValueData rescheduleReasonCodeValue) {

        return new LoanRescheduleRequestData(id, loanId, statusEnum, clientName, loanAccountNumber, clientId, rescheduleFromDate,
                rescheduleReasonCodeValue);
    }


    /**
     * @return the recalculateInterest
     */
    public Boolean getRecalculateInterest() {
        boolean value = false;

        if (recalculateInterest != null) {
            value = recalculateInterest;
        }

        return value;
    }

    public void updateLoanTransactionData(LoanTransactionData loanTransactionData) {
        this.loanTransactionData = loanTransactionData;
    }


}
