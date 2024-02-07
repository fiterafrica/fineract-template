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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
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
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class ClientFamilyMemberCommandFromApiJsonDeserializer {

    private final FromJsonHelper fromApiJsonHelper;
    private final Set<String> supportedParameters = new HashSet<>(
            Arrays.asList("id", "clientId", "firstName", "middleName", "lastName", "qualification", "mobileNumber", "age", "isDependent",
                    "relationshipId", "maritalStatusId", "genderId", "dateOfBirth", "professionId", "locale", "dateFormat", "familyMembers",
                    "email", "address1", "address2", "address3", "postalCode", "stateProvinceId", "countryId", "cityId", "addressTypeId"));

    @Autowired
    public ClientFamilyMemberCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("FamilyMembers");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.extractArrayNamed("familyMembers", element) != null) {
            final JsonArray familyMembers = this.fromApiJsonHelper.extractJsonArrayNamed("familyMembers", element);
            baseDataValidator.reset().value(familyMembers).arrayNotEmpty();
        } else {
            baseDataValidator.reset().value(this.fromApiJsonHelper.extractJsonArrayNamed("familyMembers", element)).arrayNotEmpty();
        }

        validateForCreate(1, json);

    }

    public void validateForCreate(final long clientId, String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("FamilyMembers");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        baseDataValidator.reset().value(clientId).notBlank().integerGreaterThanZero();

        if (this.fromApiJsonHelper.extractStringNamed("firstName", element) != null) {
            final String firstName = this.fromApiJsonHelper.extractStringNamed("firstName", element);
            baseDataValidator.reset().parameter("firstName").value(firstName).notNull().notBlank().notExceedingLengthOf(100);
        } else {
            baseDataValidator.reset().parameter("firstName").value(this.fromApiJsonHelper.extractStringNamed("firstName", element))
                    .notNull().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractStringNamed("lastName", element) != null) {
            final String lastName = this.fromApiJsonHelper.extractStringNamed("lastName", element);
            baseDataValidator.reset().parameter("lastName").value(lastName).notNull().notBlank().notExceedingLengthOf(100);
        } else {
            baseDataValidator.reset().parameter("lastName").value(this.fromApiJsonHelper.extractStringNamed("lastName", element)).notNull()
                    .notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractStringNamed("middleName", element) != null) {
            final String middleName = this.fromApiJsonHelper.extractStringNamed("middleName", element);
            baseDataValidator.reset().parameter("middleName").value(middleName).notNull().notBlank().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractStringNamed("qualification", element) != null) {
            final String qualification = this.fromApiJsonHelper.extractStringNamed("qualification", element);
            baseDataValidator.reset().parameter("qualification").value(qualification).notNull().notBlank().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractStringNamed("mobileNumber", element) != null) {
            final String mobileNumber = this.fromApiJsonHelper.extractStringNamed("mobileNumber", element);
            baseDataValidator.reset().parameter("mobileNumber").value(mobileNumber).notNull().notBlank().notExceedingLengthOf(100);
        } else {
            baseDataValidator.reset().parameter("mobileNumber").value(this.fromApiJsonHelper.extractStringNamed("mobileNumber", element))
                    .notNull().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractBooleanNamed("isDependent", element) != null) {
            final Boolean isDependent = this.fromApiJsonHelper.extractBooleanNamed("isDependent", element);
            baseDataValidator.reset().parameter("isDependent").value(isDependent).notNull().notBlank().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractLongNamed("relationshipId", element) != null) {
            final long relationShipId = this.fromApiJsonHelper.extractLongNamed("relationshipId", element);
            baseDataValidator.reset().parameter("relationshipId").value(relationShipId).notBlank().longGreaterThanZero();

        } else {
            baseDataValidator.reset().parameter("relationshipId").value(this.fromApiJsonHelper.extractLongNamed("relationShipId", element))
                    .notBlank().longGreaterThanZero();
        }

        if (this.fromApiJsonHelper.extractLongNamed("maritalStatusId", element) != null) {
            final long maritalStatusId = this.fromApiJsonHelper.extractLongNamed("maritalStatusId", element);
            baseDataValidator.reset().parameter("maritalStatusId").value(maritalStatusId).notBlank().longGreaterThanZero();

        }

        if (this.fromApiJsonHelper.extractLongNamed("genderId", element) != null) {
            final long genderId = this.fromApiJsonHelper.extractLongNamed("genderId", element);
            baseDataValidator.reset().parameter("genderId").value(genderId).notBlank().longGreaterThanZero();

        } else {
            baseDataValidator.reset().parameter("genderId").value(this.fromApiJsonHelper.extractStringNamed("genderId", element)).notBlank()
                    .longGreaterThanZero();
        }

        if (this.fromApiJsonHelper.extractLongNamed("age", element) != null) {
            final long age = this.fromApiJsonHelper.extractLongNamed("age", element);
            baseDataValidator.reset().parameter("age").value(age).notBlank().longGreaterThanZero();

        } else {
            baseDataValidator.reset().parameter("age").value(this.fromApiJsonHelper.extractStringNamed("age", element)).notBlank()
                    .longGreaterThanZero();
        }

        if (this.fromApiJsonHelper.extractLongNamed("professionId", element) != null) {
            final long professionId = this.fromApiJsonHelper.extractLongNamed("professionId", element);
            baseDataValidator.reset().parameter("professionId").value(professionId).notBlank().longGreaterThanZero();

        }

        if (this.fromApiJsonHelper.extractLocalDateNamed("dateOfBirth", element) != null) {
            final LocalDate dateOfBirth = this.fromApiJsonHelper.extractLocalDateNamed("dateOfBirth", element);
            baseDataValidator.reset().parameter("dateOfBirth").value(dateOfBirth).value(dateOfBirth).notNull()
                    .validateDateBefore(DateUtils.getBusinessLocalDate());

        }

        if (this.fromApiJsonHelper.extractStringNamed("email", element) != null) {
            final String email = this.fromApiJsonHelper.extractStringNamed("email", element);
            baseDataValidator.reset().parameter("email").value(email).notNull().notBlank().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractLongNamed("countryId", element) != null) {
            final long countryId = this.fromApiJsonHelper.extractLongNamed("countryId", element);
            baseDataValidator.reset().parameter("countryId").value(countryId).notBlank().longGreaterThanZero();

        } else {
            baseDataValidator.reset().parameter("countryId").value(this.fromApiJsonHelper.extractStringNamed("countryId", element))
                    .notBlank().longGreaterThanZero();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

    }

    public void validateForUpdate(final long familyMemberId, String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("FamilyMembers");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        baseDataValidator.reset().value(familyMemberId).notBlank().integerGreaterThanZero();

        if (this.fromApiJsonHelper.extractStringNamed("firstName", element) != null) {
            final String firstName = this.fromApiJsonHelper.extractStringNamed("firstName", element);
            baseDataValidator.reset().parameter("firstName").value(firstName).notNull().notBlank().notExceedingLengthOf(100);
        } else {
            baseDataValidator.reset().parameter("firstName").value(this.fromApiJsonHelper.extractStringNamed("firstName", element))
                    .notNull().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractStringNamed("lastName", element) != null) {
            final String lastName = this.fromApiJsonHelper.extractStringNamed("lastName", element);
            baseDataValidator.reset().parameter("lastName").value(lastName).notNull().notBlank().notExceedingLengthOf(100);
        } else {
            baseDataValidator.reset().parameter("lastName").value(this.fromApiJsonHelper.extractStringNamed("lastName", element)).notNull()
                    .notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractStringNamed("middleName", element) != null) {
            final String middleName = this.fromApiJsonHelper.extractStringNamed("middleName", element);
            baseDataValidator.reset().parameter("middleName").value(middleName).notNull().notBlank().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractStringNamed("qualification", element) != null) {
            final String qualification = this.fromApiJsonHelper.extractStringNamed("qualification", element);
            baseDataValidator.reset().parameter("qualification").value(qualification).notNull().notBlank().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractLongNamed("relationshipId", element) != null) {
            final long relationShipId = this.fromApiJsonHelper.extractLongNamed("relationshipId", element);
            baseDataValidator.reset().parameter("relationshipId").value(relationShipId).notBlank().longGreaterThanZero();

        } else {
            baseDataValidator.reset().parameter("relationshipId")
                    .value(this.fromApiJsonHelper.extractStringNamed("relationshipId", element)).notBlank().longGreaterThanZero();
        }

        if (this.fromApiJsonHelper.extractLongNamed("maritalStatusId", element) != null) {
            final long maritalStatusId = this.fromApiJsonHelper.extractLongNamed("maritalStatusId", element);
            baseDataValidator.reset().parameter("maritalStatusId").value(maritalStatusId).notBlank().longGreaterThanZero();

        }

        if (this.fromApiJsonHelper.extractLongNamed("genderId", element) != null) {
            final long genderId = this.fromApiJsonHelper.extractLongNamed("genderId", element);
            baseDataValidator.reset().parameter("genderId").value(genderId).longGreaterThanZero();

        } else {
            baseDataValidator.reset().parameter("genderId").value(this.fromApiJsonHelper.extractStringNamed("genderId", element)).notBlank()
                    .longGreaterThanZero();
        }

        if (this.fromApiJsonHelper.extractLongNamed("professionId", element) != null) {
            final long professionId = this.fromApiJsonHelper.extractLongNamed("professionId", element);
            baseDataValidator.reset().parameter("professionId").value(professionId).longGreaterThanZero();

        }

        if (this.fromApiJsonHelper.extractLocalDateNamed("dateOfBirth", element) != null) {
            final LocalDate dateOfBirth = this.fromApiJsonHelper.extractLocalDateNamed("dateOfBirth", element);
            baseDataValidator.reset().parameter("dateOfBirth").value(dateOfBirth).validateDateBefore(DateUtils.getBusinessLocalDate());

        }

        if (this.fromApiJsonHelper.extractStringNamed("email", element) != null) {
            final String email = this.fromApiJsonHelper.extractStringNamed("email", element);
            baseDataValidator.reset().parameter("email").value(email).notNull().notBlank().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractLongNamed("age", element) != null) {
            final long age = this.fromApiJsonHelper.extractLongNamed("age", element);
            baseDataValidator.reset().parameter("age").value(age).notBlank().longGreaterThanZero();

        } else {
            baseDataValidator.reset().parameter("age").value(this.fromApiJsonHelper.extractStringNamed("age", element)).notBlank()
                    .longGreaterThanZero();
        }

        if (this.fromApiJsonHelper.extractStringNamed("mobileNumber", element) != null) {
            final String mobileNumber = this.fromApiJsonHelper.extractStringNamed("mobileNumber", element);
            baseDataValidator.reset().parameter("mobileNumber").value(mobileNumber).notNull().notBlank().notExceedingLengthOf(100);
        } else {
            baseDataValidator.reset().parameter("mobileNumber").value(this.fromApiJsonHelper.extractStringNamed("mobileNumber", element))
                    .notNull().notExceedingLengthOf(100);
        }

        if (this.fromApiJsonHelper.extractLongNamed("countryId", element) != null) {
            final long countryId = this.fromApiJsonHelper.extractLongNamed("countryId", element);
            baseDataValidator.reset().parameter("countryId").value(countryId).notBlank().longGreaterThanZero();

        } else {
            baseDataValidator.reset().parameter("countryId").value(this.fromApiJsonHelper.extractStringNamed("countryId", element))
                    .notBlank().longGreaterThanZero();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

    }

    public void validateForDelete(final long familyMemberId) {

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("FamilyMembers");

        // final JsonElement element = this.fromApiJsonHelper.parse(json);

        baseDataValidator.reset().value(familyMemberId).notBlank().integerGreaterThanZero();
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

}
