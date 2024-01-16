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
package org.apache.fineract.portfolio.loanaccount.serialization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.api.LoanApprovalMatrixConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class LoanDecisionTransitionApiJsonValidator {

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public LoanDecisionTransitionApiJsonValidator(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }

    public void validateApplicationReview(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Set<String> disbursementParameters = new HashSet<>(
                Arrays.asList(LoanApiConstants.loanId, LoanApiConstants.loanReviewOnDateParameterName, LoanApiConstants.noteParameterName,
                        LoanApiConstants.localeParameterName, LoanApiConstants.dateFormatParameterName));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, disbursementParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loanDecisionEngine");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final LocalDate loanReviewOnDate = this.fromApiJsonHelper.extractLocalDateNamed(LoanApiConstants.loanReviewOnDateParameterName,
                element);
        baseDataValidator.reset().parameter(LoanApiConstants.loanReviewOnDateParameterName).value(loanReviewOnDate).notNull();

        final String note = this.fromApiJsonHelper.extractStringNamed(LoanApiConstants.noteParameterName, element);
        baseDataValidator.reset().parameter(LoanApiConstants.noteParameterName).value(note).notExceedingLengthOf(1000).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateDueDiligence(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Set<String> disbursementParameters = new HashSet<>(Arrays.asList(LoanApiConstants.loanId,
                LoanApiConstants.loanReviewOnDateParameterName, LoanApiConstants.noteParameterName, LoanApiConstants.localeParameterName,
                LoanApiConstants.dateFormatParameterName, LoanApiConstants.dueDiligenceOnDateParameterName,LoanApiConstants.dueDiligenceRecommendedAmountParameterName,
                LoanApiConstants.recommendedLoanTermFrequencyParameterName, LoanApiConstants.recommendedLoanTermFrequencyTypeParameterName,
                LoanApiConstants.isIdeaClientParamName));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, disbursementParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loanDecisionEngine");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final LocalDate dueDiligenceOn = this.fromApiJsonHelper.extractLocalDateNamed(LoanApiConstants.dueDiligenceOnDateParameterName,
                element);
        baseDataValidator.reset().parameter(LoanApiConstants.loanReviewOnDateParameterName).value(dueDiligenceOn).notNull();

        final String note = this.fromApiJsonHelper.extractStringNamed(LoanApiConstants.noteParameterName, element);
        baseDataValidator.reset().parameter(LoanApiConstants.noteParameterName).value(note).notExceedingLengthOf(1000).notNull();

        final BigDecimal recommendedAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(LoanApiConstants.dueDiligenceRecommendedAmountParameterName, element);
        baseDataValidator.reset().parameter(LoanApiConstants.dueDiligenceRecommendedAmountParameterName).value(recommendedAmount).notNull()
                .integerGreaterThanZero();

        final Long recommendedLoanTermFrequency = this.fromApiJsonHelper.extractLongNamed(LoanApiConstants.recommendedLoanTermFrequencyParameterName, element);
        baseDataValidator.reset().parameter(LoanApiConstants.recommendedLoanTermFrequencyParameterName).value(recommendedLoanTermFrequency).notNull()
                .integerGreaterThanZero();

        final Long recommendedLoanTermFrequencyType = this.fromApiJsonHelper.extractLongNamed(LoanApiConstants.recommendedLoanTermFrequencyTypeParameterName, element);
        baseDataValidator.reset().parameter(LoanApiConstants.recommendedLoanTermFrequencyTypeParameterName).value(recommendedLoanTermFrequencyType).notNull()
                .integerGreaterThanZero();

        Boolean isIdeaClient = this.fromApiJsonHelper.extractBooleanNamed(LoanApiConstants.isIdeaClientParamName, element);
        if (isIdeaClient == null)
            isIdeaClient = Boolean.FALSE;
        baseDataValidator.reset().parameter(LoanApiConstants.isIdeaClientParamName).value(isIdeaClient).notNull()
                .validateForBooleanValue();


        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateCollateralReview(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Set<String> disbursementParameters = new HashSet<>(Arrays.asList(LoanApiConstants.loanId,
                LoanApiConstants.collateralReviewOnDateParameterName, LoanApiConstants.noteParameterName,
                LoanApiConstants.localeParameterName, LoanApiConstants.dateFormatParameterName));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, disbursementParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loanDecisionEngine");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final LocalDate collateralReviewOn = this.fromApiJsonHelper
                .extractLocalDateNamed(LoanApiConstants.collateralReviewOnDateParameterName, element);
        baseDataValidator.reset().parameter(LoanApiConstants.collateralReviewOnDateParameterName).value(collateralReviewOn).notNull();

        final String note = this.fromApiJsonHelper.extractStringNamed(LoanApiConstants.noteParameterName, element);
        baseDataValidator.reset().parameter(LoanApiConstants.noteParameterName).value(note).notExceedingLengthOf(1000).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateCreateApprovalMatrix(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Set<String> disbursementParameters = new HashSet<>(Arrays.asList(LoanApprovalMatrixConstants.currencyParameterName,
                LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMinTerm, LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMinTerm, LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxTerm, LoanApiConstants.localeParameterName));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, disbursementParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loanApprovalMatrix");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String currency = this.fromApiJsonHelper.extractStringNamed(LoanApprovalMatrixConstants.currencyParameterName, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.currencyParameterName).value(currency).notExceedingLengthOf(10)
                .notNull();

        final BigDecimal levelOneUnsecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxAmount)
                .value(levelOneUnsecuredFirstCycleMaxAmount).positiveAmount().notNull();

        final Integer levelOneUnsecuredFirstCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMinTerm)
                .value(levelOneUnsecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelOneUnsecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxTerm)
                .value(levelOneUnsecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelOneUnsecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxAmount)
                .value(levelOneUnsecuredSecondCycleMaxAmount).positiveAmount().notNull();

        final Integer levelOneUnsecuredSecondCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMinTerm)
                .value(levelOneUnsecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelOneUnsecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxTerm)
                .value(levelOneUnsecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelOneSecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxAmount)
                .value(levelOneSecuredFirstCycleMaxAmount).positiveAmount().notNull();

        final Integer levelOneSecuredFirstCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMinTerm)
                .value(levelOneSecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelOneSecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxTerm)
                .value(levelOneSecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelOneSecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxAmount)
                .value(levelOneSecuredSecondCycleMaxAmount).positiveAmount().notNull();

        final Integer levelOneSecuredSecondCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMinTerm)
                .value(levelOneSecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelOneSecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxTerm)
                .value(levelOneSecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelTwoUnsecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxAmount)
                .value(levelTwoUnsecuredFirstCycleMaxAmount).positiveAmount().notNull();

        final Integer levelTwoUnsecuredFirstCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMinTerm)
                .value(levelTwoUnsecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelTwoUnsecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxTerm)
                .value(levelTwoUnsecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelTwoUnsecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxAmount)
                .value(levelTwoUnsecuredSecondCycleMaxAmount).positiveAmount().notNull();

        final Integer levelTwoUnsecuredSecondCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMinTerm)
                .value(levelTwoUnsecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelTwoUnsecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxTerm)
                .value(levelTwoUnsecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelTwoSecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxAmount)
                .value(levelTwoSecuredFirstCycleMaxAmount).positiveAmount().notNull();

        final Integer levelTwoSecuredFirstCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMinTerm)
                .value(levelTwoSecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelTwoSecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxTerm)
                .value(levelTwoSecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelTwoSecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxAmount)
                .value(levelTwoSecuredSecondCycleMaxAmount).positiveAmount().notNull();

        final Integer levelTwoSecuredSecondCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMinTerm)
                .value(levelTwoSecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelTwoSecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxTerm)
                .value(levelTwoSecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelThreeUnsecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxAmount)
                .value(levelThreeUnsecuredFirstCycleMaxAmount).positiveAmount().notNull();

        final Integer levelThreeUnsecuredFirstCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMinTerm)
                .value(levelThreeUnsecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelThreeUnsecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxTerm)
                .value(levelThreeUnsecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelThreeUnsecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxAmount)
                .value(levelThreeUnsecuredSecondCycleMaxAmount).positiveAmount().notNull();

        final Integer levelThreeUnsecuredSecondCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMinTerm)
                .value(levelThreeUnsecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelThreeUnsecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxTerm)
                .value(levelThreeUnsecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelThreeSecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxAmount)
                .value(levelThreeSecuredFirstCycleMaxAmount).positiveAmount().notNull();

        final Integer levelThreeSecuredFirstCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMinTerm)
                .value(levelThreeSecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelThreeSecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxTerm)
                .value(levelThreeSecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelThreeSecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxAmount)
                .value(levelThreeSecuredSecondCycleMaxAmount).positiveAmount().notNull();

        final Integer levelThreeSecuredSecondCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMinTerm)
                .value(levelThreeSecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelThreeSecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxTerm)
                .value(levelThreeSecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelFourUnsecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxAmount)
                .value(levelFourUnsecuredFirstCycleMaxAmount).positiveAmount().notNull();

        final Integer levelFourUnsecuredFirstCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMinTerm)
                .value(levelFourUnsecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelFourUnsecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxTerm)
                .value(levelFourUnsecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelFourUnsecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxAmount)
                .value(levelFourUnsecuredSecondCycleMaxAmount).positiveAmount().notNull();

        final Integer levelFourUnsecuredSecondCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMinTerm)
                .value(levelFourUnsecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelFourUnsecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxTerm)
                .value(levelFourUnsecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelFourSecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxAmount)
                .value(levelFourSecuredFirstCycleMaxAmount).positiveAmount().notNull();

        final Integer levelFourSecuredFirstCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMinTerm)
                .value(levelFourSecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelFourSecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxTerm)
                .value(levelFourSecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelFourSecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxAmount)
                .value(levelFourSecuredSecondCycleMaxAmount).positiveAmount().notNull();

        final Integer levelFourSecuredSecondCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMinTerm)
                .value(levelFourSecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelFourSecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxTerm)
                .value(levelFourSecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelFiveUnsecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxAmount)
                .value(levelFiveUnsecuredFirstCycleMaxAmount).positiveAmount().notNull();

        final Integer levelFiveUnsecuredFirstCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMinTerm)
                .value(levelFiveUnsecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelFiveUnsecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxTerm)
                .value(levelFiveUnsecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelFiveUnsecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxAmount)
                .value(levelFiveUnsecuredSecondCycleMaxAmount).positiveAmount().notNull();

        final Integer levelFiveUnsecuredSecondCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMinTerm)
                .value(levelFiveUnsecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelFiveUnsecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxTerm)
                .value(levelFiveUnsecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelFiveSecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxAmount)
                .value(levelFiveSecuredFirstCycleMaxAmount).positiveAmount().notNull();

        final Integer levelFiveSecuredFirstCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMinTerm)
                .value(levelFiveSecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelFiveSecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxTerm)
                .value(levelFiveSecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();

        final BigDecimal levelFiveSecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxAmount, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxAmount)
                .value(levelFiveSecuredSecondCycleMaxAmount).positiveAmount().notNull();

        final Integer levelFiveSecuredSecondCycleMinTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMinTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMinTerm)
                .value(levelFiveSecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();

        final Integer levelFiveSecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxTerm, element);
        baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxTerm)
                .value(levelFiveSecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();

        // Lower levels amounts should not be greater than upper levels

        validateAmountsUnsecuredFirstCycle(levelOneUnsecuredFirstCycleMaxAmount, levelTwoUnsecuredFirstCycleMaxAmount,
                levelThreeUnsecuredFirstCycleMaxAmount, levelFourUnsecuredFirstCycleMaxAmount, levelFiveUnsecuredFirstCycleMaxAmount);

        validateAmountsUnsecuredSecondCycle(levelOneUnsecuredSecondCycleMaxAmount, levelTwoUnsecuredSecondCycleMaxAmount,
                levelThreeUnsecuredSecondCycleMaxAmount, levelFourUnsecuredSecondCycleMaxAmount, levelFiveUnsecuredSecondCycleMaxAmount);

        validateAmountsSecuredFirstCycle(levelOneSecuredFirstCycleMaxAmount, levelTwoSecuredFirstCycleMaxAmount,
                levelThreeSecuredFirstCycleMaxAmount, levelFourSecuredFirstCycleMaxAmount, levelFiveSecuredFirstCycleMaxAmount);

        validateAmountsSecuredSecondCycle(levelOneSecuredSecondCycleMaxAmount, levelTwoSecuredSecondCycleMaxAmount,
                levelThreeSecuredSecondCycleMaxAmount, levelFourSecuredSecondCycleMaxAmount, levelFiveSecuredSecondCycleMaxAmount);

        // Level One
        loanTermValidation(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMinTerm, levelOneUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxTerm, levelOneUnsecuredFirstCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMinTerm, levelOneUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxTerm, levelOneUnsecuredSecondCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMinTerm, levelOneSecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxTerm, levelOneSecuredFirstCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMinTerm, levelOneSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxTerm, levelOneSecuredSecondCycleMaxTerm);

        // Level Two
        loanTermValidation(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMinTerm, levelTwoUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxTerm, levelTwoUnsecuredFirstCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMinTerm, levelTwoUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxTerm, levelTwoUnsecuredSecondCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMinTerm, levelTwoSecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxTerm, levelTwoSecuredFirstCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMinTerm, levelTwoSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxTerm, levelTwoSecuredSecondCycleMaxTerm);

        // Level Three
        loanTermValidation(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMinTerm, levelThreeUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxTerm, levelThreeUnsecuredFirstCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMinTerm, levelThreeUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxTerm, levelThreeUnsecuredSecondCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMinTerm, levelThreeSecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxTerm, levelThreeSecuredFirstCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMinTerm, levelThreeSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxTerm, levelThreeSecuredSecondCycleMaxTerm);

        // Level Four
        loanTermValidation(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMinTerm, levelFourUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxTerm, levelFourUnsecuredFirstCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMinTerm, levelFourUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxTerm, levelFourUnsecuredSecondCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMinTerm, levelFourSecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxTerm, levelFourSecuredFirstCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMinTerm, levelFourSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxTerm, levelFourSecuredSecondCycleMaxTerm);

        // Level Five
        loanTermValidation(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMinTerm, levelFiveUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxTerm, levelFiveUnsecuredFirstCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMinTerm, levelFiveUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxTerm, levelFiveUnsecuredSecondCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMinTerm, levelFiveSecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxTerm, levelFiveSecuredFirstCycleMaxTerm);
        loanTermValidation(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMinTerm, levelFiveSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxTerm, levelFiveSecuredSecondCycleMaxTerm);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private static void loanTermValidation(String minimumLoanTermName, Integer minTerm, String maximumLoanTermName, Integer maxTerm) {
        if (minTerm > maxTerm) {
            throw new GeneralPlatformDomainRuleException("error.msg.minimum.loan.term.should.not.be.greater.than.maximum.loan.term",
                    String.format("Minimum Loan Term - %s - [%s] should not be greater than maximum Loan Term - %s -  [%s] ",
                            minimumLoanTermName, minTerm, maximumLoanTermName, maxTerm));
        }
    }

    private static void validateAmountsUnsecuredFirstCycle(BigDecimal levelOneUnsecuredFirstCycleMaxAmount,
            BigDecimal levelTwoUnsecuredFirstCycleMaxAmount, BigDecimal levelThreeUnsecuredFirstCycleMaxAmount,
            BigDecimal levelFourUnsecuredFirstCycleMaxAmount, BigDecimal levelFiveUnsecuredFirstCycleMaxAmount) {
        if (levelOneUnsecuredFirstCycleMaxAmount.compareTo(levelTwoUnsecuredFirstCycleMaxAmount) >= 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.one.unsecure.first.cycle.should.not.be.greater.than.level.two.unsecure.first.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level one [%s]  unsecure first cycle should not be greater than for level two [%s] unsecure first cycle",
                            levelOneUnsecuredFirstCycleMaxAmount, levelTwoUnsecuredFirstCycleMaxAmount));

        }
        if (levelTwoUnsecuredFirstCycleMaxAmount.compareTo(levelThreeUnsecuredFirstCycleMaxAmount) >= 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.two.unsecure.first.cycle.should.not.be.greater.than.level.three.unsecure.first.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level two [%s] unsecure first cycle should not be greater than for level three [%s] unsecure first cycle",
                            levelTwoUnsecuredFirstCycleMaxAmount, levelThreeUnsecuredFirstCycleMaxAmount));

        }

        if (levelThreeUnsecuredFirstCycleMaxAmount.compareTo(levelFourUnsecuredFirstCycleMaxAmount) >= 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.three.unsecure.first.cycle.should.not.be.greater.than.level.four.unsecure.first.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level three [%s] unsecure first cycle should not be greater than for level four [%s] unsecure first cycle",
                            levelThreeUnsecuredFirstCycleMaxAmount, levelFourUnsecuredFirstCycleMaxAmount));

        }
        if (levelFourUnsecuredFirstCycleMaxAmount.compareTo(levelFiveUnsecuredFirstCycleMaxAmount) > 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.four.unsecure.first.cycle.should.not.be.greater.than.level.five.unsecure.first.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level four [%s] unsecure first cycle should not be greater than for level five [%s] unsecure first cycle",
                            levelFourUnsecuredFirstCycleMaxAmount, levelFiveUnsecuredFirstCycleMaxAmount));

        }
    }

    private static void validateAmountsUnsecuredSecondCycle(BigDecimal levelOneUnsecuredSecondCycleMaxAmount,
            BigDecimal levelTwoUnsecuredSecondCycleMaxAmount, BigDecimal levelThreeUnsecuredSecondCycleMaxAmount,
            BigDecimal levelFourUnsecuredSecondCycleMaxAmount, BigDecimal levelFiveUnsecuredSecondCycleMaxAmount) {
        if (levelOneUnsecuredSecondCycleMaxAmount.compareTo(levelTwoUnsecuredSecondCycleMaxAmount) >= 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.one.unsecure.second.cycle.should.not.be.greater.than.level.two.unsecure.second.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level one [%s] unsecure Second cycle should not be greater than for level two [%s] unsecure Second cycle",
                            levelOneUnsecuredSecondCycleMaxAmount, levelTwoUnsecuredSecondCycleMaxAmount));

        }
        if (levelTwoUnsecuredSecondCycleMaxAmount.compareTo(levelThreeUnsecuredSecondCycleMaxAmount) >= 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.two.unsecure.second.cycle.should.not.be.greater.than.level.three.unsecure.second.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level two [%s] unsecure Second cycle should not be greater than for level three [%s] unsecure Second cycle",
                            levelTwoUnsecuredSecondCycleMaxAmount, levelThreeUnsecuredSecondCycleMaxAmount));

        }

        if (levelThreeUnsecuredSecondCycleMaxAmount.compareTo(levelFourUnsecuredSecondCycleMaxAmount) >= 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.three.unsecure.second.cycle.should.not.be.greater.than.level.four.unsecure.second.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level three [%s] unsecure Second cycle should not be greater than for level four [%s] unsecure Second cycle",
                            levelThreeUnsecuredSecondCycleMaxAmount, levelFourUnsecuredSecondCycleMaxAmount));

        }
        if (levelFourUnsecuredSecondCycleMaxAmount.compareTo(levelFiveUnsecuredSecondCycleMaxAmount) > 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.four.unsecure.second.cycle.should.not.be.greater.than.level.five.unsecure.second.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level four [%s] unsecure Second cycle should not be greater than for level five [%s] unsecure Second cycle ",
                            levelFourUnsecuredSecondCycleMaxAmount, levelFiveUnsecuredSecondCycleMaxAmount));

        }
    }

    private static void validateAmountsSecuredFirstCycle(BigDecimal levelOneSecuredFirstCycleMaxAmount,
            BigDecimal levelTwoSecuredFirstCycleMaxAmount, BigDecimal levelThreeSecuredFirstCycleMaxAmount,
            BigDecimal levelFourSecuredFirstCycleMaxAmount, BigDecimal levelFiveSecuredFirstCycleMaxAmount) {
        if (levelOneSecuredFirstCycleMaxAmount.compareTo(levelTwoSecuredFirstCycleMaxAmount) >= 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.one.secure.first.cycle.should.not.be.greater.than.level.two.secure.first.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level one [%s] secure first cycle should not be greater than for level two [%s] secure first cycle",
                            levelOneSecuredFirstCycleMaxAmount, levelTwoSecuredFirstCycleMaxAmount));

        }
        if (levelTwoSecuredFirstCycleMaxAmount.compareTo(levelThreeSecuredFirstCycleMaxAmount) >= 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.two.secure.first.cycle.should.not.be.greater.than.level.three.secure.first.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level two [%s] secure first cycle should not be greater than for level three [%s] secure first cycle",
                            levelTwoSecuredFirstCycleMaxAmount, levelThreeSecuredFirstCycleMaxAmount));

        }

        if (levelThreeSecuredFirstCycleMaxAmount.compareTo(levelFourSecuredFirstCycleMaxAmount) >= 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.three.secure.first.cycle.should.not.be.greater.than.level.four.secure.first.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level three [%s] secure first cycle should not be greater than for level four [%s] secure first cycle ",
                            levelThreeSecuredFirstCycleMaxAmount, levelFourSecuredFirstCycleMaxAmount));

        }
        if (levelFourSecuredFirstCycleMaxAmount.compareTo(levelFiveSecuredFirstCycleMaxAmount) > 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.four.secure.first.cycle.should.not.be.greater.than.level.five.secure.first.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level four [%s] secure first cycle should not be greater than for level five [%s] secure first cycle ",
                            levelFourSecuredFirstCycleMaxAmount, levelFiveSecuredFirstCycleMaxAmount));

        }
    }

    private static void validateAmountsSecuredSecondCycle(BigDecimal levelOneSecuredSecondCycleMaxAmount,
            BigDecimal levelTwoSecuredSecondCycleMaxAmount, BigDecimal levelThreeSecuredSecondCycleMaxAmount,
            BigDecimal levelFourSecuredSecondCycleMaxAmount, BigDecimal levelFiveSecuredSecondCycleMaxAmount) {
        if (levelOneSecuredSecondCycleMaxAmount.compareTo(levelTwoSecuredSecondCycleMaxAmount) >= 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.one.secure.second.cycle.should.not.be.greater.than.level.two.secure.second.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level one [%s] secure Second cycle should not be greater than for level two [%s] secure Second cycle",
                            levelOneSecuredSecondCycleMaxAmount, levelTwoSecuredSecondCycleMaxAmount));

        }
        if (levelTwoSecuredSecondCycleMaxAmount.compareTo(levelThreeSecuredSecondCycleMaxAmount) >= 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.two.secure.second.cycle.should.not.be.greater.than.level.three.secure.second.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level two [%s] secure Second cycle should not be greater than for level three [%s] secure Second cycle",
                            levelTwoSecuredSecondCycleMaxAmount, levelThreeSecuredSecondCycleMaxAmount));

        }

        if (levelThreeSecuredSecondCycleMaxAmount.compareTo(levelFourSecuredSecondCycleMaxAmount) >= 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.three.secure.second.cycle.should.not.be.greater.than.level.four.secure.second.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level three [%s] secure Second cycle should not be greater than for level four [%s] secure Second cycle",
                            levelThreeSecuredSecondCycleMaxAmount, levelFourSecuredSecondCycleMaxAmount));

        }
        if (levelFourSecuredSecondCycleMaxAmount.compareTo(levelFiveSecuredSecondCycleMaxAmount) > 0) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.max.amount.for.level.four.secure.second.cycle.should.not.be.greater.than.level.five.secure.second.cycle.max.amount",
                    String.format(
                            "Loan maximum amount for level four [%s] secure Second cycle should not be greater than for level five [%s] secure Second cycle",
                            levelFourSecuredSecondCycleMaxAmount, levelFiveSecuredSecondCycleMaxAmount));

        }
    }

    public void validateUpdateApprovalMatrix(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Set<String> disbursementParameters = new HashSet<>(Arrays.asList(LoanApprovalMatrixConstants.currencyParameterName,
                LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMinTerm, LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMinTerm, LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMinTerm,
                LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxTerm,
                LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxAmount,
                LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMinTerm,
                LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxTerm, LoanApiConstants.localeParameterName));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, disbursementParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loanApprovalMatrix");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.currencyParameterName, element)) {
            final String currency = this.fromApiJsonHelper.extractStringNamed(LoanApprovalMatrixConstants.currencyParameterName, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.currencyParameterName).value(currency).notExceedingLengthOf(10)
                    .notNull();
        }
        BigDecimal levelOneUnsecuredFirstCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxAmount, element)) {
            levelOneUnsecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxAmount)
                    .value(levelOneUnsecuredFirstCycleMaxAmount).positiveAmount().notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMinTerm, element)) {
            final Integer levelOneUnsecuredFirstCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMinTerm)
                    .value(levelOneUnsecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxTerm, element)) {
            final Integer levelOneUnsecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneUnsecuredFirstCycleMaxTerm)
                    .value(levelOneUnsecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelOneUnsecuredSecondCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxAmount, element)) {
            levelOneUnsecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxAmount)
                    .value(levelOneUnsecuredSecondCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMinTerm, element)) {
            final Integer levelOneUnsecuredSecondCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMinTerm)
                    .value(levelOneUnsecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();
        }

        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxTerm, element)) {
            final Integer levelOneUnsecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneUnsecuredSecondCycleMaxTerm)
                    .value(levelOneUnsecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelOneSecuredFirstCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxAmount, element)) {
            levelOneSecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxAmount)
                    .value(levelOneSecuredFirstCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMinTerm, element)) {
            final Integer levelOneSecuredFirstCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMinTerm)
                    .value(levelOneSecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxTerm, element)) {
            final Integer levelOneSecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneSecuredFirstCycleMaxTerm)
                    .value(levelOneSecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelOneSecuredSecondCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxAmount, element)) {
            levelOneSecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxAmount)
                    .value(levelOneSecuredSecondCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMinTerm, element)) {
            final Integer levelOneSecuredSecondCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMinTerm)
                    .value(levelOneSecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxTerm, element)) {
            final Integer levelOneSecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelOneSecuredSecondCycleMaxTerm)
                    .value(levelOneSecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelTwoUnsecuredFirstCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxAmount, element)) {
            levelTwoUnsecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxAmount)
                    .value(levelTwoUnsecuredFirstCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMinTerm, element)) {
            final Integer levelTwoUnsecuredFirstCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMinTerm)
                    .value(levelTwoUnsecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxTerm, element)) {
            final Integer levelTwoUnsecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoUnsecuredFirstCycleMaxTerm)
                    .value(levelTwoUnsecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelTwoUnsecuredSecondCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxAmount, element)) {
            levelTwoUnsecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxAmount)
                    .value(levelTwoUnsecuredSecondCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMinTerm, element)) {
            final Integer levelTwoUnsecuredSecondCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMinTerm)
                    .value(levelTwoUnsecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxTerm, element)) {
            final Integer levelTwoUnsecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoUnsecuredSecondCycleMaxTerm)
                    .value(levelTwoUnsecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelTwoSecuredFirstCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxAmount, element)) {
            levelTwoSecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxAmount)
                    .value(levelTwoSecuredFirstCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMinTerm, element)) {
            final Integer levelTwoSecuredFirstCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMinTerm)
                    .value(levelTwoSecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxTerm, element)) {
            final Integer levelTwoSecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoSecuredFirstCycleMaxTerm)
                    .value(levelTwoSecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelTwoSecuredSecondCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxAmount, element)) {
            levelTwoSecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxAmount)
                    .value(levelTwoSecuredSecondCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMinTerm, element)) {
            final Integer levelTwoSecuredSecondCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMinTerm)
                    .value(levelTwoSecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxTerm, element)) {
            final Integer levelTwoSecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelTwoSecuredSecondCycleMaxTerm)
                    .value(levelTwoSecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelThreeUnsecuredFirstCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxAmount, element)) {
            levelThreeUnsecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxAmount)
                    .value(levelThreeUnsecuredFirstCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMinTerm, element)) {
            final Integer levelThreeUnsecuredFirstCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMinTerm)
                    .value(levelThreeUnsecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxTerm, element)) {
            final Integer levelThreeUnsecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeUnsecuredFirstCycleMaxTerm)
                    .value(levelThreeUnsecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelThreeUnsecuredSecondCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxAmount, element)) {
            levelThreeUnsecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxAmount)
                    .value(levelThreeUnsecuredSecondCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMinTerm, element)) {
            final Integer levelThreeUnsecuredSecondCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMinTerm)
                    .value(levelThreeUnsecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxTerm, element)) {
            final Integer levelThreeUnsecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeUnsecuredSecondCycleMaxTerm)
                    .value(levelThreeUnsecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelThreeSecuredFirstCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxAmount, element)) {
            levelThreeSecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxAmount)
                    .value(levelThreeSecuredFirstCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMinTerm, element)) {
            final Integer levelThreeSecuredFirstCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMinTerm)
                    .value(levelThreeSecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxTerm, element)) {
            final Integer levelThreeSecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeSecuredFirstCycleMaxTerm)
                    .value(levelThreeSecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelThreeSecuredSecondCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxAmount, element)) {
            levelThreeSecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxAmount)
                    .value(levelThreeSecuredSecondCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMinTerm, element)) {
            final Integer levelThreeSecuredSecondCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMinTerm)
                    .value(levelThreeSecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxTerm, element)) {
            final Integer levelThreeSecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelThreeSecuredSecondCycleMaxTerm)
                    .value(levelThreeSecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelFourUnsecuredFirstCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxAmount, element)) {
            levelFourUnsecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxAmount)
                    .value(levelFourUnsecuredFirstCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMinTerm, element)) {
            final Integer levelFourUnsecuredFirstCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMinTerm)
                    .value(levelFourUnsecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxTerm, element)) {
            final Integer levelFourUnsecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourUnsecuredFirstCycleMaxTerm)
                    .value(levelFourUnsecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelFourUnsecuredSecondCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxAmount, element)) {
            levelFourUnsecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxAmount)
                    .value(levelFourUnsecuredSecondCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMinTerm, element)) {
            final Integer levelFourUnsecuredSecondCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMinTerm)
                    .value(levelFourUnsecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxTerm, element)) {
            final Integer levelFourUnsecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourUnsecuredSecondCycleMaxTerm)
                    .value(levelFourUnsecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelFourSecuredFirstCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxAmount, element)) {
            levelFourSecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxAmount)
                    .value(levelFourSecuredFirstCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMinTerm, element)) {
            final Integer levelFourSecuredFirstCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMinTerm)
                    .value(levelFourSecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxTerm, element)) {
            final Integer levelFourSecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourSecuredFirstCycleMaxTerm)
                    .value(levelFourSecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelFourSecuredSecondCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxAmount, element)) {
            levelFourSecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxAmount)
                    .value(levelFourSecuredSecondCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMinTerm, element)) {
            final Integer levelFourSecuredSecondCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMinTerm)
                    .value(levelFourSecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxTerm, element)) {
            final Integer levelFourSecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFourSecuredSecondCycleMaxTerm)
                    .value(levelFourSecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelFiveUnsecuredFirstCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxAmount, element)) {
            levelFiveUnsecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxAmount)
                    .value(levelFiveUnsecuredFirstCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMinTerm, element)) {
            final Integer levelFiveUnsecuredFirstCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMinTerm)
                    .value(levelFiveUnsecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxTerm, element)) {
            final Integer levelFiveUnsecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveUnsecuredFirstCycleMaxTerm)
                    .value(levelFiveUnsecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelFiveUnsecuredSecondCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxAmount, element)) {
            levelFiveUnsecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxAmount)
                    .value(levelFiveUnsecuredSecondCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMinTerm, element)) {
            final Integer levelFiveUnsecuredSecondCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMinTerm)
                    .value(levelFiveUnsecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxTerm, element)) {
            final Integer levelFiveUnsecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveUnsecuredSecondCycleMaxTerm)
                    .value(levelFiveUnsecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelFiveSecuredFirstCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxAmount, element)) {
            levelFiveSecuredFirstCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxAmount)
                    .value(levelFiveSecuredFirstCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMinTerm, element)) {
            final Integer levelFiveSecuredFirstCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMinTerm)
                    .value(levelFiveSecuredFirstCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxTerm, element)) {
            final Integer levelFiveSecuredFirstCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveSecuredFirstCycleMaxTerm)
                    .value(levelFiveSecuredFirstCycleMaxTerm).notNull().integerGreaterThanZero();
        }
        BigDecimal levelFiveSecuredSecondCycleMaxAmount = null;
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxAmount, element)) {
            levelFiveSecuredSecondCycleMaxAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxAmount, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxAmount)
                    .value(levelFiveSecuredSecondCycleMaxAmount).positiveAmount().notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMinTerm, element)) {
            final Integer levelFiveSecuredSecondCycleMinTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMinTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMinTerm)
                    .value(levelFiveSecuredSecondCycleMinTerm).notNull().integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxTerm, element)) {
            final Integer levelFiveSecuredSecondCycleMaxTerm = this.fromApiJsonHelper
                    .extractIntegerWithLocaleNamed(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxTerm, element);
            baseDataValidator.reset().parameter(LoanApprovalMatrixConstants.levelFiveSecuredSecondCycleMaxTerm)
                    .value(levelFiveSecuredSecondCycleMaxTerm).notNull().integerGreaterThanZero();
        }

        // Lower levels amounts should not be greater than upper levels

        validateAmountsUnsecuredFirstCycle(levelOneUnsecuredFirstCycleMaxAmount, levelTwoUnsecuredFirstCycleMaxAmount,
                levelThreeUnsecuredFirstCycleMaxAmount, levelFourUnsecuredFirstCycleMaxAmount, levelFiveUnsecuredFirstCycleMaxAmount);

        validateAmountsUnsecuredSecondCycle(levelOneUnsecuredSecondCycleMaxAmount, levelTwoUnsecuredSecondCycleMaxAmount,
                levelThreeUnsecuredSecondCycleMaxAmount, levelFourUnsecuredSecondCycleMaxAmount, levelFiveUnsecuredSecondCycleMaxAmount);

        validateAmountsSecuredFirstCycle(levelOneSecuredFirstCycleMaxAmount, levelTwoSecuredFirstCycleMaxAmount,
                levelThreeSecuredFirstCycleMaxAmount, levelFourSecuredFirstCycleMaxAmount, levelFiveSecuredFirstCycleMaxAmount);

        validateAmountsSecuredSecondCycle(levelOneSecuredSecondCycleMaxAmount, levelTwoSecuredSecondCycleMaxAmount,
                levelThreeSecuredSecondCycleMaxAmount, levelFourSecuredSecondCycleMaxAmount, levelFiveSecuredSecondCycleMaxAmount);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateIcReviewStage(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Set<String> reviewParameters = new HashSet<>(
                Arrays.asList(LoanApiConstants.loanId, LoanApiConstants.icReviewOnDateParameterName, LoanApiConstants.noteParameterName,
                        LoanApiConstants.localeParameterName, LoanApiConstants.dateFormatParameterName, LoanApiConstants.icReviewRecommendedAmount,
                        LoanApiConstants.icReviewTermFrequency, LoanApiConstants.icReviewTermPeriodFrequencyEnum));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, reviewParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loanDecisionEngine");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final LocalDate icReviewOn = this.fromApiJsonHelper.extractLocalDateNamed(LoanApiConstants.icReviewOnDateParameterName, element);
        baseDataValidator.reset().parameter(LoanApiConstants.icReviewOnDateParameterName).value(icReviewOn).notNull();
        final BigDecimal icReviewRecommendedAmount = this.fromApiJsonHelper.extractBigDecimalNamed(LoanApiConstants.icReviewRecommendedAmount, element, Locale.US);
        baseDataValidator.reset().parameter(LoanApiConstants.icReviewRecommendedAmount).value(icReviewRecommendedAmount).integerGreaterThanZero().notNull();
        final Integer icReviewTermFrequency = this.fromApiJsonHelper.extractIntegerNamed(LoanApiConstants.icReviewTermFrequency, element, Locale.US);
        baseDataValidator.reset().parameter(LoanApiConstants.icReviewTermFrequency).value(icReviewTermFrequency).integerGreaterThanZero().notNull();
        final Integer icReviewTermPeriodFrequencyEnum = this.fromApiJsonHelper.extractIntegerNamed(LoanApiConstants.icReviewTermPeriodFrequencyEnum, element, Locale.US);
        baseDataValidator.reset().parameter(LoanApiConstants.icReviewTermPeriodFrequencyEnum).value(icReviewTermPeriodFrequencyEnum).notNull();
        final String note = this.fromApiJsonHelper.extractStringNamed(LoanApiConstants.noteParameterName, element);
        baseDataValidator.reset().parameter(LoanApiConstants.noteParameterName).value(note).notExceedingLengthOf(1000).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validatePrepareAndSignContractStage(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Set<String> reviewParameters = new HashSet<>(
                Arrays.asList(LoanApiConstants.loanId, LoanApiConstants.icReviewOnDateParameterName, LoanApiConstants.noteParameterName,
                        LoanApiConstants.localeParameterName, LoanApiConstants.dateFormatParameterName));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, reviewParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loanDecisionEngine");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final LocalDate icReviewOn = this.fromApiJsonHelper.extractLocalDateNamed(LoanApiConstants.icReviewOnDateParameterName, element);
        baseDataValidator.reset().parameter(LoanApiConstants.icReviewOnDateParameterName).value(icReviewOn).notNull();
        final String note = this.fromApiJsonHelper.extractStringNamed(LoanApiConstants.noteParameterName, element);
        baseDataValidator.reset().parameter(LoanApiConstants.noteParameterName).value(note).notExceedingLengthOf(1000).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }
}
