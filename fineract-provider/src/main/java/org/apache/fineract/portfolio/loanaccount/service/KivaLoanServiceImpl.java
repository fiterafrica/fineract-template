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
package org.apache.fineract.portfolio.loanaccount.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections.CollectionUtils;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.dataqueries.data.GenericResultsetData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetRowData;
import org.apache.fineract.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.apache.fineract.infrastructure.documentmanagement.data.DocumentData;
import org.apache.fineract.infrastructure.documentmanagement.domain.StorageType;
import org.apache.fineract.infrastructure.documentmanagement.service.DocumentReadPlatformServiceImpl;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRecruitmentSurvey;
import org.apache.fineract.portfolio.client.domain.ClientRecruitmentSurveyRepository;
import org.apache.fineract.portfolio.loanaccount.data.KivaLoanAccount;
import org.apache.fineract.portfolio.loanaccount.data.KivaLoanAccountSchedule;
import org.apache.fineract.portfolio.loanaccount.data.KivaLoanAccountScheduleParameters;
import org.apache.fineract.portfolio.loanaccount.data.KivaLoanAwaitingApprovalData;
import org.apache.fineract.portfolio.loanaccount.data.KivaLoanAwaitingRepaymentData;
import org.apache.fineract.portfolio.loanaccount.data.KivaLoanData;
import org.apache.fineract.portfolio.loanaccount.data.KivaLoanRepaymentData;
import org.apache.fineract.portfolio.loanaccount.data.KivaLocaleData;
import org.apache.fineract.portfolio.loanaccount.data.KivaLocationData;
import org.apache.fineract.portfolio.loanaccount.data.KivaSupportedCurrencyData;
import org.apache.fineract.portfolio.loanaccount.data.KivaSupportedLocationData;
import org.apache.fineract.portfolio.loanaccount.data.KivaSupportedThemeData;
import org.apache.fineract.portfolio.loanaccount.data.KivaLoanExceptions;
import org.apache.fineract.portfolio.loanaccount.data.LoanDetailToKivaData;
import org.apache.fineract.portfolio.loanaccount.data.ThemeData;
import org.apache.fineract.portfolio.loanaccount.domain.KivaCurrency;
import org.apache.fineract.portfolio.loanaccount.domain.KivaCurrencyRepository;
import org.apache.fineract.portfolio.loanaccount.domain.KivaLoanAwaitingApproval;
import org.apache.fineract.portfolio.loanaccount.domain.KivaLoanAwaitingApprovalRepository;
import org.apache.fineract.portfolio.loanaccount.domain.KivaLocation;
import org.apache.fineract.portfolio.loanaccount.domain.KivaLocationRepository;
import org.apache.fineract.portfolio.loanaccount.domain.KivaTheme;
import org.apache.fineract.portfolio.loanaccount.domain.KivaThemeRepository;
import org.apache.fineract.portfolio.loanaccount.domain.KivaSectorActivity;
import org.apache.fineract.portfolio.loanaccount.domain.KivaSectorActivityRepository;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanDueDiligenceException;
import org.apache.fineract.portfolio.loanaccount.serialization.KivaDateSerializerApi;
import org.apache.fineract.portfolio.loanproduct.service.LoanEnumerations;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class KivaLoanServiceImpl implements KivaLoanService {

    private static final Logger LOG = LoggerFactory.getLogger(KivaLoanServiceImpl.class);
    public static final String FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    public static final String FORM_URL_CONTENT_TYPE = "Content-Type";
    public static final String LOAN_STATUS = "payingBack";
    public static final Integer INITIAL_LOAN_LIMIT = 100;
    public static final Long ACTIVITY_ID = 110L;
    public static final Integer DESCRIPTION_LANGUAGE_ID = 1;

    private final LoanRepository loanRepository;
    private final DocumentReadPlatformServiceImpl documentReadPlatformService;
    private final ClientRecruitmentSurveyRepository clientRecruitmentSurveyRepository;
    private final KivaLoanAwaitingApprovalRepository kivaLoanAwaitingApprovalRepository;
    private final KivaLoanAwaitingApprovalReadPlatformService kivaLoanAwaitingApprovalReadPlatformService;
    private final KivaThemeRepository kivaThemeRepository;
    private final KivaLocationRepository kivaLocationRepository;
    private final KivaCurrencyRepository kivaCurrencyRepository;
    private final KivaSectorActivityRepository kivaSectorActivityRepository;
    private final ReadWriteNonCoreDataService readWriteNonCoreDataService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    @Autowired
    private Environment env;

    private static final String KIVA_DEPARTMENT_NOT_FOUND = "Loan Department not supported by kiva";
    private static final String KIVA_SECTOR_ACTIVITY_MISMATCH = "The Sector and Activity are not compatible with Kiva";

    @JsonIgnoreProperties({"stackTrace", "suppressed", "localizedMessage"})
    static abstract class ThrowableMixin {}
    @Override
    @CronTarget(jobName = JobName.POST_LOAN_ACCOUNTS_TO_KIVA)
    public void postLoanAccountsToKiva() {
        // Authenticate to KIVA
        String accessToken = authenticateToKiva();

        List<KivaLoanAccountSchedule> kivaLoanAccountSchedules = new ArrayList<>();
        List<KivaLoanAccount> kivaLoanAccounts = new ArrayList<>();
        List<Boolean> notPictured = new ArrayList<>(); // use it when need to send group loans
        notPictured.add(Boolean.TRUE);

        List<Loan> loanList = loanRepository.findLoanAccountsToBePostedToKiva();

        ArrayList<KivaLoanExceptions> kivaLoanExceptions = new ArrayList<>();

        List<Throwable> exceptions = new ArrayList<>();

        LOG.info("Posting this Loan Account To Kiva And Size = = > " + loanList.size());
        if (!CollectionUtils.isEmpty(loanList)) {
            for (Loan loan : loanList) {
                try {
                    kivaLoanAccounts.clear();
                    kivaLoanAccountSchedules.clear();
                    String loanToKiva = loanPayloadToKivaMapper(kivaLoanAccountSchedules, kivaLoanAccounts, notPictured, loan);
                    LOG.info("Loan Account To be Sent to Kiva : =GSON = >  " + loanToKiva);
                    String loanDraftUUID = postLoanToKiva(accessToken, loanToKiva);
                    loan.setKivaUUId(loanDraftUUID);
                    loanRepository.saveAndFlush(loan);
                } catch (Exception e) {
                    log.error("Post Loan to KIVA has failed" + e);
                    KivaLoanExceptions kivaLoanException = new KivaLoanExceptions();
                    kivaLoanException.setError(e);
                    kivaLoanException.setLoanId(loan.getAccountNumber());
                    kivaLoanExceptions.add(kivaLoanException);
                }
            }
            try{
                if(!CollectionUtils.isEmpty(kivaLoanExceptions)){
                    postKivaExceptions(kivaLoanExceptions);
                    RuntimeException exception = new RuntimeException("There were " + kivaLoanExceptions.size() + " kiva exceptions.");
                    exceptions.add(exception);
                }}
            catch (Exception e){
                log.error("Could not post errors to external service" + e);
                exceptions.add(e);
            }
            if (!CollectionUtils.isEmpty(exceptions)) {
                try {
                    throw new JobExecutionException(exceptions);
                } catch (JobExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void postKivaExceptions(ArrayList<KivaLoanExceptions> kivaLoanExceptions) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(Throwable.class, ThrowableMixin.class);

        String json = null;
        try {
            json = mapper.writeValueAsString(kivaLoanExceptions);

            OkHttpClient client = new OkHttpClient().newBuilder().build();

            RequestBody body = RequestBody.create(json, MediaType.parse(org.springframework.http.MediaType.APPLICATION_JSON_VALUE));
            Request request = new Request.Builder()
                    .url(getConfigProperty("fineract.integrations.kiva.logsUrl"))
                    .method("POST", body)
                    .addHeader(FORM_URL_CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                    .build();
                Response response = client.newCall(request).execute();

                log.info("External log service response: "+ response.body().string());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @CronTarget(jobName = JobName.POST_LOAN_REPAYMENTS_TO_KIVA)
    public void postLoanRepaymentsToKiva() throws JobExecutionException {
        // Authenticate to KIVA
        String accessToken = authenticateToKiva();
        // Reset this table to receive new loan data
        kivaLoanAwaitingApprovalRepository.deleteAll();
        // Get Loan Accounts expecting repayment from KIVA
        KivaLoanAwaitingRepaymentData intialKivaLoanRequest = getLoanAccountsReadyForRepayments(accessToken, 1, 0, LOAN_STATUS);
        log.info("Kiva Loan Waiting Repayment" + intialKivaLoanRequest.toString());
        Integer totalPages = getKivaLoanPageSize(intialKivaLoanRequest.getTotal_records());
        log.info("Kiva Loan Total Records  " + intialKivaLoanRequest.getTotal_records());
        log.info("Kiva Loan Pages  " + totalPages);
        if (totalPages > 0) {
            for (int page = 1; page <= totalPages; page++) {
                int offset = (page - 1) * INITIAL_LOAN_LIMIT;
                KivaLoanAwaitingRepaymentData kivaLoanAwaitingRepaymentData = getLoanAccountsReadyForRepayments(accessToken, 100, offset,
                        LOAN_STATUS);
                log.info("Kiva Loan Waiting Repayment-- Page - - " + page + " - Offset - " + offset + "  ** "
                        + kivaLoanAwaitingRepaymentData.toString());

                saveKivaLoanAccountsAwaitingForRepayment(kivaLoanAwaitingRepaymentData);
            }
            // Now send repayments to Kiva
            KivaLoanRepaymentData kivaLoanRepaymentData = new KivaLoanRepaymentData();
            final Collection<KivaLoanAwaitingApprovalData> loanAwaitingApprovalData = this.kivaLoanAwaitingApprovalReadPlatformService
                    .retrieveAllKivaLoanAwaitingApproval();
            kivaLoanRepaymentData.setRepayments(loanAwaitingApprovalData);
            LOG.info(kivaLoanRepaymentData.toString());
            Gson gson = new GsonBuilder().create();

            String repayment = gson.toJson(kivaLoanRepaymentData);
            LOG.info("Repayment Object to Kiva :=> " + repayment);
            postLoanRepaymentToKiva(accessToken, repayment);

        }

    }

    private void saveKivaLoanAccountsAwaitingForRepayment(KivaLoanAwaitingRepaymentData kivaLoanAwaitingRepaymentData) {
        if (!CollectionUtils.isEmpty(kivaLoanAwaitingRepaymentData.getData())) {

            for (KivaLoanData kivaLoanData : kivaLoanAwaitingRepaymentData.getData()) {
                KivaLoanAwaitingApproval kivaLoanAwaitingApproval = new KivaLoanAwaitingApproval(kivaLoanData);
                kivaLoanAwaitingApprovalRepository.saveAndFlush(kivaLoanAwaitingApproval);
            }
        }
    }

    private String loanPayloadToKivaMapper(List<KivaLoanAccountSchedule> kivaLoanAccountSchedules, List<KivaLoanAccount> kivaLoanAccounts,
            List<Boolean> notPictured, Loan loan) {

        Client client = loan.getClient();
        String gender = (client.gender() != null) ? client.gender().label().toLowerCase() : "unknown";
        String loanPurpose = (loan.getLoanPurpose() != null) ? loan.getLoanPurpose().label() : "Not Defined";
        String clientKivaId = client.getKivaId();
        String base64Image = generateBase64Image(loan);

        if (base64Image == null) {
            throw new LoanDueDiligenceException("validation.msg.loan.profile.image.not.uploaded", "Loan profile image not uploaded");
        }

        ClientRecruitmentSurvey clientRecruitmentSurvey = clientRecruitmentSurveyRepository.getByClientId(client.getId());
        String location = getClientLocation(clientRecruitmentSurvey);

        KivaLoanAccount loanAccount = new KivaLoanAccount(loan.getLoanSummary().getTotalPrincipalDisbursed(), clientKivaId,
                client.getFirstname(), gender, client.getLastname(), getLoanKivaId(loan));
        kivaLoanAccounts.add(loanAccount);

        for (LoanRepaymentScheduleInstallment scheduleInstallment : loan.getRepaymentScheduleInstallments()) {
            KivaLoanAccountSchedule schedule = new KivaLoanAccountSchedule(Date.valueOf(scheduleInstallment.getDueDate()),
                    scheduleInstallment.getInterestOutstanding(loan.getCurrency()).getAmount(),
                    scheduleInstallment.getPrincipal(loan.getCurrency()).getAmount());
            kivaLoanAccountSchedules.add(schedule);
        }
        Date firstRepaymentDate = loan.getExpectedFirstRepaymentOnDate() != null ? Date.valueOf(loan.getExpectedFirstRepaymentOnDate())
                : null;
        KivaLoanAccountScheduleParameters scheduleParameters = new KivaLoanAccountScheduleParameters(firstRepaymentDate,
                loan.getNumberOfRepayments(), loan.getLoanRepaymentScheduleDetail().getRepayEvery(),
                LoanEnumerations.repaymentFrequencyType(loan.getLoanRepaymentScheduleDetail().getRepaymentPeriodFrequencyType().getValue())
                        .getValue().toLowerCase(),
                LoanEnumerations.interestType(loan.getLoanProductRelatedDetail().getInterestMethod().getValue()).getValue(),
                loan.getLoanProductRelatedDetail().getAnnualNominalInterestRate().toString());

        Long activityId = getActivityId(client);
        // build final object
        LoanDetailToKivaData loanDetailToKivaData = new LoanDetailToKivaData(activityId, Boolean.TRUE, loan.getCurrencyCode(),
                loan.getDescription(), DESCRIPTION_LANGUAGE_ID, Date.valueOf(loan.getDisbursementDate()), " ", base64Image,
                client.getId().toString(), generateInternalLoanId(loan.getDisbursementDate(), loan.getId()), loanPurpose, location,
                getKivaLoanDepartmentThemeType(loan), kivaLoanAccounts, kivaLoanAccountSchedules, notPictured, scheduleParameters);

        Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new KivaDateSerializerApi()).create();

        return gson.toJson(loanDetailToKivaData);
    }

    private String getClientLocation(ClientRecruitmentSurvey clientRecruitmentSurvey) {
        String clientLocation = (clientRecruitmentSurvey != null) ? clientRecruitmentSurvey.getSurveyLocation().label() : "Not Defined";
        String clientCountry = (clientRecruitmentSurvey != null) ? clientRecruitmentSurvey.getCountry().label() : "Not Defined";
        return clientLocation + ": " + clientCountry;
    }

    private String generateBase64Image(Loan loan) {
        final DocumentData documentData = this.documentReadPlatformService.retrieveKivaLoanProfileImage("loans", loan.getId());
        if (documentData != null && documentData.fileLocation() != null && documentData.storageType().equals(StorageType.FILE_SYSTEM)) {
            String formatIdentifier = null;

            if ("image/png".equalsIgnoreCase(documentData.contentType())) {
                formatIdentifier = "data:image/png;base64,";
            } else if ("image/jpg".equalsIgnoreCase(documentData.contentType())
                    || "image/jpeg".equalsIgnoreCase(documentData.contentType())) {
                formatIdentifier = "data:image/jpeg;base64,";
            } else if ("image/gif".equalsIgnoreCase(documentData.contentType())) {
                formatIdentifier = "data:image/gif;base64,";
            } else {
                throw new GeneralPlatformDomainRuleException("error.msg.unsupported.image.type.to.kiva",
                        "Only PNG,GIF,JPEG,JPG are accepted but this " + documentData.contentType() + " was found");
            }

            return formatIdentifier + getImageAsBase64(documentData.fileLocation());
        }
        return null;
    }

    @NotNull
    private Integer getKivaLoanDepartmentThemeType(Loan loan) {

        Optional<KivaTheme> kivaTheme = kivaThemeRepository.findByName(loan.getDepartment().label());
        if (kivaTheme.isPresent()) {
            return kivaTheme.get().getThemeId().intValue();
        } else {
            throw new LoanDueDiligenceException("validation.msg.loan.department.not.supported.by.kiva", KIVA_DEPARTMENT_NOT_FOUND);
        }
    }

    private Long getActivityId(Client client){

        final String datatableName = "Kiva Business Details";
        final Long clientId = client.getId();

        GenericResultsetData data = this.readWriteNonCoreDataService.retrieveDataTableGenericResultSet(
                datatableName, clientId, null, null, null, null);

        if (data.getData().isEmpty()){
            throw new LoanDueDiligenceException("validation.msg.loan.sector.activity.missing", "Client is missing Kiva business details");
        }

        ResultsetRowData kivaResultsRowData = data.getData().get(0);
        Long sectorId = Long.valueOf(kivaResultsRowData.getRow().get(1));
        Long activityId = Long.valueOf(kivaResultsRowData.getRow().get(2));

        CodeValueData sectorCodeValue = this.codeValueReadPlatformService.retrieveCodeValue(sectorId);
        CodeValueData activityCoderValue = this.codeValueReadPlatformService.retrieveCodeValue(activityId);

        Optional<KivaSectorActivity> kivaSectorActivity =  this.kivaSectorActivityRepository.findBySectorAndActivity(sectorCodeValue.getName(),activityCoderValue.getName());

        if (kivaSectorActivity.isPresent()) {
            return kivaSectorActivity.get().getActivityId();
        } else {
            throw new LoanDueDiligenceException("validation.msg.loan.sector.activity.not.supported.by.kiva", KIVA_SECTOR_ACTIVITY_MISMATCH);
        }
    }

    private String getLoanKivaId(Loan loan) {
        return loan.getKivaId();
    }

    private String authenticateToKiva() {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("fineract.integrations.kiva.oAuthUrl")).newBuilder();
        String url = urlBuilder.build().toString();

        OkHttpClient client = new OkHttpClient();
        Response response = null;

        RequestBody formBody = new FormBody.Builder().add("grant_type", getConfigProperty("fineract.integrations.kiva.grantType"))
                .add("scope", getConfigProperty("fineract.integrations.kiva.scope"))
                .add("audience", getConfigProperty("fineract.integrations.kiva.audience"))
                .add("client_id", getConfigProperty("fineract.integrations.kiva.clientId"))
                .add("client_secret", getConfigProperty("fineract.integrations.kiva.clientSecret")).build();

        Request request = new Request.Builder().url(url).header(FORM_URL_CONTENT_TYPE, FORM_URL_ENCODED).post(formBody).build();

        List<Throwable> exceptions = new ArrayList<>();

        try {
            response = client.newCall(request).execute();
            String resObject = response.body().string();
            if (response.isSuccessful()) {

                JsonObject jsonResponse = JsonParser.parseString(resObject).getAsJsonObject();
                String accessToken = jsonResponse.get("access_token").getAsString();

                log.info("Login to KIVA is Successful");

                return accessToken;
            } else {
                log.error("Login to KIVA failed with Message:" + resObject);

                handleAPIIntegrityIssues(resObject);

            }
        } catch (Exception e) {
            log.error("Authentication to KIVA has failed" + e);
            exceptions.add(e);
        }
        if (!CollectionUtils.isEmpty(exceptions)) {
            try {
                throw new JobExecutionException(exceptions);
            } catch (JobExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private void handleAPIIntegrityIssues(String httpResponse) {
        throw new PlatformDataIntegrityException(httpResponse, httpResponse);
    }

    private String getConfigProperty(String propertyName) {
        return this.env.getProperty(propertyName);
    }

    private String generateInternalLoanId(LocalDate disbursementDate, Long loanId) {
        Integer year = disbursementDate.getYear();
        return "LOAN/" + loanId + "/" + year;
    }

    private String postLoanToKiva(String accessToken, String loanToKiva) {
        HttpUrl urlBuilder = new HttpUrl.Builder().scheme(getConfigProperty("fineract.integrations.kiva.httpType"))
                .host(getConfigProperty("fineract.integrations.kiva.baseUrl"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.apiVersion"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.partnerCode"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.partnerId")).addPathSegment("loan_draft").build();

        String url = urlBuilder.toString();

        OkHttpClient client = new OkHttpClient();
        Response response = null;

        RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), loanToKiva);

        Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + accessToken).post(formBody).build();

        List<Throwable> exceptions = new ArrayList<>();

        try {
            response = client.newCall(request).execute();
            String resObject = response.body().string();
            if (response.isSuccessful()) {

                JsonObject jsonResponse = JsonParser.parseString(resObject).getAsJsonObject();
                log.info("Loan Account Response from KIVA :=>" + resObject);

                return jsonResponse.get("loan_draft_uuid").getAsString();

            } else {
                log.error("Post Loan to KIVA failed with Message:" + resObject);

                handleAPIIntegrityIssues(resObject);

            }
        } catch (Exception e) {
            log.error("Post Loan to KIVA has failed" + e);
            exceptions.add(e);
        }
        if (!CollectionUtils.isEmpty(exceptions)) {
            try {
                throw new JobExecutionException(exceptions);
            } catch (JobExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public String getImageAsBase64(String imageName) {
        File imageFile = new File(imageName);
        byte[] imageBytes;
        try {
            imageBytes = FileCopyUtils.copyToByteArray(imageFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private KivaLoanAwaitingRepaymentData getLoanAccountsReadyForRepayments(String accessToken, Integer limit, Integer offset,
            String status) {

        HttpUrl.Builder builder = new HttpUrl.Builder().scheme(getConfigProperty("fineract.integrations.kiva.httpType"))
                .host(getConfigProperty("fineract.integrations.kiva.baseUrl"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.apiVersion"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.partnerCode"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.partnerId")).addPathSegment("loans")
                .addQueryParameter("limit", String.valueOf(limit)).addQueryParameter("offset", String.valueOf(offset))
                .addQueryParameter("status", status);

        String url = builder.build().toString();

        OkHttpClient client = new OkHttpClient();
        List<Throwable> exceptions = new ArrayList<>();
        Response response = null;

        Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + accessToken).get().build();

        try {
            response = client.newCall(request).execute();
            String resObject = response.body().string();
            if (response.isSuccessful()) {
                return fromJson(JsonParser.parseString(resObject).getAsJsonObject());
            } else {
                log.error("Get Loans expecting repayment from KIVA failed with Message:" + resObject);

                handleAPIIntegrityIssues(resObject);

            }
        } catch (Exception e) {
            log.error("Get Loans expecting repayment from KIVA failed" + e);
            exceptions.add(e);
        }
        if (!CollectionUtils.isEmpty(exceptions)) {
            try {
                throw new JobExecutionException(exceptions);
            } catch (JobExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public KivaLoanAwaitingRepaymentData fromJson(JsonObject jsonResponse) {

        KivaLoanAwaitingRepaymentData loanResponse = new KivaLoanAwaitingRepaymentData();

        loanResponse.setTotal_records(jsonResponse.get("total_records").getAsInt());

        JsonArray dataArray = jsonResponse.getAsJsonArray("data");
        List<KivaLoanData> loanDataList = new ArrayList<>();
        for (int i = 0; i < dataArray.size(); i++) {
            JsonObject loanDataObject = dataArray.get(i).getAsJsonObject();

            KivaLoanData loanData = new KivaLoanData();

            loanData.setBorrower_count(loanDataObject.get("borrower_count").getAsInt());
            loanData.setInternal_loan_id(loanDataObject.get("internal_loan_id").getAsString());
            loanData.setInternal_client_id(loanDataObject.get("internal_client_id").getAsString());
            loanData.setPartner_id(loanDataObject.get("partner_id").getAsString());
            loanData.setPartner(loanDataObject.get("partner").getAsString());
            loanData.setKiva_id(loanDataObject.get("kiva_id").getAsString());
            loanData.setUuid(loanDataObject.get("uuid").getAsString());
            loanData.setName(loanDataObject.get("name").getAsString());
            loanData.setLocation(loanDataObject.get("location").getAsString());
            loanData.setStatus(loanDataObject.get("status").getAsString());
            loanData.setLoan_price(loanDataObject.get("loan_price").getAsString());
            loanData.setLoan_local_price(loanDataObject.get("loan_local_price").getAsString());
            loanData.setLoan_currency(loanDataObject.get("loan_currency").getAsString());
            loanData.setDelinquent(loanDataObject.get("delinquent").getAsBoolean());
            loanData.setFundedAmount(loanDataObject.get("fundedAmount").getAsBigDecimal());

            loanDataList.add(loanData);
        }
        loanResponse.setData(loanDataList);

        return loanResponse;
    }

    private Integer getKivaLoanPageSize(Integer totalRecords) {

        Integer totalPages = totalRecords / INITIAL_LOAN_LIMIT;
        Integer remainder = totalRecords % INITIAL_LOAN_LIMIT;

        if (remainder > 0) {
            totalPages++;
        }
        return totalPages;
    }

    private void postLoanRepaymentToKiva(String accessToken, String repayments) {
        HttpUrl urlBuilder = new HttpUrl.Builder().scheme(getConfigProperty("fineract.integrations.kiva.httpType"))
                .host(getConfigProperty("fineract.integrations.kiva.baseUrl"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.apiVersion"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.partnerCode"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.partnerId")).addPathSegment("repayments").build();

        String url = urlBuilder.toString();
        LOG.info("Post Loan Repayments to KIVA URL :=>" + url);
        OkHttpClient client = new OkHttpClient();
        Response response = null;

        RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), repayments);

        Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + accessToken).post(formBody).build();

        List<Throwable> exceptions = new ArrayList<>();

        try {
            response = client.newCall(request).execute();
            String resObject = response.body().string();
            if (response.isSuccessful()) {

                JsonObject jsonResponse = JsonParser.parseString(resObject).getAsJsonObject();
                log.info("Loan Account Repayment Response from KIVA :=>" + resObject);

            } else {
                log.error("Post Loan Repayments to KIVA failed with Message:" + resObject);

                handleAPIIntegrityIssues(resObject);

            }
        } catch (Exception e) {
            log.error("Post Loan Repayments to KIVA has failed" + e);
            exceptions.add(e);
        }
        if (!CollectionUtils.isEmpty(exceptions)) {
            try {
                throw new JobExecutionException(exceptions);
            } catch (JobExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean validateLoanKivaDetails(Loan loan) {
        if (loan.getLoanPurpose() == null) {
            throw new LoanDueDiligenceException("validation.msg.loan.loanPurposeId.cannot.be.blank", "Loan purpose required.");
        }
        if (loan.getDescription() == null) {
            throw new LoanDueDiligenceException("validation.msg.loan.description.cannot.be.blank", "Loan description required.");
        }

        ClientRecruitmentSurvey clientRecruitmentSurvey = clientRecruitmentSurveyRepository.getByClientId(loan.getClientId());
        if (clientRecruitmentSurvey == null) {
            throw new LoanDueDiligenceException("validation.msg.client.recruitment.survey.required", "Client recruitment survey required");
        }
        String clientLocation = clientRecruitmentSurvey.getSurveyLocation().label();
        String clientCountry = clientRecruitmentSurvey.getCountry().label();

        if (clientCountry == null) {
            throw new LoanDueDiligenceException("validation.msg.client.recruitment.survey.country.required",
                    "Client recruitment survey country required");
        }
        if (clientLocation == null) {
            throw new LoanDueDiligenceException("validation.msg.client.recruitment.survey.location.required",
                    "Client recruitment survey location required");
        }

        List<KivaLocation> locations = this.kivaLocationRepository.findAll();
        KivaLocation kivaLocation = locations.stream().filter(location -> clientLocation.equalsIgnoreCase(location.getLocation())
                && clientCountry.equalsIgnoreCase(location.getCountry())).findAny().orElse(null);
        if (kivaLocation == null) {
            throw new LoanDueDiligenceException("validation.msg.client.survey.location.not.supported.by.kiva",
                    "Client Survey location/country not supported by kiva");
        }

        List<KivaCurrency> currencies = this.kivaCurrencyRepository.findAll();
        KivaCurrency kivaCurrency = currencies.stream()
                .filter(currency -> loan.getCurrency().getCode().equalsIgnoreCase(currency.getName())).findAny().orElse(null);
        if (kivaCurrency == null) {
            throw new LoanDueDiligenceException("validation.msg.loan.currency.not.supported.by.kiva",
                    "Loan Currency not supported by kiva");
        }

        List<KivaTheme> themes = this.kivaThemeRepository.findAll();
        KivaTheme kivaTheme = themes.stream().filter(theme -> loan.getDepartment().label().equalsIgnoreCase(theme.getName())).findAny()
                .orElse(null);
        if (kivaTheme == null) {
            throw new LoanDueDiligenceException("validation.msg.loan.department.not.supported.by.kiva", KIVA_DEPARTMENT_NOT_FOUND);
        }
        try {
            final DocumentData documentData = this.documentReadPlatformService.retrieveKivaLoanProfileImage("loans", loan.getId());
        } catch (EmptyResultDataAccessException e) {
            throw new LoanDueDiligenceException("validation.msg.loan.profile.image.not.uploaded", "Loan profile image not uploaded");
        }

        getActivityId(loan.client());

        return true;
    }

    @Override
    @CronTarget(jobName = JobName.DOWNLOAD_KIVA_DEPENDENCIES_META_DATA)
    public void updateKivaDependenciesMetaData() {
        LOG.info("starting download kiva metadata job");
        String accessToken = authenticateToKiva();
        updateKivaSupportedCurrencies(accessToken);
        updateKivaSupportedThemes(accessToken);
        updateKivaSupportedLocations(accessToken);
        LOG.info("end download kiva metadata job");

    }

    private void updateKivaSupportedCurrencies(String accessToken) {

        HttpUrl.Builder builder = new HttpUrl.Builder().scheme(getConfigProperty("fineract.integrations.kiva.httpType"))
                .host(getConfigProperty("fineract.integrations.kiva.baseUrl"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.apiVersion"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.partnerCode"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.partnerId")).addPathSegment("config")
                .addPathSegment("locales");

        String url = builder.build().toString();

        OkHttpClient client = new OkHttpClient();
        Response response = null;
        List<Throwable> exceptions = new ArrayList<>();
        Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + accessToken).get().build();

        try {
            response = client.newCall(request).execute();
            String resObject = response.body().string();
            if (response.isSuccessful()) {
                ObjectMapper objectMapper = new ObjectMapper();
                KivaSupportedCurrencyData data = objectMapper.readValue(resObject, KivaSupportedCurrencyData.class);
                List<KivaCurrency> currencies = new ArrayList<KivaCurrency>();
                for (KivaLocaleData locale : data.getLocales()) {
                    KivaCurrency currency = new KivaCurrency();
                    currency.setName(locale.getCurrency());
                    currency.setLanguage(locale.getLanguageCode());
                    currencies.add(currency);
                }

                this.kivaCurrencyRepository.truncateTable();
                this.kivaCurrencyRepository.saveAllAndFlush(currencies);
            } else {
                log.error("Get supported currencies from KIVA failed with Message:" + resObject);

                handleAPIIntegrityIssues(resObject);

            }
        } catch (Exception e) {
            log.error("Get supported currencies from KIVA failed" + e);
            exceptions.add(e);
        }
        if (!CollectionUtils.isEmpty(exceptions)) {
            try {
                throw new JobExecutionException(exceptions);
            } catch (JobExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void updateKivaSupportedThemes(String accessToken) {

        HttpUrl.Builder builder = new HttpUrl.Builder().scheme(getConfigProperty("fineract.integrations.kiva.httpType"))
                .host(getConfigProperty("fineract.integrations.kiva.baseUrl"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.apiVersion"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.partnerCode"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.partnerId")).addPathSegment("config")
                .addPathSegment("themes");

        String url = builder.build().toString();

        OkHttpClient client = new OkHttpClient();
        Response response = null;

        List<Throwable> exceptions = new ArrayList<>();
        Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + accessToken).get().build();

        try {
            response = client.newCall(request).execute();
            String resObject = response.body().string();
            if (response.isSuccessful()) {
                ObjectMapper objectMapper = new ObjectMapper();
                KivaSupportedThemeData data = objectMapper.readValue(resObject, KivaSupportedThemeData.class);
                List<KivaTheme> themes = new ArrayList<KivaTheme>();
                for (ThemeData theme : data.getThemes()) {
                    KivaTheme kivaTheme = new KivaTheme();
                    kivaTheme.setThemeId(theme.getThemeTypeId());
                    kivaTheme.setName(theme.getThemeType());
                    themes.add(kivaTheme);
                }

                this.kivaThemeRepository.truncateTable();
                this.kivaThemeRepository.saveAllAndFlush(themes);
            } else {
                log.error("Get supported themese from KIVA failed with Message:" + resObject);

                handleAPIIntegrityIssues(resObject);

            }
        } catch (Exception e) {
            log.error("Get supported themes from KIVA failed" + e);
            exceptions.add(e);
        }
        if (!CollectionUtils.isEmpty(exceptions)) {
            try {
                throw new JobExecutionException(exceptions);
            } catch (JobExecutionException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void updateKivaSupportedLocations(String accessToken) {

        HttpUrl.Builder builder = new HttpUrl.Builder().scheme(getConfigProperty("fineract.integrations.kiva.httpType"))
                .host(getConfigProperty("fineract.integrations.kiva.baseUrl"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.apiVersion"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.partnerCode"))
                .addPathSegment(getConfigProperty("fineract.integrations.kiva.partnerId")).addPathSegment("config")
                .addPathSegment("locations");

        String url = builder.build().toString();

        OkHttpClient client = new OkHttpClient();
        Response response = null;

        List<Throwable> exceptions = new ArrayList<>();
        Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + accessToken).get().build();

        try {
            response = client.newCall(request).execute();
            String resObject = response.body().string();
            if (response.isSuccessful()) {
                ObjectMapper objectMapper = new ObjectMapper();
                KivaSupportedLocationData data = objectMapper.readValue(resObject, KivaSupportedLocationData.class);
                List<KivaLocation> locations = new ArrayList<KivaLocation>();
                for (KivaLocationData location : data.getLocations()) {
                    KivaLocation kivaLocation = new KivaLocation();
                    kivaLocation.setCountry(location.getCountry());
                    kivaLocation.setLocation(location.getLocation());
                    kivaLocation.setFullName(location.getFullName());
                    locations.add(kivaLocation);
                }

                this.kivaLocationRepository.truncateTable();
                this.kivaLocationRepository.saveAllAndFlush(locations);
            } else {
                log.error("Get supported locations from KIVA failed with Message:" + resObject);

                handleAPIIntegrityIssues(resObject);

            }
        } catch (Exception e) {
            log.error("Get supported locations from KIVA failed" + e);
            exceptions.add(e);
        }
        if (!CollectionUtils.isEmpty(exceptions)) {
            try {
                throw new JobExecutionException(exceptions);
            } catch (JobExecutionException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
