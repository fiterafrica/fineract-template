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
package org.apache.fineract.infrastructure.creditbureau.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.creditbureau.data.CRBREPORTTYPES;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.apache.fineract.portfolio.loanaccount.data.CrbKenyaMetropolRequestData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaConsumerVerificationData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaCorporateVerificationData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolAccountInfo;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolCrbAccountInfoRepository;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolCrbCreditInfoEnhancedReport;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolCrbCreditInfoEnhancedRepository;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolCrbIdentityReport;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolCrbIdentityVerificationRepository;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolLenderSector;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolLenderSectorRepository;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolNumberOfBouncedCheques;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolNumberOfBouncedChequesRepository;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolNumberOfCreditApplication;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolNumberOfCreditApplicationRepository;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolNumberOfEnquiries;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolNumberOfEnquiriesRepository;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolPpiAnalysis;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolPpiAnalysisRepository;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolVerifiedName;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolVerifiedNameRepository;
import org.apache.fineract.portfolio.loanaccount.service.TransUnionCrbConsumerVerificationReadPlatformService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetropolCrbVerificationWritePlatformServiceImpl implements MetropolCrbVerificationWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(MetropolCrbVerificationWritePlatformServiceImpl.class);
    public static final String FORM_URL_CONTENT_TYPE = "Content-Type";
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final TransUnionCrbConsumerVerificationReadPlatformService verificationReadPlatformService;
    private final MetropolCrbIdentityVerificationRepository metropolCrbIdentityVerificationRepository;
    private final MetropolCrbCreditInfoEnhancedRepository metropolCrbCreditInfoEnhancedRepository;
    private final MetropolCrbAccountInfoRepository accountInfoRepository;
    private final MetropolNumberOfEnquiriesRepository numberOfEnquiriesRepository;
    private final MetropolNumberOfCreditApplicationRepository numberOfCreditApplicationRepository;
    private final MetropolNumberOfBouncedChequesRepository numberOfBouncedChequesRepository;
    private final MetropolLenderSectorRepository lenderSectorRepository;
    private final MetropolPpiAnalysisRepository ppiAnalysisRepository;
    private final MetropolVerifiedNameRepository verifiedNameRepository;

    @Autowired
    private final Environment env;

    @Override
    public CommandProcessingResult loanVerificationToMetropolKenya(Long loanId, JsonCommand command) {
        Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

        TransUnionRwandaConsumerVerificationData individualClient = null;
        TransUnionRwandaCorporateVerificationData corporateClient = null;

        if (!loan.getCurrencyCode().equals("KES")) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.currency.is.not.support.on.metropol.crb.document.verification",
                    "Document verification is only supported for loans in KSH but found " + loan.getCurrencyCode() + " currency ");
        }

        Client clientObj = this.clientRepositoryWrapper.findOneWithNotFoundDetection(loan.getClientId());
        MetropolCrbIdentityReport metropolCrbIdentityReport = null;
        try {
            if (clientObj.getLegalForm().equals(LegalForm.PERSON.getValue())) {
                individualClient = this.verificationReadPlatformService.retrieveConsumer(clientObj.getId());
                metropolCrbIdentityReport = verifyIdentityDocument(individualClient.getNationalID(), loan, clientObj, "001");
            } else {
                corporateClient = this.verificationReadPlatformService.retrieveCorporate(clientObj.getId());
                metropolCrbIdentityReport = verifyIdentityDocument(corporateClient.getCompanyRegNo(), loan, clientObj, "005");
            }
        } catch (Exception e) {
            throw new GeneralPlatformDomainRuleException("Verification failed with error: ", e.getMessage());
        }
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(clientObj.getId()).withResourceIdAsString(metropolCrbIdentityReport.getId().toString()) //
                .build();
    }

    @Override
    public CommandProcessingResult loanCreditInfoEnhancedToMetropolKenya(Long loanId, JsonCommand command) {
        Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

        TransUnionRwandaConsumerVerificationData individualClient = null;
        TransUnionRwandaCorporateVerificationData corporateClient = null;

        if (!loan.getCurrencyCode().equals("KES")) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.currency.is.not.support.on.metropol.crb.credit.info.enhanced",
                    "Credit Info Enhanced is only supported for loans in KSH but found " + loan.getCurrencyCode() + " currency ");
        }

        Client clientObj = this.clientRepositoryWrapper.findOneWithNotFoundDetection(loan.getClientId());
        MetropolCrbCreditInfoEnhancedReport metropolCrbCreditInfoEnhancedReport = null;
        try {
            if (clientObj.getLegalForm().equals(LegalForm.PERSON.getValue())) {
                individualClient = this.verificationReadPlatformService.retrieveConsumer(clientObj.getId());
                metropolCrbCreditInfoEnhancedReport = verifyCreditInfoEnhanced(individualClient.getNationalID(), loan, clientObj, "001");
            } else {
                corporateClient = this.verificationReadPlatformService.retrieveCorporate(clientObj.getId());
                metropolCrbCreditInfoEnhancedReport = verifyCreditInfoEnhanced(corporateClient.getCompanyRegNo(), loan, clientObj, "005");
            }
        } catch (Exception e) {
            throw new GeneralPlatformDomainRuleException("Credit Info Enhanced report failed with error: ", e.getMessage());
        }
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(clientObj.getId()).withResourceIdAsString(metropolCrbCreditInfoEnhancedReport.getId().toString()) //
                .build();
    }

    @Override
    public CommandProcessingResult verifyLoanReportJsonOnMetropolKenya(Long loanId, JsonCommand command) {
        Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

        TransUnionRwandaConsumerVerificationData individualClient = null;
        TransUnionRwandaCorporateVerificationData corporateClient = null;

        if (!loan.getCurrencyCode().equals("KES")) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.currency.is.not.support.on.metropol.crb.report.json",
                    "Report Json is only supported for loans in KSH but found " + loan.getCurrencyCode() + " currency ");
        }

        Client clientObj = this.clientRepositoryWrapper.findOneWithNotFoundDetection(loan.getClientId());
        MetropolCrbCreditInfoEnhancedReport metropolCrbCreditInfoEnhancedReport = null;
        try {
            if (clientObj.getLegalForm().equals(LegalForm.PERSON.getValue())) {
                individualClient = this.verificationReadPlatformService.retrieveConsumer(clientObj.getId());
                metropolCrbCreditInfoEnhancedReport = verifyReportJson(individualClient.getNationalID(), loan, clientObj, "001");
            } else {
                corporateClient = this.verificationReadPlatformService.retrieveCorporate(clientObj.getId());
                metropolCrbCreditInfoEnhancedReport = verifyReportJson(corporateClient.getCompanyRegNo(), loan, clientObj, "005");
            }
        } catch (Exception e) {
            throw new GeneralPlatformDomainRuleException("Report Json failed with error: ", e.getMessage());
        }
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(clientObj.getId()).withResourceIdAsString(metropolCrbCreditInfoEnhancedReport.getId().toString()) //
                .build();
    }

    private MetropolCrbIdentityReport verifyIdentityDocument(String documentId, Loan loan, Client client, String identityType)
            throws NoSuchAlgorithmException, IOException {
        CrbKenyaMetropolRequestData requestData = new CrbKenyaMetropolRequestData(1, documentId, identityType);
        String jsonPayload = convertRequestPayloadToJson(requestData);

        String timestamp = DateUtils.generateTimestamp();
        String hash = generateHash(jsonPayload, timestamp);

        JsonObject jsonResponse = sendRequest(getConfigProperty("fineract.integrations.metropol.crb.rest.clientVerify"), jsonPayload,
                timestamp, hash, loan, client);

        MetropolCrbIdentityReport metropolCrbIdentityReport = getMetropolCrbIdentityReport(jsonResponse, loan, client);
        metropolCrbIdentityReport = metropolCrbIdentityVerificationRepository.saveAndFlush(metropolCrbIdentityReport);
        return metropolCrbIdentityReport;
    }

    private MetropolCrbCreditInfoEnhancedReport verifyCreditInfoEnhanced(String documentId, Loan loan, Client client, String identityType)
            throws NoSuchAlgorithmException, IOException {
        CrbKenyaMetropolRequestData requestData = new CrbKenyaMetropolRequestData(10, "45555", documentId, identityType,
                loan.getApprovedPrincipal().intValue(), 1);
        String jsonPayload = convertRequestPayloadToJson(requestData);

        String timestamp = DateUtils.generateTimestamp();
        String hash = generateHash(jsonPayload, timestamp);

        JsonObject jsonResponse = sendRequest(getConfigProperty("fineract.integrations.metropol.crb.rest.clientVerifyCreditInfoEnhanced"),
                jsonPayload, timestamp, hash, loan, client);

        MetropolCrbCreditInfoEnhancedReport crbCreditInfoEnhancedReport = getMetropolCrbCreditInfoEnhancedReport(jsonResponse, loan, client,
                CRBREPORTTYPES.CREDIT_INFO_ENHANCED.name());
        crbCreditInfoEnhancedReport = metropolCrbCreditInfoEnhancedRepository.saveAndFlush(crbCreditInfoEnhancedReport);
        // Save Account Info
        extractAndSaveAccountInfo(jsonResponse, crbCreditInfoEnhancedReport);
        // NumberOfEnquires
        extractAndSaveNumberOfInquiries(jsonResponse, crbCreditInfoEnhancedReport);
        // Number of credit Application
        extractAndSaveNumberOfCreditApplication(jsonResponse, crbCreditInfoEnhancedReport);
        // Number of bounched checques
        extractAndSaveNumberOfBouncedCheques(jsonResponse, crbCreditInfoEnhancedReport);
        // Lender Sector
        extractAndSaveLenderSector(jsonResponse, crbCreditInfoEnhancedReport);
        return crbCreditInfoEnhancedReport;
    }

    private JsonObject sendRequest(String urlStr, String payload, String timestamp, String hash, Loan loan, Client client)
            throws IOException {
        OkHttpClient httpClient = new OkHttpClient();

        RequestBody requestBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), payload);
        Request request = new Request.Builder().url(urlStr).post(requestBody)
                .addHeader("X-METROPOL-REST-API-KEY", getConfigProperty("fineract.integrations.metropol.crb.rest.publicKey"))
                .addHeader("X-METROPOL-REST-API-HASH", hash).addHeader("X-METROPOL-REST-API-TIMESTAMP", timestamp)
                .addHeader("Content-Type", "application/json").build();

        Response response = httpClient.newCall(request).execute();

        final String responseBodyString = response.body() != null ? response.body().string() : "";
        final JsonObject jsonResponse = JsonParser.parseString(responseBodyString).getAsJsonObject();

        if (response.isSuccessful()) {

            String resObject = response.body().string();
            LOG.info("Response from Metropol CRB: " + resObject);
            return JsonParser.parseString(resObject).getAsJsonObject();
        } else {
            if (jsonResponse.has("api_code") && "E017".equals(jsonResponse.get("api_code").getAsString())) {
                final String apiCodeDescription = jsonResponse.has("api_code_description")
                        ? jsonResponse.get("api_code_description").getAsString()
                        : "No Account Information";

                LOG.info("Response from Metropol CRB --> {}: {}", jsonResponse.get("api_code").getAsString(), apiCodeDescription);
                return null;
            }else {
                LOG.info("Response from Metropol CRB: " + response.code() + ":" + response.message() + ";" + response);
                throw new GeneralPlatformDomainRuleException("error.msg.loan.credit.info.enhanced.failed",
                        "Credit Info Enhanced failed with error: " + response.code() + ":" + response.message() + "");
            }
        }

    }

    @NotNull
    private MetropolCrbIdentityReport getMetropolCrbIdentityReport(JsonObject jsonResponse, Loan loan, Client client) {
        MetropolCrbIdentityReport metropolCrbIdentityReport = new MetropolCrbIdentityReport();

        metropolCrbIdentityReport.setClientId(client);
        metropolCrbIdentityReport.setLoanId(loan);
        metropolCrbIdentityReport.setCitizenship(getStringField(jsonResponse, "citizenship"));
        metropolCrbIdentityReport.setClan(getStringField(jsonResponse, "clan"));
        metropolCrbIdentityReport.setDateOfBirth(getStringField(jsonResponse, "dob"));
        metropolCrbIdentityReport.setDateOfDeath(getStringField(jsonResponse, "dod"));
        metropolCrbIdentityReport.setEthnicGroup(getStringField(jsonResponse, "ethnic_group"));
        metropolCrbIdentityReport.setFamily(getStringField(jsonResponse, "family"));
        metropolCrbIdentityReport.setFingerprint(getStringField(jsonResponse, "fingerprint"));
        metropolCrbIdentityReport.setFirstName(getStringField(jsonResponse, "first_name"));
        metropolCrbIdentityReport.setGender(getStringField(jsonResponse, "gender"));
        metropolCrbIdentityReport.setIdentityNumber(getStringField(jsonResponse, "id_number"));
        metropolCrbIdentityReport.setIdentityType(getStringField(jsonResponse, "identity_type"));
        metropolCrbIdentityReport.setOccupation(getStringField(jsonResponse, "occupation"));
        metropolCrbIdentityReport.setOtherName(getStringField(jsonResponse, "other_name"));
        metropolCrbIdentityReport.setPhoto(getStringField(jsonResponse, "photo"));
        metropolCrbIdentityReport.setPlaceOfBirth(getStringField(jsonResponse, "place_of_birth"));
        metropolCrbIdentityReport.setPlaceOfDeath(getStringField(jsonResponse, "place_of_death"));
        metropolCrbIdentityReport.setPlaceOfLive(getStringField(jsonResponse, "place_of_live"));
        metropolCrbIdentityReport.setRegOffice(getStringField(jsonResponse, "regoffice"));
        metropolCrbIdentityReport.setSerialNumber(getStringField(jsonResponse, "serial_number"));
        metropolCrbIdentityReport.setSignature(getStringField(jsonResponse, "signature"));
        metropolCrbIdentityReport.setSurname(getStringField(jsonResponse, "surname"));
        metropolCrbIdentityReport.setTrxId(getStringField(jsonResponse, "trx_id"));
        return metropolCrbIdentityReport;
    }

    private String generateHash(String payload, String timestamp) throws NoSuchAlgorithmException {
        String concatenatedString = getConfigProperty("fineract.integrations.metropol.crb.rest.privateKey") + payload
                + getConfigProperty("fineract.integrations.metropol.crb.rest.publicKey") + timestamp;
        LOG.info("Concatenated string: " + concatenatedString);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(concatenatedString.getBytes(StandardCharsets.UTF_8));

        StringBuilder hashHex = new StringBuilder();
        for (byte b : hashBytes) {
            hashHex.append(String.format("%02x", b));
        }

        return hashHex.toString();

    }

    private String getConfigProperty(String propertyName) {
        return this.env.getProperty(propertyName);
    }

    private String convertRequestPayloadToJson(CrbKenyaMetropolRequestData requestData) {
        Gson gson = new GsonBuilder().create();
        String request = gson.toJson(requestData);
        LOG.info("Actual Payload to be sent - - >" + request);
        return request;
    }

    public String getStringField(JsonObject jsonObject, String fieldName) {
        if (jsonObject != null && jsonObject.has(fieldName) && jsonObject.get(fieldName).isJsonPrimitive()
                && jsonObject.get(fieldName).getAsJsonPrimitive().isString()) {
            return jsonObject.get(fieldName).getAsString();
        }
        return null;
    }

    public Boolean getBooleanField(JsonObject jsonObject, String fieldName) {
        if (jsonObject != null && jsonObject.has(fieldName) && jsonObject.get(fieldName).isJsonPrimitive()
                && jsonObject.get(fieldName).getAsJsonPrimitive().isBoolean()) {
            return jsonObject.get(fieldName).getAsBoolean();
        }
        return null;
    }

    public Integer getIntegerField(JsonObject jsonObject, String fieldName) {
        if (jsonObject != null && jsonObject.has(fieldName) && jsonObject.get(fieldName).isJsonPrimitive()
                && jsonObject.get(fieldName).getAsJsonPrimitive().isNumber()) {
            return jsonObject.get(fieldName).getAsInt();
        }
        return null;
    }

    public Double getDoubleField(JsonObject jsonObject, String fieldName) {
        if (jsonObject != null && jsonObject.has(fieldName) && jsonObject.get(fieldName).isJsonPrimitive()
                && jsonObject.get(fieldName).getAsJsonPrimitive().isNumber()) {
            return jsonObject.get(fieldName).getAsDouble();
        }
        return null;
    }

    @NotNull
    private MetropolCrbCreditInfoEnhancedReport getMetropolCrbCreditInfoEnhancedReport(JsonObject jsonResponse, Loan loan, Client client,
            String reportType) {
        MetropolCrbCreditInfoEnhancedReport enhancedReport = new MetropolCrbCreditInfoEnhancedReport();

        enhancedReport.setClientId(client);
        enhancedReport.setLoanId(loan);
        enhancedReport.setReportType(reportType);
        enhancedReport.setApiCode(getStringField(jsonResponse, "api_code"));
        enhancedReport.setApiCodeDescription(getStringField(jsonResponse, "api_code_description"));
        enhancedReport.setApplicationRefNo(getStringField(jsonResponse, "application_ref_no"));
        enhancedReport.setCreditScore(getIntegerField(jsonResponse, "credit_score").toString());
        enhancedReport.setDelinquencyCode(getStringField(jsonResponse, "delinquency_code"));
        enhancedReport.setHasError(getBooleanField(jsonResponse, "has_error"));
        enhancedReport.setHasFraud(getBooleanField(jsonResponse, "has_fraud"));
        enhancedReport.setIdentityNumber(getStringField(jsonResponse, "identity_number"));
        enhancedReport.setIdentityType(getStringField(jsonResponse, "identity_type"));
        enhancedReport.setIsGuarantor(getBooleanField(jsonResponse, "is_guarantor"));
        enhancedReport.setTrxId(getStringField(jsonResponse, "trx_id"));
        return enhancedReport;
    }

    private void extractAndSaveAccountInfo(JsonObject jsonResponse, MetropolCrbCreditInfoEnhancedReport crbCreditInfoEnhancedReport) {

        JsonArray accountInfoArray = jsonResponse.getAsJsonArray("account_info");

        if (accountInfoArray != null) {
            for (JsonElement accountInfoElement : accountInfoArray) {
                if (accountInfoElement.isJsonObject()) {
                    MetropolAccountInfo enhancedReport = getMetropolCrbCreditInfoEnhancedReport(accountInfoElement.getAsJsonObject(),
                            crbCreditInfoEnhancedReport);
                    accountInfoRepository.saveAndFlush(enhancedReport);
                }
            }
        }

    }

    @NotNull
    private MetropolAccountInfo getMetropolCrbCreditInfoEnhancedReport(JsonObject jsonResponse,
            MetropolCrbCreditInfoEnhancedReport crbCreditInfoEnhancedReport) {

        MetropolAccountInfo acc = new MetropolAccountInfo();

        acc.setCrbCreditInfoEnhancedReport(crbCreditInfoEnhancedReport);
        acc.setAccountNumber(getStringField(jsonResponse, "account_number"));
        acc.setAccountStatus(getStringField(jsonResponse, "account_status"));
        acc.setCurrentBalance(getStringField(jsonResponse, "current_balance"));
        acc.setDateOpened(getStringField(jsonResponse, "date_opened"));
        acc.setDaysInArrears(getIntegerField(jsonResponse, "days_in_arrears"));
        acc.setDelinquencyCode(getStringField(jsonResponse, "delinquency_code"));
        acc.setHighestDaysInArrears(getIntegerField(jsonResponse, "highest_days_in_arrears"));
        acc.setIsYourAccount(getBooleanField(jsonResponse, "is_your_account"));
        acc.setLastPaymentAmount(getStringField(jsonResponse, "last_payment_amount"));
        acc.setLastPaymentDate(getStringField(jsonResponse, "last_payment_date"));
        acc.setLoadedAt(getStringField(jsonResponse, "loaded_at"));
        acc.setOriginalAmount(getStringField(jsonResponse, "original_amount"));
        acc.setOverdueBalance(getStringField(jsonResponse, "overdue_balance"));
        acc.setOverdueDate(getStringField(jsonResponse, "overdue_date"));
        acc.setProductTypeId(getIntegerField(jsonResponse, "product_type_id"));
        return acc;
    }

    @NotNull
    private void extractAndSaveNumberOfInquiries(JsonObject jsonResponse, MetropolCrbCreditInfoEnhancedReport crbCreditInfoEnhancedReport) {
        JsonObject obj = jsonResponse.getAsJsonObject("no_of_enquiries");

        MetropolNumberOfEnquiries num = new MetropolNumberOfEnquiries();
        if (obj != null) {
            num.setCrbCreditInfoEnhancedReport(crbCreditInfoEnhancedReport);
            num.setLast12Months(getIntegerField(obj, "last_12_months"));
            num.setLast3Months(getIntegerField(obj, "last_3_months"));
            num.setLast6Months(getIntegerField(obj, "last_6_months"));
            numberOfEnquiriesRepository.saveAndFlush(num);
        }
    }

    @NotNull
    private void extractAndSaveNumberOfCreditApplication(JsonObject jsonResponse,
            MetropolCrbCreditInfoEnhancedReport crbCreditInfoEnhancedReport) {
        JsonObject obj = jsonResponse.getAsJsonObject("no_of_credit_applications");

        MetropolNumberOfCreditApplication creditApplication = new MetropolNumberOfCreditApplication();
        if (obj != null) {
            creditApplication.setCrbCreditInfoEnhancedReport(crbCreditInfoEnhancedReport);
            creditApplication.setLast12Months(getIntegerField(obj, "last_12_months"));
            creditApplication.setLast3Months(getIntegerField(obj, "last_3_months"));
            creditApplication.setLast6Months(getIntegerField(obj, "last_6_months"));
            numberOfCreditApplicationRepository.saveAndFlush(creditApplication);
        }
    }

    @NotNull
    private void extractAndSaveNumberOfBouncedCheques(JsonObject jsonResponse,
            MetropolCrbCreditInfoEnhancedReport crbCreditInfoEnhancedReport) {
        JsonObject obj = jsonResponse.getAsJsonObject("no_of_bounced_cheques");

        MetropolNumberOfBouncedCheques numberOfBouncedCheques = new MetropolNumberOfBouncedCheques();
        if (obj != null) {
            numberOfBouncedCheques.setCrbCreditInfoEnhancedReport(crbCreditInfoEnhancedReport);
            numberOfBouncedCheques.setLast12Months(getIntegerField(obj, "last_12_months"));
            numberOfBouncedCheques.setLast3Months(getIntegerField(obj, "last_3_months"));
            numberOfBouncedCheques.setLast6Months(getIntegerField(obj, "last_6_months"));
            numberOfBouncedChequesRepository.saveAndFlush(numberOfBouncedCheques);
        }
    }

    @NotNull
    private void extractAndSaveLenderSector(JsonObject jsonResponse, MetropolCrbCreditInfoEnhancedReport crbCreditInfoEnhancedReport) {
        JsonObject obj = jsonResponse.getAsJsonObject("lender_sector");

        MetropolLenderSector lenderSector = new MetropolLenderSector();
        if (obj != null) {
            JsonObject sectorBankJson = obj.getAsJsonObject("sector_bank");
            JsonObject sectorOtherJson = obj.getAsJsonObject("sector_other");

            lenderSector.setCrbCreditInfoEnhancedReport(crbCreditInfoEnhancedReport);
            if (sectorBankJson != null) {
                lenderSector.setBankAccountNpa(getIntegerField(sectorBankJson, "account_npa"));
                lenderSector.setBankAccountPerforming(getIntegerField(sectorBankJson, "account_performing"));
                lenderSector.setBankAccountPerformingNpaHistory(getIntegerField(sectorBankJson, "account_performing_npa_history"));
            }

            if (sectorOtherJson != null) {
                lenderSector.setOtherAccountNpa(getIntegerField(sectorOtherJson, "account_npa"));
                lenderSector.setOtherAccountPerforming(getIntegerField(sectorOtherJson, "account_performing"));
                lenderSector.setOtherAccountPerformingNpaHistory(getIntegerField(sectorOtherJson, "account_performing_npa_history"));
            }
            lenderSectorRepository.saveAndFlush(lenderSector);
        }
    }

    @NotNull
    private void extractAndSavePpiAnalysis(JsonObject jsonResponse, MetropolCrbCreditInfoEnhancedReport crbCreditInfoEnhancedReport) {
        JsonObject obj = jsonResponse.getAsJsonObject("ppi_analysis");

        MetropolPpiAnalysis ppiAnalysis = new MetropolPpiAnalysis();
        if (obj != null) {
            ppiAnalysis.setCrbCreditInfoEnhancedReport(crbCreditInfoEnhancedReport);
            ppiAnalysis.setMonth(getStringField(obj, "month"));
            ppiAnalysis.setPpi(getDoubleField(obj, "ppi"));
            ppiAnalysis.setPpiRank(getStringField(obj, "ppi_rank"));
            ppiAnalysisRepository.saveAndFlush(ppiAnalysis);
        }
    }

    @NotNull
    private void extractAndSaveVerifiedName(JsonObject jsonResponse, MetropolCrbCreditInfoEnhancedReport crbCreditInfoEnhancedReport) {
        JsonObject obj = jsonResponse.getAsJsonObject("verified_name");

        MetropolVerifiedName verifiedName = new MetropolVerifiedName();
        if (obj != null) {
            verifiedName.setCrbCreditInfoEnhancedReport(crbCreditInfoEnhancedReport);
            verifiedName.setFirstName(getStringField(obj, "first_name"));
            verifiedName.setOtherName(getStringField(obj, "other_name"));
            verifiedName.setSurname(getStringField(obj, "surname"));
            verifiedNameRepository.saveAndFlush(verifiedName);
        }
    }

    private MetropolCrbCreditInfoEnhancedReport verifyReportJson(String documentId, Loan loan, Client client, String identityType)
            throws NoSuchAlgorithmException, IOException {
        CrbKenyaMetropolRequestData requestData = new CrbKenyaMetropolRequestData(5, documentId, identityType,
                loan.getApprovedPrincipal().intValue(), 1);
        String jsonPayload = convertRequestPayloadToJson(requestData);

        String timestamp = DateUtils.generateTimestamp();
        String hash = generateHash(jsonPayload, timestamp);

        JsonObject jsonResponse = sendRequest(getConfigProperty("fineract.integrations.metropol.crb.rest.reportJson"), jsonPayload,
                timestamp, hash, loan, client);

        MetropolCrbCreditInfoEnhancedReport crbCreditInfoEnhancedReport = getMetropolCrbCreditInfoEnhancedReport(jsonResponse, loan, client,
                CRBREPORTTYPES.JSON_REPORT.name());
        crbCreditInfoEnhancedReport = metropolCrbCreditInfoEnhancedRepository.saveAndFlush(crbCreditInfoEnhancedReport);
        // Save Account Info
        extractAndSaveAccountInfo(jsonResponse, crbCreditInfoEnhancedReport);
        // NumberOfEnquires
        extractAndSaveNumberOfInquiries(jsonResponse, crbCreditInfoEnhancedReport);
        // Number of credit Application
        extractAndSaveNumberOfCreditApplication(jsonResponse, crbCreditInfoEnhancedReport);
        // Number of bounched checques
        extractAndSaveNumberOfBouncedCheques(jsonResponse, crbCreditInfoEnhancedReport);
        // Lender Sector
        extractAndSaveLenderSector(jsonResponse, crbCreditInfoEnhancedReport);
        // PPI Analysis
        extractAndSavePpiAnalysis(jsonResponse, crbCreditInfoEnhancedReport);
        // Verified Name
        extractAndSaveVerifiedName(jsonResponse, crbCreditInfoEnhancedReport);
        return crbCreditInfoEnhancedReport;
    }
}
