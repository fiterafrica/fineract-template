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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections.CollectionUtils;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.data.RwandaConsumerCreditData;
import org.apache.fineract.portfolio.loanaccount.data.RwandaCorporateCreditData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionAuthenticationData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaConsumerCreditData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaCorporateCreditData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbConsumerLogger;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbConsumerLoggerRepository;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbCorporateLogger;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbCorporateLoggerRepository;
import org.apache.fineract.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransUnionCrbServiceImpl implements TransUnionCrbService {

    private static final Logger LOG = LoggerFactory.getLogger(TransUnionCrbServiceImpl.class);
    public static final String FORM_URL_CONTENT_TYPE = "application/json";
    private final TransUnionCrbPostConsumerCreditReadPlatformServiceImpl transUnionCrbPostConsumerCreditReadPlatformServiceImpl;
    private final TransUnionCrbPostCorporateCreditReadPlatformServiceImpl transUnionCrbPostCorporateCreditReadPlatformServiceImpl;
    private final LoanRepositoryWrapper loanRepository;
    private final TransunionCrbConsumerLoggerRepository crbConsumerLoggerRepository;
    private final TransunionCrbCorporateLoggerRepository crbCorporateLoggerRepository;
    private final PlatformSecurityContext context;
    @Autowired
    private Environment env;

    @Override
    @CronTarget(jobName = JobName.POST_RWANDA_CONSUMER_CREDIT_TO_TRANSUNION_CRB)
    public void ConsumerCreditDataUploadToTransUnion() {
        LOG.info("Starting Consumer Credit Data Upload To TransUnion CRB");
        final AppUser currentUser = this.context.authenticatedUser();
        String token = authenticateToTransUnionRestApi();
        LOG.info("CRB Token == > " + token);
        List<Integer> loansNotToBeRePostedTransUnion = new ArrayList<>();
        Collection<TransUnionRwandaConsumerCreditData> transUnionRwandaConsumerCreditDataCollection = transUnionCrbPostConsumerCreditReadPlatformServiceImpl
                .retrieveAllConsumerCredits();

        LOG.info(" >>>> Size for Consumer credit - - >" + transUnionRwandaConsumerCreditDataCollection.size());
        List<Throwable> exceptions = new ArrayList<>();

        String batchId = UUID.randomUUID().toString();

        if (!CollectionUtils.isEmpty(transUnionRwandaConsumerCreditDataCollection)) {
            for (TransUnionRwandaConsumerCreditData creditData : transUnionRwandaConsumerCreditDataCollection) {

                RwandaConsumerCreditData rwandaConsumerCreditData = new RwandaConsumerCreditData();
                rwandaConsumerCreditData.setConsumerCreditInformationRecord(creditData);
                rwandaConsumerCreditData.setRecordType("IC");
                String callbackId = null;
                try {
                    callbackId = postRwandaConsumerCreditToTransUnion(token, convertConsumerCreditPayloadToJson(rwandaConsumerCreditData));

                    if (callbackId != null && !creditData.getLoanStatus().equals(LoanStatus.ACTIVE.getValue())) {
                        // add it to list to update flag on the loan account so that next time we don't post it to
                        // TransUnion
                        // We query by status 300, 600, 601, 700 so if loan account is not Activate , then after this
                        // upload, stop re-posting
                        loansNotToBeRePostedTransUnion.add(creditData.getLoanId());
                    }
                    // Add Logger
                    saveConsumerCrbLogger(creditData.getLoanId(), batchId, callbackId, Boolean.TRUE, null,
                            convertConsumerCreditPayloadToJson(rwandaConsumerCreditData), currentUser);
                } catch (Exception e) {
                    log.error("Post Consumer Credit to TransUnion has failed" + e);
                    exceptions.add(e);
                    // Add Logger
                    saveConsumerCrbLogger(creditData.getLoanId(), batchId, null, Boolean.FALSE, e.getMessage(),
                            convertConsumerCreditPayloadToJson(rwandaConsumerCreditData), currentUser);
                }

            }
        }

        // Update flags
        if (!CollectionUtils.isEmpty(loansNotToBeRePostedTransUnion)) {
            for (Integer loanId : loansNotToBeRePostedTransUnion) {

                Loan loan = loanRepository.findOneWithNotFoundDetection(loanId.longValue());

                loan.setStopConsumerCreditUploadToTransUnion(Boolean.TRUE);
                loan.setStopConsumerCreditUploadToTransUnionOn(DateUtils.getBusinessLocalDate());

                loanRepository.saveAndFlush(loan);
            }
        }

        if (!CollectionUtils.isEmpty(exceptions)) {
            try {
                throw new JobExecutionException(exceptions);
            } catch (JobExecutionException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void saveConsumerCrbLogger(Integer loanId, String batchId, String callbackId, Boolean hasPassed, String errorLogs,
            String payload, AppUser currentUser) {
        TransunionCrbConsumerLogger logger = new TransunionCrbConsumerLogger(batchId, hasPassed, loanId, callbackId, errorLogs, payload);
        assert currentUser.getId() != null;
        logger.setCreatedBy(currentUser.getId());
        logger.setLastModifiedBy(currentUser.getId());
        crbConsumerLoggerRepository.saveAndFlush(logger);
    }

    private void saveCorporateCrbLogger(Integer loanId, String batchId, String callbackId, Boolean hasPassed, String errorLogs,
            String payload, AppUser currentUser) {
        TransunionCrbCorporateLogger logger = new TransunionCrbCorporateLogger(batchId, hasPassed, loanId, callbackId, errorLogs, payload);
        assert currentUser.getId() != null;
        logger.setCreatedBy(currentUser.getId());
        logger.setLastModifiedBy(currentUser.getId());
        crbCorporateLoggerRepository.saveAndFlush(logger);
    }

    @Override
    @CronTarget(jobName = JobName.POST_RWANDA_CORPORATE_CREDIT_TO_TRANSUNION_CRB)
    public void CorporateCreditDataUploadToTransUnion() {
        LOG.info("Starting Corporate Credit Data Upload To TransUnion CRB");
        final AppUser currentUser = this.context.authenticatedUser();
        String token = authenticateToTransUnionRestApi();
        LOG.info("CRB Token == > " + token);
        List<Integer> loansNotToBeRePostedTransUnion = new ArrayList<>();
        Collection<TransUnionRwandaCorporateCreditData> transUnionRwandaCorporateCreditDataCollection = transUnionCrbPostCorporateCreditReadPlatformServiceImpl
                .retrieveAllCorporateCredits();

        LOG.info(" >>>> Size for Corporate credit - - >" + transUnionRwandaCorporateCreditDataCollection.size());
        List<Throwable> exceptions = new ArrayList<>();
        String batchId = UUID.randomUUID().toString();
        if (!CollectionUtils.isEmpty(transUnionRwandaCorporateCreditDataCollection)) {
            for (TransUnionRwandaCorporateCreditData creditData : transUnionRwandaCorporateCreditDataCollection) {
                RwandaCorporateCreditData rwandaCorporateCreditData = new RwandaCorporateCreditData();
                rwandaCorporateCreditData.setCorporateCreditInformationRecord(creditData);
                rwandaCorporateCreditData.setRecordType("CI");
                String callbackId = null;

                try {
                    callbackId = postRwandaCorporateCreditToTransUnion(token,
                            convertConsumerCreditPayloadToJson(rwandaCorporateCreditData));

                    if (callbackId != null && !creditData.getLoanStatus().equals(LoanStatus.ACTIVE.getValue())) {
                        // add it to list to update flag on the loan account so that next time we don't post it to
                        // TransUnion
                        // We query by status 300, 600, 601, 700 so if loan account is not Activate , then after this
                        // upload, stop re-posting
                        loansNotToBeRePostedTransUnion.add(creditData.getLoanId());
                    }
                    // Add Logger
                    saveCorporateCrbLogger(creditData.getLoanId(), batchId, callbackId, Boolean.TRUE, null,
                            convertConsumerCreditPayloadToJson(rwandaCorporateCreditData), currentUser);
                } catch (Exception e) {
                    log.error("Post Corporate Credit to TransUnion has failed" + e);
                    exceptions.add(e);
                    // Add Logger
                    saveCorporateCrbLogger(creditData.getLoanId(), batchId, null, Boolean.FALSE, e.getMessage(),
                            convertConsumerCreditPayloadToJson(rwandaCorporateCreditData), currentUser);
                }

            }
        }

        // Update flags
        if (!CollectionUtils.isEmpty(loansNotToBeRePostedTransUnion)) {
            for (Integer loanId : loansNotToBeRePostedTransUnion) {

                Loan loan = loanRepository.findOneWithNotFoundDetection(loanId.longValue());

                loan.setStopConsumerCreditUploadToTransUnion(Boolean.TRUE);
                loan.setStopConsumerCreditUploadToTransUnionOn(DateUtils.getBusinessLocalDate());

                loanRepository.saveAndFlush(loan);
            }
        }
        if (!CollectionUtils.isEmpty(exceptions)) {
            try {
                throw new JobExecutionException(exceptions);
            } catch (JobExecutionException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private String convertConsumerCreditPayloadToJson(RwandaConsumerCreditData rwandaConsumerCreditData) {
        Gson gson = new GsonBuilder().create();
        String request = gson.toJson(rwandaConsumerCreditData);
        LOG.info("Actual Payload to be sent - - >" + request);
        return request;
    }

    private String convertConsumerCreditPayloadToJson(RwandaCorporateCreditData rwandaCorporateCreditData) {
        Gson gson = new GsonBuilder().create();
        String request = gson.toJson(rwandaCorporateCreditData);
        LOG.info("Corporate --> Actual Payload to be sent - - >" + request);
        return request;
    }

    private String authenticateToTransUnionRestApi() {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("fineract.integrations.transUnion.crb.rest.authenticationUrl"))
                .newBuilder();
        String url = urlBuilder.build().toString();

        TransUnionAuthenticationData transUnionAuthenticationData = new TransUnionAuthenticationData(
                getConfigProperty("fineract.integrations.transUnion.crb.rest.username"),
                getConfigProperty("fineract.integrations.transUnion.crb.rest.password"),
                getConfigProperty("fineract.integrations.transUnion.crb.rest.infinityCode"));
        Gson gson = new GsonBuilder().create();
        RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), gson.toJson(transUnionAuthenticationData));

        OkHttpClient client = new OkHttpClient();
        Response response = null;
        Request request = new Request.Builder().url(url).post(formBody).build();

        List<Throwable> exceptions = new ArrayList<>();

        try {
            response = client.newCall(request).execute();
            String resObject = response.body().string();
            if (response.isSuccessful()) {

                JsonObject jsonResponse = JsonParser.parseString(resObject).getAsJsonObject();
                String accessToken = jsonResponse.get("token").getAsString();

                log.info("Login to CRB TransUnion is Successful");

                return accessToken;
            } else {
                log.error("Login to CRB TransUnion failed with Message:" + resObject);

                handleAPIIntegrityIssues(resObject);

            }
        } catch (Exception e) {
            log.error("Authentication to CRB TransUnion has failed" + e);
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

    private String postRwandaConsumerCreditToTransUnion(String accessToken, String consumerCreditData) throws IOException {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("fineract.integrations.transUnion.crb.rest.postConsumerCredit"))
                .newBuilder();
        String url = urlBuilder.build().toString();

        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), consumerCreditData);

        Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json ").post(formBody).build();
        List<Throwable> exceptions = new ArrayList<>();

        try {
            Response response = client.newCall(request).execute();
            String resObject = response.body().string();
            if (response.isSuccessful()) {

                JsonObject jsonResponse = JsonParser.parseString(resObject).getAsJsonObject();
                log.info("Consumer Credit Response from TransUnion :=>" + resObject);
                JsonElement responseCodeElement = jsonResponse.get("responseCode");
                if (responseCodeElement != null) {
                    Integer code = responseCodeElement.getAsInt();
                    if (code == 200) {
                        return jsonResponse.get("callbackId").getAsString();
                    } else {
                        handleAPIIntegrityIssues(resObject);
                    }
                } else {
                    log.error("Null Response Code  ::--> ");
                    handleAPIIntegrityIssues(resObject);
                }
            }
        } catch (Exception e) {
            log.error("Post Consumer Credit to TransUnion has failed" + e);
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

    private String postRwandaCorporateCreditToTransUnion(String accessToken, String corporateCreditData) throws IOException {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("fineract.integrations.transUnion.crb.rest.postCorporateCredit"))
                .newBuilder();
        String url = urlBuilder.build().toString();

        OkHttpClient client = new OkHttpClient();
        Response response = null;

        RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), corporateCreditData);

        Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json ").post(formBody).build();

        List<Throwable> exceptions = new ArrayList<>();

        try {
            response = client.newCall(request).execute();
            String resObject = response.body().string();
            if (response.isSuccessful()) {

                JsonObject jsonResponse = JsonParser.parseString(resObject).getAsJsonObject();
                log.info("Corporate Credit Response from TransUnion :=>" + resObject);
                JsonElement responseCodeElement = jsonResponse.get("responseCode");
                if (responseCodeElement != null) {
                    Integer code = responseCodeElement.getAsInt();
                if (code == 200) {
                    return jsonResponse.get("callbackId").getAsString();
                } else {
                    handleAPIIntegrityIssues(resObject);
                }
                }else{
                    handleAPIIntegrityIssues(resObject);
                }
                return null;
            } else {
                log.error("Post Corporate Credit to TransUnion failed with Message:" + resObject);

                handleAPIIntegrityIssues(resObject);

            }
        } catch (Exception e) {
            log.error("Post Corporate Credit to TransUnion has failed" + e);
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
}
