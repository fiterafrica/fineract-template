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

package org.apache.fineract.portfolio.client.serialization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class ClientOtherInfoCommandFromApiJsonDeserializer {

    private final FromJsonHelper fromApiJsonHelper;
    private final Set<String> supportedParameters = new HashSet<>(Arrays.asList("id", "clientId", "strataId", "nationalityId",
            "numberOfChildren", "numberOfDependents", "yearArrivedInHostCountryId", "coSignorsName", "guarantor", "locale", "dateFormat",
            "businessLocation", "taxIdentificationNumber", "incomeGeneratingActivity", "incomeGeneratingActivityMonthlyAmount",
            "telephoneNo", "nationalIdentificationNumber", "passportNumber", "bankAccountNumber", "bankName"));

    @Autowired
    public ClientOtherInfoCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(final int legalFormId, String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ClientApiConstants.CLIENT_OTHER_INFO_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        baseDataValidator.reset().parameter(ClientApiConstants.legalFormIdParamName).value(legalFormId).notNull().inMinMaxRange(1, 2);

        final Integer strataId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ClientApiConstants.strataIdParamName, element);
        baseDataValidator.reset().parameter(ClientApiConstants.strataIdParamName).value(strataId).notNull().integerGreaterThanZero();

        if (LegalForm.fromInt(legalFormId).isPerson()) {
            final Integer nationalityId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ClientApiConstants.nationalityIdParamName,
                    element);
            baseDataValidator.reset().parameter(ClientApiConstants.nationalityIdParamName).value(nationalityId).notNull()
                    .integerGreaterThanZero();

            if (this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.numberOfChildren, element) != null) {
                final Long numberOfChildren = this.fromApiJsonHelper.extractLongNamed(ClientApiConstants.numberOfChildren, element);
                baseDataValidator.reset().parameter(ClientApiConstants.numberOfChildren).value(numberOfChildren).notNull()
                        .integerGreaterThanZero();
            }

            if (this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.numberOfDependents, element) != null) {
                final Long numberOfDependents = this.fromApiJsonHelper.extractLongNamed(ClientApiConstants.numberOfDependents, element);
                baseDataValidator.reset().parameter(ClientApiConstants.numberOfChildren).value(numberOfDependents).notNull()
                        .integerGreaterThanZero();
            }
            final Integer yearArrivedInHostCountryId = this.fromApiJsonHelper
                    .extractIntegerSansLocaleNamed(ClientApiConstants.yearArrivedInHostCountry, element);
            baseDataValidator.reset().parameter(ClientApiConstants.yearArrivedInHostCountry).value(yearArrivedInHostCountryId).notNull()
                    .integerGreaterThanZero();

            if (this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.NATIONAL_IDENTIFICATION_NUMBER, element) != null) {
                final String nationalIdentificationNumber = this.fromApiJsonHelper
                        .extractStringNamed(ClientApiConstants.NATIONAL_IDENTIFICATION_NUMBER, element);
                baseDataValidator.reset().parameter(ClientApiConstants.NATIONAL_IDENTIFICATION_NUMBER).value(nationalIdentificationNumber)
                        .notNull().notBlank();
            } else {
                final String passportNumber = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.PASSPORT_NUMBER, element);
                baseDataValidator.reset().parameter(ClientApiConstants.PASSPORT_NUMBER).value(passportNumber).notNull().notBlank();
            }

            final String bankAccountNumber = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.BANK_ACCOUNT_NUMBER, element);
            baseDataValidator.reset().parameter(ClientApiConstants.BANK_ACCOUNT_NUMBER).value(bankAccountNumber).notNull().notBlank();

            final String bankName = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.BANK_NAME, element);
            baseDataValidator.reset().parameter(ClientApiConstants.BANK_NAME).value(bankName).notNull().notBlank();

        } else if (LegalForm.fromInt(legalFormId).isEntity()) {

            final String businessLocation = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.businessLocationParamName,
                    element);
            baseDataValidator.reset().parameter(ClientApiConstants.businessLocationParamName).value(businessLocation).notBlank()
                    .notExceedingLengthOf(100);

            final Long taxIdentificationNumber = this.fromApiJsonHelper
                    .extractLongNamed(ClientApiConstants.taxIdentificationNumberParamName, element);
            baseDataValidator.reset().parameter(ClientApiConstants.taxIdentificationNumberParamName).value(taxIdentificationNumber)
                    .notBlank().integerGreaterThanZero();

            if (this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.coSignors, element) != null) {
                final String coSignors = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.coSignors, element);
                baseDataValidator.reset().parameter(ClientApiConstants.coSignors).value(coSignors).notBlank()
                        .notExceedingLengthOf(100);
            }

            if (this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.guarantor, element) != null) {
                final String guarantor = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.guarantor, element);
                baseDataValidator.reset().parameter(ClientApiConstants.guarantor).value(guarantor).notBlank()
                        .notExceedingLengthOf(100);
            }

            if (this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.incomeGeneratingActivityParamName, element) != null) {
                final Long incomeGeneratingActivity = this.fromApiJsonHelper
                        .extractLongNamed(ClientApiConstants.incomeGeneratingActivityParamName, element);
                baseDataValidator.reset().parameter(ClientApiConstants.incomeGeneratingActivityParamName).value(incomeGeneratingActivity)
                        .notNull().integerGreaterThanZero();
            }
            if (this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.incomeGeneratingActivityMonthlyAmountParamName,
                    element) != null) {
                final BigDecimal incomeGeneratingActivityMonthlyAmount = this.fromApiJsonHelper
                        .extractBigDecimalWithLocaleNamed(ClientApiConstants.incomeGeneratingActivityMonthlyAmountParamName, element);
                baseDataValidator.reset().parameter(ClientApiConstants.incomeGeneratingActivityMonthlyAmountParamName)
                        .value(incomeGeneratingActivityMonthlyAmount).notNull().zeroOrPositiveAmount();
            }
            if (this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.telephoneNoParamName,
                    element) != null) {
                final String telephoneNo = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.telephoneNoParamName, element);
                baseDataValidator.reset().parameter(ClientApiConstants.telephoneNoParamName).value(telephoneNo).notBlank()
                        .notExceedingLengthOf(20);
            }
        }
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

    }

    public void validateForUpdate(final String json, Integer legalFormId) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ClientApiConstants.CLIENT_OTHER_INFO_RESOURCE_NAME);

        boolean atLeastOneParameterPassedForUpdate = false;

        if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.strataIdParamName, element)) {
            atLeastOneParameterPassedForUpdate = true;
            final Integer strataId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ClientApiConstants.strataIdParamName, element);
            baseDataValidator.reset().parameter(ClientApiConstants.staffIdParamName).value(strataId).notNull().integerGreaterThanZero();
        }

        if (LegalForm.fromInt(legalFormId).isPerson()) {
            if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.nationalityIdParamName, element)) {
                atLeastOneParameterPassedForUpdate = true;
                final Integer nationalityId = this.fromApiJsonHelper
                        .extractIntegerSansLocaleNamed(ClientApiConstants.nationalityIdParamName, element);
                baseDataValidator.reset().parameter(ClientApiConstants.nationalityIdParamName).value(nationalityId).notNull()
                        .integerGreaterThanZero();
            }

            if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.numberOfChildren, element)) {
                atLeastOneParameterPassedForUpdate = true;
                final Integer numberOfChildren = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ClientApiConstants.numberOfChildren,
                        element);
                baseDataValidator.reset().parameter(ClientApiConstants.numberOfChildren).value(numberOfChildren).ignoreIfNull()
                        .integerGreaterThanZero();
            }

            if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.numberOfDependents, element)) {
                atLeastOneParameterPassedForUpdate = true;
                final Integer numberOfDependents = this.fromApiJsonHelper
                        .extractIntegerSansLocaleNamed(ClientApiConstants.numberOfDependents, element);
                baseDataValidator.reset().parameter(ClientApiConstants.numberOfDependents).value(numberOfDependents).ignoreIfNull()
                        .integerGreaterThanZero();
            }

            if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.yearArrivedInHostCountry, element)) {
                atLeastOneParameterPassedForUpdate = true;
                final Integer yearArrivedInHostCountryId = this.fromApiJsonHelper
                        .extractIntegerSansLocaleNamed(ClientApiConstants.yearArrivedInHostCountry, element);
                baseDataValidator.reset().parameter(ClientApiConstants.yearArrivedInHostCountry).value(yearArrivedInHostCountryId).notNull()
                        .integerGreaterThanZero();
            }
        } else if (LegalForm.fromInt(legalFormId).isEntity()) {

            if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.businessLocationParamName, element)) {
                atLeastOneParameterPassedForUpdate = true;
                final String businessLocation = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.businessLocationParamName,
                        element);
                baseDataValidator.reset().parameter(ClientApiConstants.businessLocationParamName).value(businessLocation)
                        .notBlank().notExceedingLengthOf(100);
            }

            if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.coSignors, element)) {
                atLeastOneParameterPassedForUpdate = true;
                final String coSignors = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.coSignors, element);
                baseDataValidator.reset().parameter(ClientApiConstants.coSignors).value(coSignors).notBlank()
                        .notExceedingLengthOf(100);
            }

            if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.guarantor, element)) {
                atLeastOneParameterPassedForUpdate = true;
                final String guarantor = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.guarantor, element);
                baseDataValidator.reset().parameter(ClientApiConstants.guarantor).value(guarantor).notBlank()
                        .notExceedingLengthOf(100);
            }
            if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.incomeGeneratingActivityParamName, element)) {
                atLeastOneParameterPassedForUpdate = true;
                final Long incomeGeneratingActivity = this.fromApiJsonHelper
                        .extractLongNamed(ClientApiConstants.incomeGeneratingActivityParamName, element);
                baseDataValidator.reset().parameter(ClientApiConstants.incomeGeneratingActivityParamName).value(incomeGeneratingActivity)
                        .notNull().integerGreaterThanZero();
            }
            if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.incomeGeneratingActivityMonthlyAmountParamName, element)) {
                atLeastOneParameterPassedForUpdate = true;
                final BigDecimal incomeGeneratingActivityMonthlyAmount = this.fromApiJsonHelper
                        .extractBigDecimalWithLocaleNamed(ClientApiConstants.incomeGeneratingActivityMonthlyAmountParamName, element);
                baseDataValidator.reset().parameter(ClientApiConstants.incomeGeneratingActivityMonthlyAmountParamName)
                        .value(incomeGeneratingActivityMonthlyAmount).notNull().zeroOrPositiveAmount();
            }
            if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.telephoneNoParamName, element)) {
                atLeastOneParameterPassedForUpdate = true;
                final String telephoneNo = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.telephoneNoParamName, element);
                baseDataValidator.reset().parameter(ClientApiConstants.telephoneNoParamName).value(telephoneNo).notBlank()
                        .notExceedingLengthOf(20);
            }
        }

        if (!atLeastOneParameterPassedForUpdate) {
            final Object forceError = null;
            baseDataValidator.reset().anyOfNotNull(forceError);
        }
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }

}
