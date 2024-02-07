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
package org.apache.fineract.portfolio.loanaccount.data;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class TransUnionRwandaCrbReportData {

    private Integer id;
    private String crbName;
    private String pdfId;
    private String productDisplayName;
    private String reportDate;
    private String reportType;
    private String requestNo;
    private String requester;
    private LocalDate createdOnDate;
    private String personalCrn;
    private String dateOfBirth;
    private String fullName;
    private String gender;
    private String healthInsuranceNo;
    private String maritalStatus;
    private String nationalId;
    private String otherNames;
    private String salutation;
    private String surname;
    private String corporateCrn;
    private String companyRegNo;
    private String companyName;
    private String grade;
    private String positiveScore;
    private String possibility;
    private String reasonCodeAarc1;
    private String reasonCodeAarc2;
    private String reasonCodeAarc3;
    private String reasonCodeAarc4;
    private Integer clientType;
    private TransUnionRwandaCrbSummaryReportData transUnionRwandaCrbSummaryReportData;
    private List<TransUnionRwandaCrbAccountReportData> accountReportDataList;

    public TransUnionRwandaCrbReportData(Integer id, String crbName, String pdfId, String productDisplayName, String reportDate,
            String reportType, String requestNo, String requester, LocalDate createdOnDate, String personalCrn, String dateOfBirth,
            String fullName, String gender, String healthInsuranceNo, String maritalStatus, String nationalId, String otherNames,
            String salutation, String surname, String corporateCrn, String companyRegNo, String companyName, String grade,
            String positiveScore, String possibility, String reasonCodeAarc1, String reasonCodeAarc2, String reasonCodeAarc3,
            String reasonCodeAarc4, Integer clientType) {
        this.id = id;
        this.crbName = crbName;
        this.pdfId = pdfId;
        this.productDisplayName = productDisplayName;
        this.reportDate = reportDate;
        this.reportType = reportType;
        this.requestNo = requestNo;
        this.requester = requester;
        this.createdOnDate = createdOnDate;
        this.personalCrn = personalCrn;
        this.dateOfBirth = dateOfBirth;
        this.fullName = fullName;
        this.gender = gender;
        this.healthInsuranceNo = healthInsuranceNo;
        this.maritalStatus = maritalStatus;
        this.nationalId = nationalId;
        this.otherNames = otherNames;
        this.salutation = salutation;
        this.surname = surname;
        this.corporateCrn = corporateCrn;
        this.companyRegNo = companyRegNo;
        this.companyName = companyName;
        this.grade = grade;
        this.positiveScore = positiveScore;
        this.possibility = possibility;
        this.reasonCodeAarc1 = reasonCodeAarc1;
        this.reasonCodeAarc2 = reasonCodeAarc2;
        this.reasonCodeAarc3 = reasonCodeAarc3;
        this.reasonCodeAarc4 = reasonCodeAarc4;
        this.clientType = clientType;
    }

}
