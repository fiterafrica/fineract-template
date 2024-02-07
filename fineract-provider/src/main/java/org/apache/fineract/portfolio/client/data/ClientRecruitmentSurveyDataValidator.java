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
package org.apache.fineract.portfolio.client.data;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class ClientRecruitmentSurveyDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final ConfigurationReadPlatformService configurationReadPlatformService;

    @Autowired
    public ClientRecruitmentSurveyDataValidator(final FromJsonHelper fromApiJsonHelper,
            final ConfigurationReadPlatformService configurationReadPlatformService) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.configurationReadPlatformService = configurationReadPlatformService;
    }

    public void validateForCreate(final Long clientId, String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                ClientApiConstants.CLIENT_RECRUITMENT_SURVEY_RESPONSE_REQUEST_PARAMETER);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ClientApiConstants.CLIENT_RECRUITMENT_SURVEY_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        baseDataValidator.reset().parameter("clientId").value(clientId).notNull().integerGreaterThanZero();

        final Integer countryId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ClientApiConstants.countryIdParamName, element);
        baseDataValidator.reset().parameter(ClientApiConstants.countryIdParamName).value(countryId).integerGreaterThanZero();

        final Integer cohortId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ClientApiConstants.cohortIdParamName, element);
        baseDataValidator.reset().parameter(ClientApiConstants.cohortIdParamName).value(cohortId).integerGreaterThanZero();

        final Integer programId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ClientApiConstants.programIdParamName, element);
        baseDataValidator.reset().parameter(ClientApiConstants.programIdParamName).value(programId).integerGreaterThanZero();

        final Integer surveyLocationId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ClientApiConstants.surveyLocationIdParamName,
                element);
        baseDataValidator.reset().parameter(ClientApiConstants.surveyLocationIdParamName).value(surveyLocationId).integerGreaterThanZero();

        final String surveyName = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.surveyNameParamName, element);
        baseDataValidator.reset().parameter(ClientApiConstants.surveyNameParamName).value(surveyName).notBlank();

        final LocalDate startDate = this.fromApiJsonHelper.extractLocalDateNamed(ClientApiConstants.startDateParamName, element);
        baseDataValidator.reset().parameter(ClientApiConstants.startDateParamName).value(startDate).notNull();

        final LocalDate endDate = this.fromApiJsonHelper.extractLocalDateNamed(ClientApiConstants.endDateParamName, element);
        baseDataValidator.reset().parameter(ClientApiConstants.endDateParamName).value(endDate).notNull().validateDateAfter(startDate);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

    }

    public void validateForUpdate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                ClientApiConstants.CLIENT_RECRUITMENT_SURVEY_RESPONSE_REQUEST_PARAMETER);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ClientApiConstants.CLIENT_RECRUITMENT_SURVEY_RESOURCE_NAME);

        boolean atLeastOneParameterPassedForUpdate = false;

        if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.countryIdParamName, element)) {
            atLeastOneParameterPassedForUpdate = true;
            final Integer countryId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ClientApiConstants.countryIdParamName, element);
            baseDataValidator.reset().parameter(ClientApiConstants.countryIdParamName).value(countryId).integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.cohortIdParamName, element)) {
            atLeastOneParameterPassedForUpdate = true;
            final Integer cohortId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ClientApiConstants.cohortIdParamName, element);
            baseDataValidator.reset().parameter(ClientApiConstants.cohortIdParamName).value(cohortId).integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.programIdParamName, element)) {
            atLeastOneParameterPassedForUpdate = true;
            final Integer programId = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ClientApiConstants.programIdParamName, element);
            baseDataValidator.reset().parameter(ClientApiConstants.programIdParamName).value(programId).integerGreaterThanZero();
        }
        if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.surveyNameParamName, element)) {
            atLeastOneParameterPassedForUpdate = true;
            final String surveyName = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.surveyNameParamName, element);
            baseDataValidator.reset().parameter(ClientApiConstants.surveyNameParamName).value(surveyName).notBlank();
        }
        if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.surveyLocationParamName, element)) {
            atLeastOneParameterPassedForUpdate = true;
            final String surveyLocation = this.fromApiJsonHelper.extractStringNamed(ClientApiConstants.surveyLocationParamName, element);
            baseDataValidator.reset().parameter(ClientApiConstants.surveyLocationParamName).value(surveyLocation).notBlank();
        }
        if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.startDateParamName, element)) {
            atLeastOneParameterPassedForUpdate = true;
            final LocalDate startDate = this.fromApiJsonHelper.extractLocalDateNamed(ClientApiConstants.startDateParamName, element);
            baseDataValidator.reset().parameter(ClientApiConstants.startDateParamName).value(startDate).notNull();
        }
        if (this.fromApiJsonHelper.parameterExists(ClientApiConstants.endDateParamName, element)) {
            atLeastOneParameterPassedForUpdate = true;
            final LocalDate startDate = this.fromApiJsonHelper.extractLocalDateNamed(ClientApiConstants.startDateParamName, element);
            final LocalDate endDate = this.fromApiJsonHelper.extractLocalDateNamed(ClientApiConstants.endDateParamName, element);
            baseDataValidator.reset().parameter(ClientApiConstants.endDateParamName).value(endDate).notNull().validateDateAfter(startDate);
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
