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
package org.apache.fineract.portfolio.savings.domain;

import static org.apache.fineract.portfolio.interestratechart.InterestRateChartApiConstants.endDateParamName;
import static org.apache.fineract.portfolio.interestratechart.InterestRateChartApiConstants.fromDateParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.allowOverdraftParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.chargesParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.currencyCodeParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.daysToDormancyParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.daysToEscheatParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.daysToInactiveParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.descriptionParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.digitsAfterDecimalParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.enforceMinRequiredBalanceParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.floatingInterestRateValueParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.floatingInterestRatesParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.idParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.inMultiplesOfParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.interestCalculationDaysInYearTypeParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.interestCalculationTypeParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.interestCompoundingPeriodTypeParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.interestPostingPeriodTypeParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.isDormancyTrackingActiveParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.isInterestPostingConfigUpdateParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.lienAllowedParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.lockinPeriodFrequencyParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.lockinPeriodFrequencyTypeParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.maxAllowedLienLimitParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.minBalanceForInterestCalculationParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.minOverdraftForInterestCalculationParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.minRequiredBalanceParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.minRequiredOpeningBalanceParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.nameParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.nominalAnnualInterestRateOverdraftParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.nominalAnnualInterestRateParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.numberOfCreditTransactionsParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.numberOfDebitTransactionsParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.overdraftLimitParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.shortNameParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.taxGroupIdParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.useFloatingInterestRateParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.withHoldTaxParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.withdrawalFeeForTransfersParamName;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.charge.exception.ChargeCannotBeAppliedToException;
import org.apache.fineract.portfolio.loanproduct.exception.InvalidCurrencyException;
import org.apache.fineract.portfolio.savings.SavingsApiConstants;
import org.apache.fineract.portfolio.savings.SavingsCompoundingInterestPeriodType;
import org.apache.fineract.portfolio.savings.SavingsInterestCalculationDaysInYearType;
import org.apache.fineract.portfolio.savings.SavingsInterestCalculationType;
import org.apache.fineract.portfolio.savings.SavingsPeriodFrequencyType;
import org.apache.fineract.portfolio.savings.SavingsPostingInterestPeriodType;
import org.apache.fineract.portfolio.tax.domain.TaxGroup;
import org.apache.fineract.portfolio.tax.domain.TaxGroupRepositoryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class SavingsProductAssembler {

    private final ChargeRepositoryWrapper chargeRepository;
    private final TaxGroupRepositoryWrapper taxGroupRepository;
    private final FromJsonHelper fromApiJsonHelper;

    private final CodeValueRepositoryWrapper codeValueRepository;

    @Autowired
    public SavingsProductAssembler(final ChargeRepositoryWrapper chargeRepository, final TaxGroupRepositoryWrapper taxGroupRepository,
            final FromJsonHelper fromApiJsonHelper, CodeValueRepositoryWrapper codeValueRepository) {
        this.chargeRepository = chargeRepository;
        this.taxGroupRepository = taxGroupRepository;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.codeValueRepository = codeValueRepository;
    }

    public SavingsProduct assemble(final JsonCommand command) {

        final String name = command.stringValueOfParameterNamed(nameParamName);
        final String shortName = command.stringValueOfParameterNamed(shortNameParamName);
        final String description = command.stringValueOfParameterNamedAllowingNull(descriptionParamName);

        CodeValue productCategory = getLoanProductCategory(command);
        CodeValue productType = getLoanProductType(command);

        final String currencyCode = command.stringValueOfParameterNamed(currencyCodeParamName);
        final Integer digitsAfterDecimal = command.integerValueOfParameterNamed(digitsAfterDecimalParamName);
        final Integer inMultiplesOf = command.integerValueOfParameterNamed(inMultiplesOfParamName);
        final MonetaryCurrency currency = new MonetaryCurrency(currencyCode, digitsAfterDecimal, inMultiplesOf);

        final BigDecimal interestRate = command.bigDecimalValueOfParameterNamed(nominalAnnualInterestRateParamName);

        SavingsCompoundingInterestPeriodType interestCompoundingPeriodType = null;
        final Integer interestPeriodTypeValue = command.integerValueOfParameterNamed(interestCompoundingPeriodTypeParamName);
        if (interestPeriodTypeValue != null) {
            interestCompoundingPeriodType = SavingsCompoundingInterestPeriodType.fromInt(interestPeriodTypeValue);
        }

        SavingsPostingInterestPeriodType interestPostingPeriodType = null;
        final Integer interestPostingPeriodTypeValue = command.integerValueOfParameterNamed(interestPostingPeriodTypeParamName);
        if (interestPostingPeriodTypeValue != null) {
            interestPostingPeriodType = SavingsPostingInterestPeriodType.fromInt(interestPostingPeriodTypeValue);
        }

        SavingsInterestCalculationType interestCalculationType = null;
        final Integer interestCalculationTypeValue = command.integerValueOfParameterNamed(interestCalculationTypeParamName);
        if (interestCalculationTypeValue != null) {
            interestCalculationType = SavingsInterestCalculationType.fromInt(interestCalculationTypeValue);
        }

        SavingsInterestCalculationDaysInYearType interestCalculationDaysInYearType = null;
        final Integer interestCalculationDaysInYearTypeValue = command
                .integerValueOfParameterNamed(interestCalculationDaysInYearTypeParamName);
        if (interestCalculationDaysInYearTypeValue != null) {
            interestCalculationDaysInYearType = SavingsInterestCalculationDaysInYearType.fromInt(interestCalculationDaysInYearTypeValue);
        }

        final BigDecimal minRequiredOpeningBalance = command
                .bigDecimalValueOfParameterNamedDefaultToNullIfZero(minRequiredOpeningBalanceParamName);

        final Integer lockinPeriodFrequency = command.integerValueOfParameterNamedDefaultToNullIfZero(lockinPeriodFrequencyParamName);
        SavingsPeriodFrequencyType lockinPeriodFrequencyType = null;
        final Integer lockinPeriodFrequencyTypeValue = command.integerValueOfParameterNamed(lockinPeriodFrequencyTypeParamName);
        if (lockinPeriodFrequencyTypeValue != null) {
            lockinPeriodFrequencyType = SavingsPeriodFrequencyType.fromInt(lockinPeriodFrequencyTypeValue);
        }

        boolean iswithdrawalFeeApplicableForTransfer = false;
        if (command.parameterExists(withdrawalFeeForTransfersParamName)) {
            iswithdrawalFeeApplicableForTransfer = command.booleanPrimitiveValueOfParameterNamed(withdrawalFeeForTransfersParamName);
        }

        final AccountingRuleType accountingRuleType = AccountingRuleType.fromInt(command.integerValueOfParameterNamed("accountingRule"));

        // Savings product charges
        final Set<Charge> charges = assembleListOfSavingsProductCharges(command, currencyCode);

        boolean allowOverdraft = false;
        if (command.parameterExists(allowOverdraftParamName)) {
            allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(allowOverdraftParamName);
        }

        BigDecimal overdraftLimit = BigDecimal.ZERO;
        if (command.parameterExists(overdraftLimitParamName)) {
            overdraftLimit = command.bigDecimalValueOfParameterNamed(overdraftLimitParamName);
        }

        BigDecimal nominalAnnualInterestRateOverdraft = BigDecimal.ZERO;
        if (command.parameterExists(nominalAnnualInterestRateOverdraftParamName)) {
            nominalAnnualInterestRateOverdraft = command.bigDecimalValueOfParameterNamed(nominalAnnualInterestRateOverdraftParamName);
        }

        BigDecimal minOverdraftForInterestCalculation = BigDecimal.ZERO;
        if (command.parameterExists(minOverdraftForInterestCalculationParamName)) {
            minOverdraftForInterestCalculation = command.bigDecimalValueOfParameterNamed(minOverdraftForInterestCalculationParamName);
        }

        boolean enforceMinRequiredBalance = false;
        if (command.parameterExists(enforceMinRequiredBalanceParamName)) {
            enforceMinRequiredBalance = command.booleanPrimitiveValueOfParameterNamed(enforceMinRequiredBalanceParamName);
        }

        boolean useFloatingInterestRate = false;
        if (command.parameterExists(useFloatingInterestRateParamName)) {
            useFloatingInterestRate = command.booleanPrimitiveValueOfParameterNamed(useFloatingInterestRateParamName);
        }

        BigDecimal minRequiredBalance = BigDecimal.ZERO;
        if (command.parameterExists(minRequiredBalanceParamName)) {
            minRequiredBalance = command.bigDecimalValueOfParameterNamed(minRequiredBalanceParamName);
        }

        boolean lienAllowed = false;
        if (command.parameterExists(lienAllowedParamName)) {
            lienAllowed = command.booleanPrimitiveValueOfParameterNamed(lienAllowedParamName);
        }

        BigDecimal maxAllowedLienLimit = BigDecimal.ZERO;
        if (command.parameterExists(maxAllowedLienLimitParamName)) {
            maxAllowedLienLimit = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(maxAllowedLienLimitParamName);
        }
        final BigDecimal minBalanceForInterestCalculation = command
                .bigDecimalValueOfParameterNamedDefaultToNullIfZero(minBalanceForInterestCalculationParamName);

        boolean withHoldTax = command.booleanPrimitiveValueOfParameterNamed(withHoldTaxParamName);
        final TaxGroup taxGroup = assembleTaxGroup(command);

        final Boolean isDormancyTrackingActive = command.booleanObjectValueOfParameterNamed(isDormancyTrackingActiveParamName);
        final Long daysToInactive = command.longValueOfParameterNamed(daysToInactiveParamName);
        final Long daysToDormancy = command.longValueOfParameterNamed(daysToDormancyParamName);
        final Long daysToEscheat = command.longValueOfParameterNamed(daysToEscheatParamName);

        final Boolean isInterestPostingConfigUpdate = command.booleanObjectValueOfParameterNamed(isInterestPostingConfigUpdateParamName);
        final Long numOfCreditTransaction = command.longValueOfParameterNamed(numberOfCreditTransactionsParamName);
        final Long numOfDebitTransaction = command.longValueOfParameterNamed(numberOfDebitTransactionsParamName);

        return SavingsProduct.createNew(name, shortName, description, currency, interestRate, interestCompoundingPeriodType,
                interestPostingPeriodType, interestCalculationType, interestCalculationDaysInYearType, minRequiredOpeningBalance,
                lockinPeriodFrequency, lockinPeriodFrequencyType, iswithdrawalFeeApplicableForTransfer, accountingRuleType, charges,
                allowOverdraft, overdraftLimit, enforceMinRequiredBalance, minRequiredBalance, lienAllowed, maxAllowedLienLimit,
                minBalanceForInterestCalculation, nominalAnnualInterestRateOverdraft, minOverdraftForInterestCalculation, withHoldTax,
                taxGroup, isDormancyTrackingActive, daysToInactive, daysToDormancy, daysToEscheat, isInterestPostingConfigUpdate,
                numOfCreditTransaction, numOfDebitTransaction, useFloatingInterestRate,productCategory,productType);
    }

    public Set<SavingsProductFloatingInterestRate> assembleListOfFloatingInterestRates(final JsonCommand command,
            SavingsProduct savingsProduct) {
        final Set<SavingsProductFloatingInterestRate> floatingInterestRates = new HashSet<>();
        if (command.parameterExists(floatingInterestRatesParamName)) {
            final JsonArray floatingInterestRatesArray = command.arrayOfParameterNamed(floatingInterestRatesParamName);
            if (floatingInterestRatesArray != null) {
                for (int i = 0; i < floatingInterestRatesArray.size(); i++) {
                    final JsonObject floatingInterestRateElement = floatingInterestRatesArray.get(i).getAsJsonObject();
                    SavingsProductFloatingInterestRate floatingInterestRate = assembleSavingsProductFloatingInterestRateFrom(
                            floatingInterestRateElement, savingsProduct);
                    validateSavingsProductFloatingInterestRate(floatingInterestRate, floatingInterestRates);
                    floatingInterestRates.add(floatingInterestRate);
                }
            }
        }
        return floatingInterestRates;
    }

    public Set<Charge> assembleListOfSavingsProductCharges(final JsonCommand command, final String savingsProductCurrencyCode) {

        final Set<Charge> charges = new HashSet<>();

        if (command.parameterExists(chargesParamName)) {
            final JsonArray chargesArray = command.arrayOfParameterNamed(chargesParamName);
            if (chargesArray != null) {
                for (int i = 0; i < chargesArray.size(); i++) {

                    final JsonObject jsonObject = chargesArray.get(i).getAsJsonObject();
                    if (jsonObject.has(idParamName)) {
                        final Long id = jsonObject.get(idParamName).getAsLong();

                        final Charge charge = this.chargeRepository.findOneWithNotFoundDetection(id);

                        if (!charge.isSavingsCharge()) {
                            final String errorMessage = "Charge with identifier " + charge.getId()
                                    + " cannot be applied to Savings product.";
                            throw new ChargeCannotBeAppliedToException("savings.product", errorMessage, charge.getId());
                        }

                        if (!savingsProductCurrencyCode.equals(charge.getCurrencyCode())) {
                            final String errorMessage = "Charge and Savings Product must have the same currency.";
                            throw new InvalidCurrencyException("charge", "attach.to.savings.product", errorMessage);
                        }
                        charges.add(charge);
                    }
                }
            }
        }

        return charges;
    }

    public TaxGroup assembleTaxGroup(final JsonCommand command) {
        final Long taxGroupId = command.longValueOfParameterNamed(taxGroupIdParamName);
        TaxGroup taxGroup = null;
        if (taxGroupId != null) {
            taxGroup = this.taxGroupRepository.findOneWithNotFoundDetection(taxGroupId);
        }
        return taxGroup;
    }

    public SavingsProductFloatingInterestRate assembleSavingsProductFloatingInterestRateFrom(final JsonElement element,
            SavingsProduct savingsProduct) {

        final LocalDate fromDate = this.fromApiJsonHelper.extractLocalDateNamed(fromDateParamName, element);
        final LocalDate toDate = this.fromApiJsonHelper.extractLocalDateNamed(endDateParamName, element);
        final BigDecimal floatingInterestRate = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(floatingInterestRateValueParamName,
                element);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource("SavingsProductFloatingInterestRates");
        if (fromDate == null) {
            baseDataValidator.parameter("fromDate").failWithCode("fromDate.is.empty");
        }
        if (floatingInterestRate == null) {
            baseDataValidator.parameter("floatingInterestRate").failWithCode("floatingInterestRate.is.empty");
        }
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        final SavingsProductFloatingInterestRate savingsProductFloatingInterestRate = SavingsProductFloatingInterestRate.createNew(fromDate,
                toDate, floatingInterestRate, savingsProduct);

        return savingsProductFloatingInterestRate;
    }

    public void validateSavingsProductFloatingInterestRate(SavingsProductFloatingInterestRate savingsProductFloatingInterestRateToValidate,
            Set<SavingsProductFloatingInterestRate> existingSavingProductFloatingInterestRates) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource("SavingsProductFloatingInterestRates");

        if (CollectionUtils.isNotEmpty(existingSavingProductFloatingInterestRates)) {

            // is floating Interest with same from date already exist , throw validation error
            for (SavingsProductFloatingInterestRate existingSavingsProductFloatingInterestRate : existingSavingProductFloatingInterestRates) {
                if (DateUtils.isSameLocalDate(savingsProductFloatingInterestRateToValidate.getFromDate(),
                        existingSavingsProductFloatingInterestRate.getFromDate())) {
                    baseDataValidator.parameter("fromDate").failWithCode("multiple.interest.rate.with.same.fromDate");
                }

                if (savingsProductFloatingInterestRateToValidate.getFromDate()
                        .isAfter(existingSavingsProductFloatingInterestRate.getFromDate())
                        && (existingSavingsProductFloatingInterestRate.getEndDate() != null && savingsProductFloatingInterestRateToValidate
                                .getFromDate().isBefore(existingSavingsProductFloatingInterestRate.getEndDate()))) {
                    baseDataValidator.parameter("fromDate")
                            .failWithCode("fromDate.is.overlapping.with.other.floating.interest.rate.period");
                }

                if (savingsProductFloatingInterestRateToValidate.getEndDate() != null) {
                    if (savingsProductFloatingInterestRateToValidate.getEndDate()
                            .isAfter(existingSavingsProductFloatingInterestRate.getFromDate())
                            && (existingSavingsProductFloatingInterestRate.getEndDate() != null
                                    && savingsProductFloatingInterestRateToValidate.getEndDate()
                                            .isBefore(existingSavingsProductFloatingInterestRate.getEndDate()))) {
                        baseDataValidator.parameter("endDate")
                                .failWithCode("endDate.is.overlapping.with.other.floating.interest.rate.period");
                    }
                }
            }
        }
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }

    public static Date asDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    @Nullable
    private CodeValue getLoanProductCategory(JsonCommand command) {
        CodeValue productCategory = null;
        final Long productCategoryId = command.longValueOfParameterNamed(SavingsApiConstants.SAVINGS_PRODUCT_CATEGORY_ID);
        if (productCategoryId != null) {
            productCategory = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(SavingsApiConstants.SAVINGS_PRODUCT_CATEGORY,
                    productCategoryId);
        }
        return productCategory;
    }

    @Nullable
    private CodeValue getLoanProductType(JsonCommand command) {
        CodeValue productType = null;
        final Long productTypeId = command.longValueOfParameterNamed(SavingsApiConstants.SAVINGS_PRODUCT_TYPE_ID);
        if (productTypeId != null) {
            productType = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(SavingsApiConstants.SAVINGS_PRODUCT_TYPE,
                    productTypeId);
        }
        return productType;
    }
}
