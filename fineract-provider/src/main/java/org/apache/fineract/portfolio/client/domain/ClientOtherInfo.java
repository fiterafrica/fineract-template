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
package org.apache.fineract.portfolio.client.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;

@Entity
@Table(name = "m_client_other_info")
public class ClientOtherInfo extends AbstractPersistableCustom {

    @OneToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    @ManyToOne
    @JoinColumn(name = "strata_cv_id", nullable = false)
    private CodeValue strata;
    @ManyToOne
    @JoinColumn(name = "year_arrived_in_country_cv_id")
    private CodeValue yearArrivedInHostCountry;

    @ManyToOne
    @JoinColumn(name = "nationality_cv_id")
    private CodeValue nationality;

    @Column(name = "number_of_children")
    private Integer numberOfChildren;

    @Column(name = "number_of_dependents")
    private Integer numberOfDependents;

    @Column(name = "co_signors", unique = true)
    private String coSignors;

    @Column(name = "guarantor")
    private String guarantor;

    @Column(name = "business_location")
    private String businessLocation;

    @Column(name = "tax_identification_number")
    private Long taxIdentificationNumber;

    @Column(name = "income_generating_activity")
    private Long incomeGeneratingActivity;

    @Column(name = "income_generating_activity_monthly_amount")
    private BigDecimal incomeGeneratingActivityMonthlyAmount;

    @Column(name = "telephone_no")
    private String telephoneNo;

    @Column(name = "national_identification_number")
    private String nationalIdentificationNumber;

    @Column(name = "passport_number")
    private String passportNumber;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_name")
    private String bankName;

    public ClientOtherInfo() {}

    public ClientOtherInfo(Client client, CodeValue strata, CodeValue yearArrivedInHostCountry, CodeValue nationality,
            Integer numberOfChildren, Integer numberOfDependents, String nationalIdentificationNumber, String passportNumber,
            String bankAccountNumber, String bankName, String telephoneNo) {
        this.client = client;
        this.strata = strata;
        this.yearArrivedInHostCountry = yearArrivedInHostCountry;
        this.nationality = nationality;
        this.numberOfChildren = numberOfChildren;
        this.numberOfDependents = numberOfDependents;
        this.nationalIdentificationNumber = nationalIdentificationNumber;
        this.passportNumber = passportNumber;
        this.bankAccountNumber = bankAccountNumber;
        this.bankName = bankName;
        this.telephoneNo = telephoneNo;

    }

    public ClientOtherInfo(Client client, CodeValue strata, final String businessLocation, final Long taxIdentificationNumber,
            final Long incomeGeneratingActivity, final BigDecimal incomeGeneratingActivityMonthlyAmount, final String telephoneNo,
            final String coSignors, final String guarantor) {
        this.client = client;
        this.strata = strata;
        this.businessLocation = businessLocation;
        this.taxIdentificationNumber = taxIdentificationNumber;
        this.incomeGeneratingActivity = incomeGeneratingActivity;
        this.incomeGeneratingActivityMonthlyAmount = incomeGeneratingActivityMonthlyAmount;
        this.telephoneNo = telephoneNo;
        this.coSignors = coSignors;
        this.guarantor = guarantor;

    }

    public static ClientOtherInfo createNew(JsonCommand command, Client client, final CodeValue strata, final CodeValue nationality,
            final CodeValue yearArrivedInHostCountry) {

        final Integer numberOfChildren = command.integerValueOfParameterNamed(ClientApiConstants.numberOfChildren);
        final Integer numberOfDependents = command.integerValueOfParameterNamed(ClientApiConstants.numberOfDependents);
        final String nationalIdentificationNumber = command.stringValueOfParameterNamed(ClientApiConstants.NATIONAL_IDENTIFICATION_NUMBER);
        final String passportNumber = command.stringValueOfParameterNamed(ClientApiConstants.PASSPORT_NUMBER);
        final String bankAccountNumber = command.stringValueOfParameterNamed(ClientApiConstants.BANK_ACCOUNT_NUMBER);
        final String bankName = command.stringValueOfParameterNamed(ClientApiConstants.BANK_NAME);
        final String telephoneNo = command.stringValueOfParameterNamed(ClientApiConstants.telephoneNoParamName);
        return new ClientOtherInfo(client, strata, yearArrivedInHostCountry, nationality, numberOfChildren, numberOfDependents,
                nationalIdentificationNumber, passportNumber, bankAccountNumber, bankName, telephoneNo);
    }

    public static ClientOtherInfo createNewForEntity(JsonCommand command, Client client, final CodeValue strata) {

        final String coSignors = command.stringValueOfParameterNamedAllowingNull(ClientApiConstants.coSignors);
        final String guarantor = command.stringValueOfParameterNamedAllowingNull(ClientApiConstants.guarantor);
        final String businessLocation = command.stringValueOfParameterNamedAllowingNull(ClientApiConstants.businessLocationParamName);
        final Long taxIdentificationNumber = command.longValueOfParameterNamed(ClientApiConstants.taxIdentificationNumberParamName);
        final Long incomeGeneratingActivity = command.longValueOfParameterNamed(ClientApiConstants.incomeGeneratingActivityParamName);
        final BigDecimal incomeGeneratingActivityMonthlyAmount = command
                .bigDecimalValueOfParameterNamed(ClientApiConstants.incomeGeneratingActivityMonthlyAmountParamName);
        final String telephoneNo = command.stringValueOfParameterNamedAllowingNull(ClientApiConstants.telephoneNoParamName);

        return new ClientOtherInfo(client, strata, businessLocation, taxIdentificationNumber, incomeGeneratingActivity,
                incomeGeneratingActivityMonthlyAmount, telephoneNo, coSignors, guarantor);
    }

    public Map<String, Object> update(final JsonCommand command, final Integer legalFormId) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

        if (command.isChangeInLongParameterNamed(ClientApiConstants.strataIdParamName, strataId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.strataIdParamName);
            actualChanges.put(ClientApiConstants.strataIdParamName, newValue);
        }

        if (LegalForm.fromInt(legalFormId).isPerson()) {
            if (command.isChangeInLongParameterNamed(ClientApiConstants.nationalityIdParamName, nationalityId())) {
                final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.nationalityIdParamName);
                actualChanges.put(ClientApiConstants.nationalityIdParamName, newValue);
            }

            if (command.isChangeInIntegerParameterNamed(ClientApiConstants.numberOfChildren, this.numberOfChildren)) {
                final Integer newValue = command.integerValueOfParameterNamed(ClientApiConstants.numberOfChildren);
                actualChanges.put(ClientApiConstants.numberOfChildren, newValue);
                this.numberOfChildren = newValue;
            }

            if (command.isChangeInIntegerParameterNamed(ClientApiConstants.numberOfDependents, this.numberOfDependents)) {
                final Integer newValue = command.integerValueOfParameterNamed(ClientApiConstants.numberOfDependents);
                actualChanges.put(ClientApiConstants.numberOfDependents, newValue);
                this.numberOfDependents = newValue;
            }

            if (command.isChangeInLongParameterNamed(ClientApiConstants.yearArrivedInHostCountry, yearArrivedInHostCountryId())) {
                final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.yearArrivedInHostCountry);
                actualChanges.put(ClientApiConstants.yearArrivedInHostCountry, newValue);
            }
            if (command.isChangeInStringParameterNamed(ClientApiConstants.NATIONAL_IDENTIFICATION_NUMBER,
                    this.nationalIdentificationNumber)) {
                final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.NATIONAL_IDENTIFICATION_NUMBER);
                actualChanges.put(ClientApiConstants.NATIONAL_IDENTIFICATION_NUMBER, newValue);
                this.nationalIdentificationNumber = newValue;
            }
            if (command.isChangeInStringParameterNamed(ClientApiConstants.PASSPORT_NUMBER, this.passportNumber)) {
                final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.PASSPORT_NUMBER);
                actualChanges.put(ClientApiConstants.PASSPORT_NUMBER, newValue);
                this.passportNumber = newValue;
            }
            if (command.isChangeInStringParameterNamed(ClientApiConstants.BANK_ACCOUNT_NUMBER, this.bankAccountNumber)) {
                final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.BANK_ACCOUNT_NUMBER);
                actualChanges.put(ClientApiConstants.BANK_ACCOUNT_NUMBER, newValue);
                this.bankAccountNumber = newValue;
            }
            if (command.isChangeInStringParameterNamed(ClientApiConstants.BANK_NAME, this.bankName)) {
                final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.BANK_NAME);
                actualChanges.put(ClientApiConstants.BANK_NAME, newValue);
                this.bankName = newValue;
            }
            if (command.isChangeInStringParameterNamed(ClientApiConstants.telephoneNoParamName, this.telephoneNo)) {
                final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.telephoneNoParamName);
                actualChanges.put(ClientApiConstants.telephoneNoParamName, newValue);
                this.telephoneNo = newValue;
            }

        } else if (LegalForm.fromInt(legalFormId).isEntity()) {
            if (command.isChangeInStringParameterNamed(ClientApiConstants.businessLocationParamName, this.businessLocation)) {
                final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.businessLocationParamName);
                actualChanges.put(ClientApiConstants.businessLocationParamName, newValue);
                this.businessLocation = newValue;
            }
            if (command.isChangeInLongParameterNamed(ClientApiConstants.taxIdentificationNumberParamName, this.taxIdentificationNumber)) {
                final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.taxIdentificationNumberParamName);
                actualChanges.put(ClientApiConstants.taxIdentificationNumberParamName, newValue);
                this.taxIdentificationNumber = newValue;
            }
            if (command.isChangeInLongParameterNamed(ClientApiConstants.incomeGeneratingActivityParamName, this.incomeGeneratingActivity)) {
                final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.incomeGeneratingActivityParamName);
                actualChanges.put(ClientApiConstants.incomeGeneratingActivityParamName, newValue);
                this.incomeGeneratingActivity = newValue;
            }
            if (command.isChangeInBigDecimalParameterNamed(ClientApiConstants.incomeGeneratingActivityMonthlyAmountParamName,
                    this.incomeGeneratingActivityMonthlyAmount)) {
                final BigDecimal newValue = command
                        .bigDecimalValueOfParameterNamed(ClientApiConstants.incomeGeneratingActivityMonthlyAmountParamName);
                actualChanges.put(ClientApiConstants.incomeGeneratingActivityMonthlyAmountParamName, newValue);
                this.incomeGeneratingActivityMonthlyAmount = newValue;
            }
            if (command.isChangeInStringParameterNamed(ClientApiConstants.telephoneNoParamName, this.getTelephoneNo())) {
                final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.telephoneNoParamName);
                actualChanges.put(ClientApiConstants.telephoneNoParamName, newValue);
                this.telephoneNo = newValue;
            }
            if (command.isChangeInStringParameterNamed(ClientApiConstants.coSignors, this.coSignors)) {
                final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.coSignors);
                actualChanges.put(ClientApiConstants.coSignors, newValue);
                this.coSignors = newValue;
            }
            if (command.isChangeInStringParameterNamed(ClientApiConstants.guarantor, this.guarantor)) {
                final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.guarantor);
                actualChanges.put(ClientApiConstants.guarantor, newValue);
                this.guarantor = newValue;
            }
        }
        return actualChanges;
    }

    private Long strataId() {
        Long strata = null;
        if (this.strata != null) {
            strata = this.strata.getId();
        }
        return strata;
    }

    private Long nationalityId() {
        Long nationality = null;
        if (this.nationality != null) {
            nationality = this.nationality.getId();
        }
        return nationality;
    }

    private Long yearArrivedInHostCountryId() {
        Long yearArrivedInHostCountry = null;
        if (this.yearArrivedInHostCountry != null) {
            yearArrivedInHostCountry = this.yearArrivedInHostCountry.getId();
        }
        return yearArrivedInHostCountry;
    }

    public void setStrata(CodeValue strata) {
        this.strata = strata;
    }

    public void setNationality(CodeValue nationality) {
        this.nationality = nationality;
    }

    public void setYearArrivedInHostCountry(CodeValue yearArrivedInHostCountry) {
        this.yearArrivedInHostCountry = yearArrivedInHostCountry;
    }

    public Long clientId() {
        return this.client.getId();
    }

    public Client getClient() {
        return client;
    }

    public String getTelephoneNo() {
        return telephoneNo;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public String getBankName() {
        return bankName;
    }
}
