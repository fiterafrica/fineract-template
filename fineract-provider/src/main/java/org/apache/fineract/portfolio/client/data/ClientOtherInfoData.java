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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;

public final class ClientOtherInfoData implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long clientId;
    private CodeValueData yearArrivedInHostCountry;
    private Integer numberOfChildren;
    private Integer numberOfDependents;
    private String coSignors;
    private String guarantor;

    private CodeValueData nationality;
    private CodeValueData strata;

    // template holder
    private Collection<CodeValueData> nationalityOptions;
    private Collection<CodeValueData> strataOptions;

    private Collection<CodeValueData> yearArrivedInHostCountryOptions;

    // company
    private String businessLocation;
    private Long taxIdentificationNumber;
    private Long incomeGeneratingActivity;
    private BigDecimal incomeGeneratingActivityMonthlyAmount;
    private String telephoneNumber;

    private String nationalIdentificationNumber;

    private String passportNumber;

    private String bankAccountNumber;

    private String bankName;

    public ClientOtherInfoData(Long id, Long clientId, CodeValueData strata, CodeValueData yearArrivedInHostCountry,
            CodeValueData nationality, Integer numberOfChildren, Integer numberOfDependents, Collection<CodeValueData> nationalityOptions,
            Collection<CodeValueData> strataOptions, Collection<CodeValueData> yearArrivedInHostCountryOptions,
            String nationalIdentificationNumber, String passportNumber, String bankAccountNumber, String bankName, String telephoneNumber) {

        this.id = id;
        this.clientId = clientId;
        this.strata = strata;
        this.yearArrivedInHostCountry = yearArrivedInHostCountry;
        this.nationality = nationality;
        this.numberOfChildren = numberOfChildren;
        this.numberOfDependents = numberOfDependents;
        this.nationalityOptions = nationalityOptions;
        this.strataOptions = strataOptions;
        this.yearArrivedInHostCountryOptions = yearArrivedInHostCountryOptions;
        this.nationalIdentificationNumber = nationalIdentificationNumber;
        this.passportNumber = passportNumber;
        this.bankAccountNumber = bankAccountNumber;
        this.bankName = bankName;
        this.telephoneNumber = telephoneNumber;
    }

    public ClientOtherInfoData(Long id, Long clientId, String coSignors, String guarantor, CodeValueData strata, String businessLocation,
            Long taxIdentificationNumber, Long incomeGeneratingActivity, BigDecimal incomeGeneratingActivityMonthlyAmount,
            String telephoneNumber) {
        this.id = id;
        this.clientId = clientId;
        this.coSignors = coSignors;
        this.guarantor = guarantor;
        this.strata = strata;
        this.businessLocation = businessLocation;
        this.taxIdentificationNumber = taxIdentificationNumber;
        this.incomeGeneratingActivity = incomeGeneratingActivity;
        this.incomeGeneratingActivityMonthlyAmount = incomeGeneratingActivityMonthlyAmount;
        this.telephoneNumber = telephoneNumber;
    }

    public static ClientOtherInfoData template(final Collection<CodeValueData> nationalityOptions,
            final Collection<CodeValueData> strataOptions, final Collection<CodeValueData> yearArrivedInHostCountryOptions) {
        Long id = null;
        Long clientId = null;
        CodeValueData strata = null;
        CodeValueData yearArrivedInHostCountry = null;
        CodeValueData nationality = null;
        Integer numberOfChildren = null;
        Integer numberOfDependents = null;

        return new ClientOtherInfoData(id, clientId, strata, yearArrivedInHostCountry, nationality, numberOfChildren, numberOfDependents,
                nationalityOptions, strataOptions, yearArrivedInHostCountryOptions, null, null, null, null, null);
    }

    public static ClientOtherInfoData instance(final Long id, final Long clientId, final CodeValueData strata,
            final CodeValueData yearArrivedInHostCountry, final CodeValueData nationality, final Integer numberOfChildren,
            final Integer numberOfDependents, final String nationalIdentificationNumber, final String passportNumber,
            final String bankAccountNumber, final String bankName, final String telephoneNumber) {
        return new ClientOtherInfoData(id, clientId, strata, yearArrivedInHostCountry, nationality, numberOfChildren, numberOfDependents,
                null, null, null, nationalIdentificationNumber, passportNumber, bankAccountNumber, bankName, telephoneNumber);
    }

    public static ClientOtherInfoData instanceEntity(final Long id, final Long clientId, String coSignors, String guarantor,
            CodeValueData strata, String businessLocation, Long taxIdentificationNumber, Long incomeGeneratingActivity,
            BigDecimal incomeGeneratingActivityMonthlyAmount, String telephoneNumber) {
        return new ClientOtherInfoData(id, clientId, coSignors, guarantor, strata, businessLocation, taxIdentificationNumber,
                incomeGeneratingActivity, incomeGeneratingActivityMonthlyAmount, telephoneNumber);
    }

    public CodeValueData getStrata() {
        return strata;
    }

}
