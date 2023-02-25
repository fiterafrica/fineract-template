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
package org.apache.fineract.portfolio.savings.data;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SavingsProductFloatingInterestRateApiJsonDeserializer {

    private final FromJsonHelper fromApiJsonHelper;
    private final Set<String> supportedParameters = new HashSet<>(
            Arrays.asList("id", "fromDate", "endDate", "dateFormat", "locale", "floatingInterestRateValue"));

    @Autowired
    public SavingsProductFloatingInterestRateApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(final long savingsProductId, String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource("SavingsProductFloatingInterestRates");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        baseDataValidator.reset().value(savingsProductId).notBlank().integerGreaterThanZero();

        final BigDecimal floatingInterestRate = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("floatingInterestRateValue",
                element);
        baseDataValidator.reset().parameter("floatingInterestRate").value(floatingInterestRate).notNull().positiveAmount();

        final LocalDate fromDate = this.fromApiJsonHelper.extractLocalDateNamed("fromDate", element);
        baseDataValidator.reset().parameter("fromDate").value(fromDate).notNull();

        if (this.fromApiJsonHelper.extractLocalDateNamed("endDate", element) != null) {
            final LocalDate endDate = this.fromApiJsonHelper.extractLocalDateNamed("endDate", element);
            baseDataValidator.reset().parameter("endDate").value(endDate).notNull().validateDateAfter(fromDate);
        }
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForUpdate(final long savingsProductFloatingInterestId, String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource("SavingsProductFloatingInterestRates");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        baseDataValidator.reset().value(savingsProductFloatingInterestId).notBlank().integerGreaterThanZero();

        if (this.fromApiJsonHelper.extractLocalDateNamed("fromDate", element) != null) {
            final LocalDate fromDate = this.fromApiJsonHelper.extractLocalDateNamed("fromDate", element);
            baseDataValidator.reset().parameter("fromDate").value(fromDate).notNull();
        }

        if (this.fromApiJsonHelper.extractLocalDateNamed("endDate", element) != null) {
            final LocalDate endDate = this.fromApiJsonHelper.extractLocalDateNamed("endDate", element);
            baseDataValidator.reset().parameter("endDate").value(endDate).notNull();
        }

        if (this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("floatingInterestRateValue", element) != null) {
            final BigDecimal floatingInterestRate = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("floatingInterestRateValue",
                    element);
            baseDataValidator.reset().parameter("floatingInterestRateValue").value(floatingInterestRate).notNull().integerGreaterThanZero();
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
