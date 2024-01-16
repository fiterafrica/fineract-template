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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.api.LoanApprovalMatrixConstants;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanApprovalMatrix;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecision;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecisionState;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDueDiligenceInfo;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanDecisionAssembler {

    private final CodeValueRepositoryWrapper codeValueRepository;

    public LoanDecision assembleFrom(final JsonCommand command, Loan loanId, AppUser currentUser) {

        LocalDate loanReviewOnDate = command.localDateValueOfParameterNamed(LoanApiConstants.loanReviewOnDateParameterName);
        final String noteText = command.stringValueOfParameterNamed("note");
        LoanDecision loanDecision = LoanDecision.reviewApplication(loanId, LoanDecisionState.REVIEW_APPLICATION.getValue(), noteText,
                Boolean.TRUE, Boolean.FALSE, loanReviewOnDate, currentUser);
        return loanDecision;
    }

    public LoanDecision assembleDueDiligenceFrom(final JsonCommand command, AppUser currentUser, LoanDecision savedLoanDecision) {

        LocalDate dueDiligenceOn = command.localDateValueOfParameterNamed(LoanApiConstants.dueDiligenceOnDateParameterName);
        final BigDecimal recommendedAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.dueDiligenceRecommendedAmountParameterName);
        final Integer termFrequency = command.integerValueOfParameterNamed(LoanApiConstants.recommendedLoanTermFrequencyParameterName);
        final Integer termPeriodFrequencyType = command.integerValueOfParameterNamed(LoanApiConstants.recommendedLoanTermFrequencyTypeParameterName);

        final String noteText = command.stringValueOfParameterNamed("note");

        LoanDecision loanDecision = savedLoanDecision;
        loanDecision.setLoanDecisionState(LoanDecisionState.DUE_DILIGENCE.getValue());
        loanDecision.setDueDiligenceNote(noteText);
        loanDecision.setDueDiligenceBy(currentUser);
        loanDecision.setDueDiligenceOn(dueDiligenceOn);
        loanDecision.setDueDiligenceSigned(Boolean.TRUE);
        loanDecision.setRejectDueDiligence(Boolean.FALSE);
        loanDecision.setDueDiligenceRecommendedAmount(recommendedAmount);
        loanDecision.setDueDiligenceTermFrequency(termFrequency);
        loanDecision.setDueDiligenceTermFrequencyType(termPeriodFrequencyType);
        return loanDecision;
    }

    public LoanDueDiligenceInfo assembleDueDiligenceDetailsFrom(final JsonCommand command, LoanDecision savedLoanDecision, Loan loan) {

        CodeValue surveyLocation = null;
        CodeValue cohort = null;
        CodeValue program = null;
        CodeValue country = null;

        final String surveyName = command.stringValueOfParameterNamed(LoanApiConstants.surveyNameParameterName);
        LocalDate startDate = command.localDateValueOfParameterNamed(LoanApiConstants.startDateParameterName);
        LocalDate endDate = command.localDateValueOfParameterNamed(LoanApiConstants.endDateParameterName);

        final Long surveyLocationId = command.longValueOfParameterNamed(LoanApiConstants.surveyLocationParameterName);
        if (surveyLocationId != null) {
            surveyLocation = this.codeValueRepository
                    .findOneByCodeNameAndIdWithNotFoundDetection(LoanApiConstants.surveyLocationCodeParameterName, surveyLocationId);
        }

        final Long cohortId = command.longValueOfParameterNamed(LoanApiConstants.cohortParameterName);
        if (cohortId != null) {
            cohort = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(LoanApiConstants.cohortCodeParameterName,
                    cohortId);
        }

        final Long programId = command.longValueOfParameterNamed(LoanApiConstants.programParameterName);
        if (programId != null) {
            program = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(LoanApiConstants.programCodeParameterName,
                    programId);
        }

        final Long countryId = command.longValueOfParameterNamed(LoanApiConstants.countryParameterName);
        if (countryId != null) {
            country = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(LoanApiConstants.countryCodeParameterName,
                    countryId);
        }

        return LoanDueDiligenceInfo.createNew(loan, savedLoanDecision, surveyName, startDate, endDate, surveyLocation, cohort, program,
                country);
    }

    public LoanDecision assembleCollateralReviewFrom(final JsonCommand command, AppUser currentUser, LoanDecision savedLoanDecision) {

        LocalDate collateralReviewOn = command.localDateValueOfParameterNamed(LoanApiConstants.collateralReviewOnDateParameterName);

        final String noteText = command.stringValueOfParameterNamed("note");

        LoanDecision loanDecision = savedLoanDecision;
        loanDecision.setLoanDecisionState(LoanDecisionState.COLLATERAL_REVIEW.getValue());
        loanDecision.setCollateralReviewNote(noteText);
        loanDecision.setCollateralReviewBy(currentUser);
        loanDecision.setCollateralReviewOn(collateralReviewOn);
        loanDecision.setCollateralReviewSigned(Boolean.TRUE);
        loanDecision.setRejectCollateralReviewSigned(Boolean.FALSE);
        return loanDecision;
    }

    public LoanApprovalMatrix assembleLoanApprovalMatrixFrom(final JsonCommand command) {

        final String currency = command.stringValueOfParameterNamed(LoanApprovalMatrixConstants.currencyParameterName);

        final BigDecimal levelOneUnsecuredFirstCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxAmount);
        final Integer levelOneUnsecuredFirstCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMinTerm);
        final Integer levelOneUnsecuredFirstCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxTerm);

        final BigDecimal levelOneUnsecuredSecondCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxAmount);
        final Integer levelOneUnsecuredSecondCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMinTerm);
        final Integer levelOneUnsecuredSecondCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxTerm);

        final BigDecimal levelOneSecuredFirstCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxAmount);
        final Integer levelOneSecuredFirstCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMinTerm);
        final Integer levelOneSecuredFirstCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxTerm);

        final BigDecimal levelOneSecuredSecondCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxAmount);
        final Integer levelOneSecuredSecondCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMinTerm);
        final Integer levelOneSecuredSecondCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxTerm);

        final BigDecimal levelTwoUnsecuredFirstCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxAmount);
        final Integer levelTwoUnsecuredFirstCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMinTerm);
        final Integer levelTwoUnsecuredFirstCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxTerm);

        final BigDecimal levelTwoUnsecuredSecondCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxAmount);
        final Integer levelTwoUnsecuredSecondCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMinTerm);
        final Integer levelTwoUnsecuredSecondCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxTerm);

        final BigDecimal levelTwoSecuredFirstCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxAmount);
        final Integer levelTwoSecuredFirstCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMinTerm);
        final Integer levelTwoSecuredFirstCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxTerm);

        final BigDecimal levelTwoSecuredSecondCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxAmount);
        final Integer levelTwoSecuredSecondCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMinTerm);
        final Integer levelTwoSecuredSecondCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxTerm);

        final BigDecimal levelThreeUnsecuredFirstCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxAmount);
        final Integer levelThreeUnsecuredFirstCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMinTerm);
        final Integer levelThreeUnsecuredFirstCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxTerm);

        final BigDecimal levelThreeUnsecuredSecondCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxAmount);
        final Integer levelThreeUnsecuredSecondCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMinTerm);
        final Integer levelThreeUnsecuredSecondCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxTerm);

        final BigDecimal levelThreeSecuredFirstCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxAmount);
        final Integer levelThreeSecuredFirstCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMinTerm);
        final Integer levelThreeSecuredFirstCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxTerm);

        final BigDecimal levelThreeSecuredSecondCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxAmount);
        final Integer levelThreeSecuredSecondCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMinTerm);
        final Integer levelThreeSecuredSecondCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxTerm);

        final BigDecimal levelFourUnsecuredFirstCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxAmount);
        final Integer levelFourUnsecuredFirstCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMinTerm);
        final Integer levelFourUnsecuredFirstCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxTerm);

        final BigDecimal levelFourUnsecuredSecondCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxAmount);
        final Integer levelFourUnsecuredSecondCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMinTerm);
        final Integer levelFourUnsecuredSecondCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxTerm);

        final BigDecimal levelFourSecuredFirstCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxAmount);
        final Integer levelFourSecuredFirstCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMinTerm);
        final Integer levelFourSecuredFirstCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxTerm);

        final BigDecimal levelFourSecuredSecondCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxAmount);
        final Integer levelFourSecuredSecondCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMinTerm);
        final Integer levelFourSecuredSecondCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxTerm);

        final BigDecimal levelFiveUnsecuredFirstCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxAmount);
        final Integer levelFiveUnsecuredFirstCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMinTerm);
        final Integer levelFiveUnsecuredFirstCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxTerm);

        final BigDecimal levelFiveUnsecuredSecondCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxAmount);
        final Integer levelFiveUnsecuredSecondCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMinTerm);
        final Integer levelFiveUnsecuredSecondCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxTerm);

        final BigDecimal levelFiveSecuredFirstCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxAmount);
        final Integer levelFiveSecuredFirstCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMinTerm);
        final Integer levelFiveSecuredFirstCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxTerm);

        final BigDecimal levelFiveSecuredSecondCycleMaxAmount = command
                .bigDecimalValueOfParameterNamed(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxAmount);
        final Integer levelFiveSecuredSecondCycleMinTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMinTerm);
        final Integer levelFiveSecuredSecondCycleMaxTerm = command
                .integerValueOfParameterNamed(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxTerm);

        return new LoanApprovalMatrix(currency, levelOneUnsecuredFirstCycleMaxAmount, levelOneUnsecuredFirstCycleMinTerm,
                levelOneUnsecuredFirstCycleMaxTerm, levelOneUnsecuredSecondCycleMaxAmount, levelOneUnsecuredSecondCycleMinTerm,
                levelOneUnsecuredSecondCycleMaxTerm, levelOneSecuredFirstCycleMaxAmount, levelOneSecuredFirstCycleMinTerm,
                levelOneSecuredFirstCycleMaxTerm, levelOneSecuredSecondCycleMaxAmount, levelOneSecuredSecondCycleMinTerm,
                levelOneSecuredSecondCycleMaxTerm, levelTwoUnsecuredFirstCycleMaxAmount, levelTwoUnsecuredFirstCycleMinTerm,
                levelTwoUnsecuredFirstCycleMaxTerm, levelTwoUnsecuredSecondCycleMaxAmount, levelTwoUnsecuredSecondCycleMinTerm,
                levelTwoUnsecuredSecondCycleMaxTerm, levelTwoSecuredFirstCycleMaxAmount, levelTwoSecuredFirstCycleMinTerm,
                levelTwoSecuredFirstCycleMaxTerm, levelTwoSecuredSecondCycleMaxAmount, levelTwoSecuredSecondCycleMinTerm,
                levelTwoSecuredSecondCycleMaxTerm, levelThreeUnsecuredFirstCycleMaxAmount, levelThreeUnsecuredFirstCycleMinTerm,
                levelThreeUnsecuredFirstCycleMaxTerm, levelThreeUnsecuredSecondCycleMaxAmount, levelThreeUnsecuredSecondCycleMinTerm,
                levelThreeUnsecuredSecondCycleMaxTerm, levelThreeSecuredFirstCycleMaxAmount, levelThreeSecuredFirstCycleMinTerm,
                levelThreeSecuredFirstCycleMaxTerm, levelThreeSecuredSecondCycleMaxAmount, levelThreeSecuredSecondCycleMinTerm,
                levelThreeSecuredSecondCycleMaxTerm, levelFourUnsecuredFirstCycleMaxAmount, levelFourUnsecuredFirstCycleMinTerm,
                levelFourUnsecuredFirstCycleMaxTerm, levelFourUnsecuredSecondCycleMaxAmount, levelFourUnsecuredSecondCycleMinTerm,
                levelFourUnsecuredSecondCycleMaxTerm, levelFourSecuredFirstCycleMaxAmount, levelFourSecuredFirstCycleMinTerm,
                levelFourSecuredFirstCycleMaxTerm, levelFourSecuredSecondCycleMaxAmount, levelFourSecuredSecondCycleMinTerm,
                levelFourSecuredSecondCycleMaxTerm, levelFiveUnsecuredFirstCycleMaxAmount, levelFiveUnsecuredFirstCycleMinTerm,
                levelFiveUnsecuredFirstCycleMaxTerm, levelFiveUnsecuredSecondCycleMaxAmount, levelFiveUnsecuredSecondCycleMinTerm,
                levelFiveUnsecuredSecondCycleMaxTerm, levelFiveSecuredFirstCycleMaxAmount, levelFiveSecuredFirstCycleMinTerm,
                levelFiveSecuredFirstCycleMaxTerm, levelFiveSecuredSecondCycleMaxAmount, levelFiveSecuredSecondCycleMinTerm,
                levelFiveSecuredSecondCycleMaxTerm);
    }

    public LoanDecision assembleIcReviewDecisionLevelOneFrom(final JsonCommand command, AppUser currentUser, LoanDecision savedLoanDecision,
            Boolean isReject, LocalDate icReviewOn, BigDecimal recommendedAmount, Integer termFrequency, Integer termPeriodFrequencyEnum) {

        final String noteText = command.stringValueOfParameterNamed("note");

        LoanDecision loanDecision = savedLoanDecision;

        loanDecision.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_ONE.getValue());
        loanDecision.setIcReviewDecisionLevelOneNote(noteText);
        loanDecision.setIcReviewDecisionLevelOneBy(currentUser);
        loanDecision.setIcReviewDecisionLevelOneOn(icReviewOn);
        if(recommendedAmount != null) {
            loanDecision.setIcReviewDecisionLevelOneRecommendedAmount(recommendedAmount);
        }
        if(termFrequency != null) {
            loanDecision.setIcReviewDecisionLevelOneTermFrequency(termFrequency);
        }
        if(termPeriodFrequencyEnum != null) {
            loanDecision.setIcReviewDecisionLevelOneTermPeriodFrequencyEnum(termPeriodFrequencyEnum);
        }

        if (isReject) {
            loanDecision.setIcReviewDecisionLevelOneSigned(Boolean.FALSE);
            loanDecision.setRejectIcReviewDecisionLevelOneSigned(Boolean.TRUE);
            return loanDecision;
        } else {
            loanDecision.setIcReviewDecisionLevelOneSigned(Boolean.TRUE);
            loanDecision.setRejectIcReviewDecisionLevelOneSigned(Boolean.FALSE);
            return loanDecision;
        }
    }

    public LoanDecision assembleIcReviewDecisionLevelTwoFrom(final JsonCommand command, AppUser currentUser, LoanDecision savedLoanDecision,
            Boolean isReject, LocalDate icReviewOn, BigDecimal recommendedAmount, Integer termFrequency, Integer termPeriodFrequencyEnum) {

        final String noteText = command.stringValueOfParameterNamed("note");
        LoanDecision loanDecision = savedLoanDecision;

        loanDecision.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_TWO.getValue());
        loanDecision.setIcReviewDecisionLevelTwoNote(noteText);
        loanDecision.setIcReviewDecisionLevelTwoBy(currentUser);
        loanDecision.setIcReviewDecisionLevelTwoOn(icReviewOn);
        if(recommendedAmount != null) {
            loanDecision.setIcReviewDecisionLevelTwoRecommendedAmount(recommendedAmount);
        }
        if(termFrequency != null) {
            loanDecision.setIcReviewDecisionLevelTwoTermFrequency(termFrequency);
        }
        if(termPeriodFrequencyEnum != null) {
            loanDecision.setIcReviewDecisionLevelTwoTermPeriodFrequencyEnum(termPeriodFrequencyEnum);
        }

        if (isReject) {
            loanDecision.setIcReviewDecisionLevelTwoSigned(Boolean.FALSE);
            loanDecision.setRejectIcReviewDecisionLevelTwoSigned(Boolean.TRUE);
        } else {
            loanDecision.setIcReviewDecisionLevelTwoSigned(Boolean.TRUE);
            loanDecision.setRejectIcReviewDecisionLevelTwoSigned(Boolean.FALSE);
        }

        return loanDecision;
    }

    public LoanDecision assembleIcReviewDecisionLevelThreeFrom(final JsonCommand command, AppUser currentUser, LoanDecision savedLoanDecision,
            Boolean isReject, LocalDate icReviewOn, BigDecimal recommendedAmount, Integer termFrequency, Integer termPeriodFrequencyEnum) {

        final String noteText = command.stringValueOfParameterNamed("note");
        LoanDecision loanDecision = savedLoanDecision;

        loanDecision.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_THREE.getValue());
        loanDecision.setIcReviewDecisionLevelThreeNote(noteText);
        loanDecision.setIcReviewDecisionLevelThreeBy(currentUser);
        loanDecision.setIcReviewDecisionLevelThreeOn(icReviewOn);
        if(recommendedAmount != null) {
            loanDecision.setIcReviewDecisionLevelThreeRecommendedAmount(recommendedAmount);
        }
        if(termFrequency != null) {
            loanDecision.setIcReviewDecisionLevelThreeTermFrequency(termFrequency);
        }
        if(termPeriodFrequencyEnum != null) {
            loanDecision.setIcReviewDecisionLevelThreeTermPeriodFrequencyEnum(termPeriodFrequencyEnum);
        }

        if (isReject) {
            loanDecision.setIcReviewDecisionLevelThreeSigned(Boolean.FALSE);
            loanDecision.setRejectIcReviewDecisionLevelThreeSigned(Boolean.TRUE);
        } else {
            loanDecision.setIcReviewDecisionLevelThreeSigned(Boolean.TRUE);
            loanDecision.setRejectIcReviewDecisionLevelThreeSigned(Boolean.FALSE);
        }

        return loanDecision;
    }

    public LoanDecision assembleIcReviewDecisionLevelFourFrom(final JsonCommand command, AppUser currentUser, LoanDecision savedLoanDecision,
            Boolean isReject, LocalDate icReviewOn, BigDecimal recommendedAmount, Integer termFrequency, Integer termPeriodFrequencyEnum) {

        final String noteText = command.stringValueOfParameterNamed("note");

        LoanDecision loanDecision = savedLoanDecision;

        loanDecision.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_FOUR.getValue());
        loanDecision.setIcReviewDecisionLevelFourNote(noteText);
        loanDecision.setIcReviewDecisionLevelFourBy(currentUser);
        loanDecision.setIcReviewDecisionLevelFourOn(icReviewOn);
        if(recommendedAmount != null) {
            loanDecision.setIcReviewDecisionLevelFourRecommendedAmount(recommendedAmount);
        }
        if(termFrequency != null) {
            loanDecision.setIcReviewDecisionLevelFourTermFrequency(termFrequency);
        }
        if(termPeriodFrequencyEnum != null) {
            loanDecision.setIcReviewDecisionLevelFourTermPeriodFrequencyEnum(termPeriodFrequencyEnum);
        }

        if (isReject) {
            loanDecision.setIcReviewDecisionLevelFourSigned(Boolean.FALSE);
            loanDecision.setRejectIcReviewDecisionLevelFourSigned(Boolean.TRUE);
        } else {
            loanDecision.setIcReviewDecisionLevelFourSigned(Boolean.TRUE);
            loanDecision.setRejectIcReviewDecisionLevelFourSigned(Boolean.FALSE);
        }

        return loanDecision;
    }

    public LoanDecision assembleIcReviewDecisionLevelFiveFrom(final JsonCommand command, AppUser currentUser, LoanDecision savedLoanDecision,
            Boolean isReject, LocalDate icReviewOn, BigDecimal recommendedAmount, Integer termFrequency, Integer termPeriodFrequencyEnum) {

        final String noteText = command.stringValueOfParameterNamed("note");
        LoanDecision loanDecision = savedLoanDecision;

        loanDecision.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_FIVE.getValue());
        loanDecision.setNextLoanIcReviewDecisionState(LoanDecisionState.PREPARE_AND_SIGN_CONTRACT.getValue());
        loanDecision.setIcReviewDecisionLevelFiveNote(noteText);
        loanDecision.setIcReviewDecisionLevelFiveBy(currentUser);
        loanDecision.setIcReviewDecisionLevelFiveOn(icReviewOn);
        if(recommendedAmount != null) {
            loanDecision.setIcReviewDecisionLevelFiveRecommendedAmount(recommendedAmount);
        }
        if(termFrequency != null) {
            loanDecision.setIcReviewDecisionLevelFiveTermFrequency(termFrequency);
        }
        if(termPeriodFrequencyEnum != null) {
            loanDecision.setIcReviewDecisionLevelFiveTermPeriodFrequencyEnum(termPeriodFrequencyEnum);
        }

        if (isReject) {
            loanDecision.setIcReviewDecisionLevelFiveSigned(Boolean.FALSE);
            loanDecision.setRejectIcReviewDecisionLevelFiveSigned(Boolean.TRUE);
        } else {
            loanDecision.setIcReviewDecisionLevelFiveSigned(Boolean.TRUE);
            loanDecision.setRejectIcReviewDecisionLevelFiveSigned(Boolean.FALSE);
        }

        return loanDecision;
    }

    public LoanDecision assemblePrepareAndSignContractFrom(final JsonCommand command, AppUser currentUser, LoanDecision savedLoanDecision) {

        LocalDate icReviewOn = command.localDateValueOfParameterNamed(LoanApiConstants.icReviewOnDateParameterName);

        final String noteText = command.stringValueOfParameterNamed("note");

        LoanDecision loanDecision = savedLoanDecision;
        loanDecision.setLoanDecisionState(LoanDecisionState.PREPARE_AND_SIGN_CONTRACT.getValue());
        loanDecision.setPrepareAndSignContractNote(noteText);
        loanDecision.setPrepareAndSignContractBy(currentUser);
        loanDecision.setPrepareAndSignContractOn(icReviewOn);
        loanDecision.setPrepareAndSignContractSigned(Boolean.TRUE);
        loanDecision.setRejectPrepareAndSignContractSigned(Boolean.FALSE);
        return loanDecision;
    }
}
