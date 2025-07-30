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
package org.apache.fineract.infrastructure.Odoo;

import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLSocketFactory;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections.CollectionUtils;
import org.apache.fineract.accounting.journalentry.domain.JournalEntry;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.accounting.journalentry.data.JournalData;
import org.apache.fineract.accounting.journalentry.data.JournalItemData;
import org.apache.fineract.infrastructure.Odoo.exception.OdooFailedException;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.domain.FineractContext;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.domain.FailedClientCreationOnDataMigration;
import org.apache.fineract.portfolio.client.domain.FailedClientCreationOnDataMigrationRepository;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionNotPostedToOdooInstanceData;
import org.apache.fineract.portfolio.loanaccount.domain.FailedLoanCreationOnDataMigration;
import org.apache.fineract.portfolio.loanaccount.domain.FailedLoanCreationOnDataMigrationRepository;
import org.apache.fineract.portfolio.loanaccount.domain.FailedLoanRepaymentOnDataMigration;
import org.apache.fineract.portfolio.loanaccount.domain.FailedLoanRepaymentOnDataMigrationRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@SuppressWarnings({ "unchecked", "rawtypes", "cast" })
public class OdooServiceImpl implements OdooService {

    private static final Logger LOG = LoggerFactory.getLogger(OdooServiceImpl.class);
    public static final String FORM_URL_CONTENT_TYPE = "Content-Type";

    @Value("${fineract.integrations.odoo.db}")
    private String odooDB;

    @Value("${fineract.integrations.odoo.username}")
    private String username;

    @Value("${fineract.integrations.odoo.password}")
    private String password;

    @Value("${fineract.integrations.odoo.url}")
    private String url;

    @Value("${fineract.integrations.celery.url}")
    private String celeryUrl;
    private ClientRepositoryWrapper clientRepository;
    private ConfigurationDomainService configurationDomainService;

    private final JournalEntryRepository journalEntryRepository;
    private final LoanReadPlatformService loanReadPlatformService;
    private ExecutorService genericExecutorService;
    private FailedClientCreationOnDataMigrationRepository failedClientCreationOnDataMigrationRepository;
    private FailedLoanCreationOnDataMigrationRepository failedLoanCreationOnDataMigrationRepository;
    private FailedLoanRepaymentOnDataMigrationRepository failedLoanRepaymentOnDataMigrationRepository;

    @Autowired
    public OdooServiceImpl(ClientRepositoryWrapper clientRepository, ConfigurationDomainService configurationDomainService,
            JournalEntryRepository journalEntryRepository, LoanReadPlatformService loanReadPlatformService,
            FailedClientCreationOnDataMigrationRepository failedClientCreationOnDataMigrationRepository,
            FailedLoanCreationOnDataMigrationRepository failedLoanCreationOnDataMigrationRepository,
            FailedLoanRepaymentOnDataMigrationRepository failedLoanRepaymentOnDataMigrationRepository) {
        this.clientRepository = clientRepository;
        this.configurationDomainService = configurationDomainService;
        this.journalEntryRepository = journalEntryRepository;
        this.loanReadPlatformService = loanReadPlatformService;
        this.failedClientCreationOnDataMigrationRepository = failedClientCreationOnDataMigrationRepository;
        this.failedLoanCreationOnDataMigrationRepository = failedLoanCreationOnDataMigrationRepository;
        this.failedLoanRepaymentOnDataMigrationRepository = failedLoanRepaymentOnDataMigrationRepository;
    }

    @PostConstruct
    public void initializeExecutorService() {
        genericExecutorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public Integer loginToOddo() {
        try {
            final XmlRpcClientConfigImpl commonConfig = new XmlRpcClientConfigImpl();
            final XmlRpcClient client = new XmlRpcClient();
            commonConfig.setServerURL(new URL(String.format("%s/xmlrpc/2/common", url)));

            Object uid = (Object) client.execute(commonConfig, "authenticate",
                    Arrays.asList(odooDB, username, password, Collections.emptyMap()));
            if (!uid.equals(false)) {
                LOG.info("Odoo Authentication successful uid" + uid);
                return (Integer) uid;
            } else {
                LOG.error("Odoo Authentication failed");
                return 0;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return 0;
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "cast" })
    @Override
    public Integer createCustomerToOddo(Client client) {
        try {
            final Integer uid = loginToOddo();
            if (uid > 0) {
                XmlRpcClient models = getCommonConfig();
                // Create client
                Integer partnerId = getPartner(client.getId(), uid, models);
                if (partnerId == null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", client.getDisplayName());
                    map.put("mobile", client.getMobileNo() != null ? client.getMobileNo() : false);
                    map.put("customer_rank", 1);
                    map.put("fineract_customer_id", client.getId().toString() != null ? client.getId().toString() : false);
                    map.put("is_company", LegalForm.fromInt(client.getLegalForm().intValue()).isEntity() ? true : false);

                    final Integer id = (Integer) models.execute("execute_kw",
                            Arrays.asList(odooDB, uid, password, "res.partner", "create", Arrays.asList(map)));
                    if (id != null) {
                        LOG.info("Odoo Client created with id " + id);
                    }
                    return id;
                }
            }
        } catch (XmlRpcException e) {
            throw new OdooFailedException(e);
        }
        return null;
    }

    private Integer getPartner(Long clientId, Integer uid, XmlRpcClient models) {

        try {
            if (uid > 0) {
                List partners;
                Map<String, Object> map = new HashMap<>();
                map.put("fields", Arrays.asList("id", "name", "email"));
                map.put("limit", 5);

                if (clientId != null) {
                    partners = Arrays.asList(
                            (Object[]) models.execute("execute_kw", Arrays.asList(odooDB, uid, password, "res.partner", "search_read",
                                    Arrays.asList(Arrays.asList(Arrays.asList("fineract_customer_id", "=", clientId.intValue()))), map)));
                    Integer partnerId = null;
                    if (partners != null && partners.size() > 0) {
                        HashMap partner = (HashMap) partners.get(0);
                        partnerId = (Integer) partner.get("id");
                    }
                    return partnerId;
                }
            }
        } catch (XmlRpcException e) {
            throw new OdooFailedException(e);
        }
        return null;
    }

    private XmlRpcClient getCommonConfig() {
        XmlRpcClient models;
        models = new XmlRpcClient() {

            {
                setConfig(new XmlRpcClientConfigImpl() {

                    {
                        try {
                            setServerURL(new URL(String.format("%s/xmlrpc/2/object", url)));
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        };
        return models;
    }

    @Override
    @CronTarget(jobName = JobName.POST_CUSTOMERS_TO_ODDO)
    public void postClientsToOddo() throws JobExecutionException {
        Boolean isOdooEnabled = this.configurationDomainService.isOdooIntegrationEnabled();
        if (isOdooEnabled) {
            List<Client> clients = this.clientRepository.getClientByIsOdooPosted(false);

            List<Throwable> errors = new ArrayList<>();

            if (clients != null && clients.size() > 0) {
                for (Client client : clients) {
                    try {
                        Integer id = createCustomerToOddo(client);
                        if (id != null) {
                            client.setOdooCustomerPosted(true);
                            client.setOdooCustomerId(id);
                            this.clientRepository.saveAndFlush(client);
                        }
                    } catch (Exception e) {
                        Throwable realCause = e;
                        if (e.getCause() != null) {
                            realCause = e.getCause();
                        }
                        LOG.error("Error occurred while posting client to Odoo with id " + client.getId() + " message "
                                + realCause.getMessage());
                        errors.add(realCause);
                    }
                }
            }

            if (errors.size() > 0) {
                throw new JobExecutionException(errors);
            }
        }
    }

    @Override
    public Boolean updateCustomerToOddo(Client client) {
        try {
            final Integer uid = loginToOddo();
            if (uid > 0) {
                XmlRpcClient models = getCommonConfig();
                Integer partnerId = getPartner(client.getId(), uid, models);
                // Update client
                if (partnerId != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", client.getDisplayName());
                    map.put("mobile", client.getMobileNo() != null ? client.getMobileNo() : false);

                    Boolean status = (Boolean) models.execute("execute_kw",
                            Arrays.asList(odooDB, uid, password, "res.partner", "write", Arrays.asList(Arrays.asList(partnerId), map)));

                    LOG.info("Odoo Client updated with id " + partnerId);
                    return status;
                }
            }
        } catch (XmlRpcException e) {
            throw new OdooFailedException(e);
        }
        return false;
    }

    @Override
    @CronTarget(jobName = JobName.POST_UPDATED_DETAILS_OF_CUSTOMER_TO_ODDO)
    public void postCustomerUpdatedDetailsToOddo() throws JobExecutionException {
        Boolean isOdooEnabled = this.configurationDomainService.isOdooIntegrationEnabled();
        if (isOdooEnabled) {
            List<Client> clients = this.clientRepository.getClientUpdatedDetailsNotPostedToOdoo(true);
            List<Throwable> errors = new ArrayList<>();

            if (clients != null && clients.size() > 0) {
                for (Client client : clients) {
                    try {
                        Boolean status = updateCustomerToOddo(client);
                        updateClientWithOdooUpdateStatus(status, client);
                    } catch (Exception e) {
                        Throwable realCause = e;
                        if (e.getCause() != null) {
                            realCause = e.getCause();
                        }
                        LOG.error("Error occurred while updating client to Odoo with id " + client.getId() + " message "
                                + realCause.getMessage());
                        errors.add(realCause);
                    }
                }
            }
            if (errors.size() > 0) {
                throw new JobExecutionException(errors);
            }
        }
    }

    public void updateClientWithOdooUpdateStatus(boolean status, Client client) {
        if (status) {
            client.setUpdatedToOdoo(true);
            this.clientRepository.saveAndFlush(client);
        }
    }

    @Override
    public JsonObject createJournalEntryToOddo(List<JournalEntry> list, Long loanTransactionId, Long transactionType, Boolean isReversed, String loanAccountNo)
            throws IOException, NoSuchAlgorithmException, KeyManagementException {

        final Integer uid = loginToOddo();
        if (uid > 0) {

            JournalItemData journalEntry = null;
            List<JournalItemData> journalItems = new ArrayList<>();

            JournalEntryToOdooData journalEntryToOdooData = new JournalEntryToOdooData();
            JournalData journalData = new JournalData();
            Client client = null;
            Office office = null;

            for (JournalEntry entry : list) {

                Integer accountId = extractGlCode(entry.getGlAccount().getGlCode());
                client = entry.getClient();
                office = entry.getOffice();

                journalEntry = new JournalItemData(entry, entry.getGlAccount().getGlCode());
                journalItems.add(journalEntry);
                if (accountId == null) {
                    throw new GeneralPlatformDomainRuleException(
                            "error.posting.journal.entries.to.odoo.has.failed.due.missing.gl.account.id",
                            "Error occurred while creating Journal Entry to Odoo with Loan Transaction Id  " + loanTransactionId
                                    + " and Type " + transactionType + " Error: GL Account  not found. GL Account ID on CBS  =: "
                                    + entry.getGlAccount().getId());
                }
            }

            // Create journal entry
            journalEntryToOdooData.setResourceId(loanTransactionId.toString());

            String ref = isReversed ? "Reversal of Journal Entry made by CBS for Loan ID : " + loanAccountNo +"; Transaction ID : L" + loanTransactionId :
                    "Journal Entry made by CBS for Loan ID : " + loanAccountNo +"; Transaction ID : L" + loanTransactionId ;

            Integer partnerId = client.getOdooCustomerId();
            if (partnerId == null) {
                throw new GeneralPlatformDomainRuleException(
                        "error.posting.journal.entries.to.odoo.has.failed.due.to.missing.client.id.or.partner.id",
                        "Error occurred while creating Journal Entry to Odoo with Loan Transaction Id  " + loanTransactionId
                                + " and Type " + transactionType + " Error: Client or Partner id not found. Client is Posted =  : "
                                + client.isOdooCustomerPosted());
            }

            journalData.setRef(ref);
            journalData.setTransactionId(loanTransactionId.toString());
            journalData.setTransactionTypeName(LoanTransactionType.fromInt(transactionType.intValue()).name());
            journalData.setTransactionTypeUniqueId(transactionType.toString());
            journalData.setReversed(isReversed);
            journalData.setClientId(client.getOdooCustomerId().longValue());
            journalData.setClientDisplayName(client.getDisplayName());
            journalData.setEntryDate(list.get(0).getTransactionDate().toString());
            journalData.setOfficeId(office.getId());
            journalData.setJournalItems(journalItems);

            journalEntryToOdooData.setResource(journalData);

            LOG.info("Journal Entry to Odoo " + journalEntryToOdooData);
            String jsonPayload = convertRequestPayloadToJson(journalEntryToOdooData);
            LOG.info("Journal Entry to Odoo JSON Payload " + jsonPayload);
            return sendRequest(jsonPayload);
        }
        return null;
    }

    @Override
    public String updateJournalEntryWithOdooStatus(String stringRequest) {
        LOG.info("Received Odoo Journal entry response: {}", stringRequest);

        JsonObject odooRequest = JsonParser.parseString(stringRequest).getAsJsonObject();
        JsonObject response = new JsonObject();

        String responseCode = getStringField(odooRequest, "responseCode");
        String responseMessage = getStringField(odooRequest, "responseMessage");
        String transactionId = getStringField(odooRequest, "cbs_journal_entry_id");

        if (transactionId == null) {
            LOG.warn("Odoo response missing 'cbs_journal_entry_id'");
            response.addProperty("success", false);
            response.addProperty("message", "cbs_journal_entry_id not found");
            response.addProperty("data", stringRequest);
            response.addProperty("ack", true);
            return response.toString();
        }

        List<JournalEntry> journalEntries = journalEntryRepository.findJournalEntriesByLoanTransactionId("L" + transactionId);

        if ("POSTED".equals(responseCode) || "REVERSED".equals(responseCode) || "EXISTING".equals(responseCode)) {
            String odooJournalId = getStringField(odooRequest, "journal_entry_no");
            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;

            if (odooJournalId != null) {
                // Map type -> amount from journalDetails
                Map<Integer, BigDecimal> amountMap = new HashMap<>(); // 1 -> credit, 2 -> debit

                if (odooRequest.has("journalDetails") && odooRequest.get("journalDetails").isJsonArray()) {
                    JsonArray journalDetails = odooRequest.getAsJsonArray("journalDetails");

                    // Process journalDetails if present
                    for (JsonElement detailElement : journalDetails) {
                        JsonObject detail = detailElement.getAsJsonObject();
                        debit = detail.get("debit").getAsBigDecimal();
                        credit = detail.get("credit").getAsBigDecimal();

                    }

                }

                for (JournalEntry je : journalEntries) {
                    if (je.getType() == 1) je.setOdooAmount(credit);
                    if (je.getType() == 2) je.setOdooAmount(debit);
                    je.setOddoPosted(true);
                    je.setOdooJournalId(odooJournalId);
                    je.setOdooResponse(responseCode);
                    this.journalEntryRepository.saveAndFlush(je);
                }
            }
        } else {
            LOG.info("Loan Transaction Not Posted to Odoo - Code:{} - Message: {}", responseCode, responseMessage);
            for (JournalEntry je : journalEntries) {
                je.setOdooResponse(responseCode + ": " + responseMessage);
                journalEntryRepository.saveAndFlush(je);
            }
        }

        response.addProperty("success", true);
        response.addProperty("message", "Successful");
        response.addProperty("ack", true);
        return response.toString();
    }



    private JsonObject sendRequest(String payload) throws IOException, NoSuchAlgorithmException, KeyManagementException {

        // Trust all certificates
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType){}

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType){}

                    @Override
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
                }
        };

        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        // Create OkHttpClient that ignores SSL validation
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0])
                .hostnameVerifier((hostname, session) -> true)
                .build();

        String authorization = Base64.getEncoder().encodeToString((username +":" +password).getBytes(UTF_8));

        RequestBody requestBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), payload);
        Request request = new Request.Builder()
                .url(celeryUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization","Basic " + authorization)
                .build();

        Response response = httpClient.newCall(request).execute();

        String resObject = response.body().string();
        if (response.isSuccessful()) {

            LOG.info("Response on Odoo Journal Entry Posting: " + resObject);
            return JsonParser.parseString(resObject).getAsJsonObject();
        } else {
            JsonObject js = JsonParser.parseString(resObject).getAsJsonObject();
            throw new GeneralPlatformDomainRuleException("error.msg.journal.entry.posting.to.odoo.failed",
                    " Failed to post Journal Entries to Odoo: " + response.code() + ":" + response.message() + " -Code From Odoo :-"
                            + getStringField(js, "responseCode") + " -Message From Odoo :-" + getStringField(js, "responseMessage"));
        }

    }

    @Override
    public JsonObject postJournalEntryToOddo(LocalDate fromDate, LocalDate toDate, Long officeId, String currency) {
        Boolean isOdooEnabled = this.configurationDomainService.isOdooIntegrationEnabled();
        List<Throwable> errors = new ArrayList<>();
        JsonObject response = new JsonObject();
        int transactions = 0;
        if (isOdooEnabled) {
            // get loan accounts with transactions not posted to Odoo
            List<LoanTransactionNotPostedToOdooInstanceData> loanTransactionNotPostedToOdooInstanceData = loanReadPlatformService
                    .retrieveLoanTransactionWhoseJournalEntriesAreNotPostedToOdoo(fromDate, toDate, officeId, currency);
            if (!CollectionUtils.isEmpty(loanTransactionNotPostedToOdooInstanceData)) {
                transactions = getTransactions(loanTransactionNotPostedToOdooInstanceData, errors, transactions);
                response.addProperty("responseMessage", "Posted Entries");
            }else{
                response.addProperty("responseMessage", "No entries to post");
            }

            response.addProperty("numberOfTransactions", transactions);

            if (errors.size() > 0) {
                List<String> errorMessages = new ArrayList<>();
                for( Throwable error : errors) {
                    errorMessages.add(error.getMessage());
                }
                Gson gson = new Gson();
                response.add("errors", gson.toJsonTree(errorMessages));
            }
            response.addProperty("responseCode", "DONE");
            return response;
        }

        response.addProperty("responseMessage", "Odoo not Enabled");
        response.addProperty("responseCode", "ERROR");

        return response;
    }

    @Override
    @CronTarget(jobName = JobName.POST_JOURNAL_ENTRY_TO_ODDO)
    public void postJournalEntryToOddo() throws JobExecutionException {
        Boolean isOdooEnabled = this.configurationDomainService.isOdooIntegrationEnabled();
        List<Throwable> errors = new ArrayList<>();
        if (isOdooEnabled) {
            // get loan accounts with transactions not posted to Odoo
            List<LoanTransactionNotPostedToOdooInstanceData> loanTransactionNotPostedToOdooInstanceData = loanReadPlatformService
                    .retrieveLoanTransactionWhoseJournalEntriesAreNotPostedToOdoo(DateUtils.getStartOfCurrentMonth(), DateUtils.getBusinessLocalDate(), null, null);
            LOG.trace("Loan Transaction Not Posted to Odoo " + loanTransactionNotPostedToOdooInstanceData.toString());
            if (!CollectionUtils.isEmpty(loanTransactionNotPostedToOdooInstanceData)) {
                getTransactions(loanTransactionNotPostedToOdooInstanceData, errors, 0);
            }
            if (errors.size() > 0) {
                throw new JobExecutionException(errors);
            }
        }
    }

    private int getTransactions(List<LoanTransactionNotPostedToOdooInstanceData> loanTransactionNotPostedToOdooInstanceData, List<Throwable> errors, int transactions) {
        for (LoanTransactionNotPostedToOdooInstanceData transaction : loanTransactionNotPostedToOdooInstanceData) {
            List<JournalEntry> JE = this.journalEntryRepository.findJournalEntriesByIsOddoPosted(false,
                    transaction.getLoanTransactionId());
            postJournalEntries(errors, JE, transaction.getLoanTransactionId(), transaction.getTransactionType(),
                    transaction.getIsReversed(), transaction.getLoanAccountNo(), transaction.getOffice());
            transactions +=1;

        }
        return transactions;
    }

    @Override
    public void postClientToOdooOnCreateTask(Client client) {
        try {
            this.genericExecutorService.execute(new PostClientCreationToOdoo(client, ThreadLocalContextUtil.getContext()));
        } catch (Exception ex) {
            // don't throw exception here
        }
    }

    @Override
    public void postClientToOdooOnUpdateTask(Map<String, Object> changes, Client client) {
        try {
            this.genericExecutorService.execute(new PostClientUpdateToOdoo(changes, client, ThreadLocalContextUtil.getContext()));
        } catch (Exception ex) {
            // don't throw exception here
        }
    }

    @Override
    public void postFailedClientsOnMigration(Client client, String errorMsg, String jsonObject) {
        try {
            this.genericExecutorService
                    .execute(new LogFailedClientCreationOnDataMigration(client, ThreadLocalContextUtil.getContext(), errorMsg, jsonObject));
        } catch (Exception ex) {
            // don't throw exception here
        }
    }

    @Override
    public void postFailedLoansOnMigration(BigDecimal amount, Long clientID, String odooLoanNumber, String odooLoanId, String errorMsg,
            String jsonObject) {
        try {
            this.genericExecutorService.execute(new LogFailedLoanCreationOnDataMigration(amount, clientID, odooLoanNumber, odooLoanId,
                    ThreadLocalContextUtil.getContext(), errorMsg, jsonObject));
        } catch (Exception ex) {
            // don't throw exception here
        }
    }

    @Override
    public void postFailedLoanRepaymentOnMigration(BigDecimal transactionAmount, Long loanId, String transactionDate, String note,
            String paymentType, String errorMsg, String jsonObject) {
        try {
            this.genericExecutorService.execute(new LogFailedLoanRepaymentOnDataMigration(transactionAmount, loanId, transactionDate, note,
                    paymentType, ThreadLocalContextUtil.getContext(), errorMsg, jsonObject));
        } catch (Exception ex) {
            // don't throw exception here
        }
    }

    private void postJournalEntries(List<Throwable> errors, List<JournalEntry> journalEntryDebitCredit, Long loanTransactionId,
            Long transactionType, Boolean isReversed, String loanAccountNo, String office) {
        if (!CollectionUtils.isEmpty(journalEntryDebitCredit)) {
            try {

                if (journalEntryDebitCredit.size() > 1) {
                    JsonObject odooAck = createJournalEntryToOddo(journalEntryDebitCredit, loanTransactionId, transactionType, isReversed, loanAccountNo);

                    boolean ack =  getBooleanField(odooAck,"ack");
                    boolean success = getBooleanField (odooAck,"success");
                    String message = getStringField(odooAck, "message");
                    if (success) {
                        for (JournalEntry je : journalEntryDebitCredit) {
                            je.setOdooAck(ack);
                            this.journalEntryRepository.saveAndFlush(je);
                        }
                    }
                    else {
                        Throwable throwable = new Throwable("Posting to odoo failed: "+ message);
                        LOG.error("Error occurred while Posting Journals to Odoo with Loan Transaction Id  " + loanTransactionId + " and Type "
                                + transactionType + message);
                        errors.add(throwable);
                    }
                }
            } catch (Exception e) {
                Throwable realCause = e;
                if (e.getCause() != null) {
                    realCause = e.getCause();
                }
                LOG.error("Error occurred while Posting Journals to Odoo with Loan Transaction Id  " + loanTransactionId + " and Type "
                        + transactionType + realCause.getMessage());
                errors.add(realCause);
            }
        }
    }

    /*
     * This will help extract the GL Code from the concatenated GL code with id posted to fineract during data
     * migration. The Code on Odoo is not unique so we concatenate the GL code with the GL id {code_id} so we need to
     * extract the GLCode from the code again if we want the integration with Odoo from Fineract work as expected. We
     * accommodated the aspect of the GL code not concatenated with the id if this is created in fineract direct
     */
    private Integer extractGlCode(String glCode) {
        if (glCode.contains("-")) {
            List<String> parts = Splitter.on(Pattern.compile("-", Pattern.LITERAL)).splitToList(glCode);
            return Integer.parseInt(parts.get(1));
        } else {
            throw new GeneralPlatformDomainRuleException("error.msg.gl.code.not.concatenated.with.id",
                    "The GL Code is not concatenated with the GL Id. Please ensure the GL Code is concatenated with the GL Id for :-"
                            + glCode);
        }
    }

    private String convertRequestPayloadToJson(JournalEntryToOdooData journalEntryToOdooData) {
        Gson gson = new GsonBuilder().create();
        String request = gson.toJson(journalEntryToOdooData);
        LOG.info("Actual (Journal Entries) Payload to be sent to Odoo API - - >" + request);
        return request;
    }

    public String getStringField(JsonObject jsonObject, String fieldName) {
        if (jsonObject != null && jsonObject.has(fieldName) && jsonObject.get(fieldName).isJsonPrimitive()
                && jsonObject.get(fieldName).getAsJsonPrimitive().isString()) {
            return jsonObject.get(fieldName).getAsString();
        }
        return null;
    }

    public boolean getBooleanField(JsonObject jsonObject, String fieldName) {
        if (jsonObject != null && jsonObject.has(fieldName) && jsonObject.get(fieldName).isJsonPrimitive()
                && jsonObject.get(fieldName).getAsJsonPrimitive().isBoolean()) {
            return jsonObject.get(fieldName).getAsBoolean();
        }
        return false;
    }

    class PostClientCreationToOdoo implements Runnable, ApplicationListener<ContextClosedEvent> {

        private final FineractContext context;
        private final Client client;

        public PostClientCreationToOdoo(Client client, FineractContext context) {
            this.context = context;
            this.client = client;
        }

        @Override
        public void run() {
            ThreadLocalContextUtil.init(context);
            postClientToOdoo(client);
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            genericExecutorService.shutdown();
            LOG.info("Shutting down the ExecutorService");
        }

        private void postClientToOdoo(Client newClient) {

            boolean isOdooIntegrationEnable = configurationDomainService.isOdooIntegrationEnabled();
            if (isOdooIntegrationEnable) {
                Integer customerId = createCustomerToOddo(newClient);
                if (customerId != null) {
                    newClient.setOdooCustomerId(customerId);
                    newClient.setOdooCustomerPosted(true);
                    clientRepository.save(newClient);
                }
            }
        }

    }

    class PostClientUpdateToOdoo implements Runnable, ApplicationListener<ContextClosedEvent> {

        private final FineractContext context;
        private final Client client;
        private final Map<String, Object> changes;

        public PostClientUpdateToOdoo(Map<String, Object> changes, Client client, FineractContext context) {
            this.context = context;
            this.client = client;
            this.changes = changes;
        }

        @Override
        public void run() {
            ThreadLocalContextUtil.init(context);
            updateClientOnOdoo(changes, client);
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            genericExecutorService.shutdown();
            LOG.info("Shutting down the ExecutorService");
        }

        private void updateClientOnOdoo(Map<String, Object> changes, Client clientForUpdate) {

            boolean isOdooEnabled = configurationDomainService.isOdooIntegrationEnabled();
            if (isOdooEnabled) {
                if (changes.containsKey(ClientApiConstants.firstnameParamName) || changes.containsKey(ClientApiConstants.fullnameParamName)
                        || changes.containsKey(ClientApiConstants.lastnameParamName)
                        || changes.containsKey(ClientApiConstants.middlenameParamName)
                        || changes.containsKey(ClientApiConstants.mobileNoParamName)) {

                    boolean status = updateCustomerToOddo(clientForUpdate);
                    if (status) {
                        clientForUpdate.setUpdatedToOdoo(true);
                    } else {
                        clientForUpdate.setUpdatedToOdoo(false);
                    }
                }
            } else {
                clientForUpdate.setUpdatedToOdoo(false);
            }

        }
    }

    class LogFailedClientCreationOnDataMigration implements Runnable, ApplicationListener<ContextClosedEvent> {

        private final FineractContext context;
        private final Client client;
        private final String errorMsg;
        private final String jsonObject;

        public LogFailedClientCreationOnDataMigration(Client client, FineractContext context, String errorMsg, String jsonObject) {
            this.context = context;
            this.client = client;
            this.errorMsg = errorMsg;
            this.jsonObject = jsonObject;
        }

        @Override
        public void run() {
            ThreadLocalContextUtil.init(context);
            postFailedClients(client, errorMsg, jsonObject);
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            genericExecutorService.shutdown();
            LOG.info("Shutting down the ExecutorService");
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        private void postFailedClients(Client newClient, String errorMsg, String jsonObject) {

            FailedClientCreationOnDataMigration failedClientCreationOnDataMigration = new FailedClientCreationOnDataMigration();
            failedClientCreationOnDataMigration.setExternalId(newClient.getExternalId());
            failedClientCreationOnDataMigration.setOffice(newClient.getOffice());
            failedClientCreationOnDataMigration.setClientType(newClient.getLegalForm());
            failedClientCreationOnDataMigration.setFirstname(newClient.getFirstname());
            failedClientCreationOnDataMigration.setLastname(newClient.getLastname());
            failedClientCreationOnDataMigration.setErrorMsg(errorMsg);
            failedClientCreationOnDataMigration.setJsonObject(jsonObject);

            failedClientCreationOnDataMigrationRepository.saveAndFlush(failedClientCreationOnDataMigration);
        }

    }

    class LogFailedLoanCreationOnDataMigration implements Runnable, ApplicationListener<ContextClosedEvent> {

        private final FineractContext context;
        private final String errorMsg;
        private final String jsonObject;
        private final BigDecimal amount;
        private final Long clientID;
        private final String odooLoanNumber;
        private final String odooLoanId;

        public LogFailedLoanCreationOnDataMigration(BigDecimal amount, Long clientID, String odooLoanNumber, String odooLoanId,
                FineractContext context, String errorMsg, String jsonObject) {
            this.context = context;
            this.amount = amount;
            this.clientID = clientID;
            this.odooLoanNumber = odooLoanNumber;
            this.odooLoanId = odooLoanId;
            this.errorMsg = errorMsg;
            this.jsonObject = jsonObject;
        }

        @Override
        public void run() {
            ThreadLocalContextUtil.init(context);
            postFailedLoans(amount, clientID, odooLoanNumber, odooLoanId, errorMsg, jsonObject);
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            genericExecutorService.shutdown();
            LOG.info("Shutting down the ExecutorService for failed loans  . . ");
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        private void postFailedLoans(BigDecimal amount, Long clientID, String odooLoanNumber, String odooLoanId, String errorMsg,
                String jsonObject) {

            FailedLoanCreationOnDataMigration failedLoanCreationOnDataMigration = new FailedLoanCreationOnDataMigration();
            failedLoanCreationOnDataMigration.setOdooLoanNumber(odooLoanNumber);
            failedLoanCreationOnDataMigration.setClient(clientID);
            failedLoanCreationOnDataMigration.setOdooLoanId(odooLoanId);
            failedLoanCreationOnDataMigration.setAmount(amount);
            failedLoanCreationOnDataMigration.setErrorMsg(errorMsg);
            failedLoanCreationOnDataMigration.setJsonObject(jsonObject);

            failedLoanCreationOnDataMigrationRepository.saveAndFlush(failedLoanCreationOnDataMigration);
        }

    }

    class LogFailedLoanRepaymentOnDataMigration implements Runnable, ApplicationListener<ContextClosedEvent> {

        private final FineractContext context;
        private final String errorMsg;
        private final String jsonObject;
        private final BigDecimal transactionAmount;
        private final Long loanId;
        private final String transactionDate;
        private final String paymentType;
        private final String note;

        public LogFailedLoanRepaymentOnDataMigration(BigDecimal transactionAmount, Long loanId, String transactionDate, String note,
                String paymentType, FineractContext context, String errorMsg, String jsonObject) {
            this.context = context;
            this.errorMsg = errorMsg;
            this.jsonObject = jsonObject;
            this.transactionAmount = transactionAmount;
            this.loanId = loanId;
            this.transactionDate = transactionDate;
            this.paymentType = paymentType;
            this.note = note;
        }

        @Override
        public void run() {
            ThreadLocalContextUtil.init(context);
            postFailedLoanRepayment(transactionAmount, loanId, transactionDate, note, paymentType, errorMsg, jsonObject);
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            genericExecutorService.shutdown();
            LOG.info("Shutting down the ExecutorService for failed loans   Repayment. . ");
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        private void postFailedLoanRepayment(BigDecimal transactionAmount, Long loanId, String transactionDate, String note,
                String paymentType, String errorMsg, String jsonObject) {

            FailedLoanRepaymentOnDataMigration failedLoanRepaymentOnDataMigration = new FailedLoanRepaymentOnDataMigration();
            failedLoanRepaymentOnDataMigration.setTransactionAmount(transactionAmount);
            failedLoanRepaymentOnDataMigration.setLoanId(loanId);
            failedLoanRepaymentOnDataMigration.setTransactionDate(transactionDate);
            failedLoanRepaymentOnDataMigration.setNote(note);
            failedLoanRepaymentOnDataMigration.setPaymentType(paymentType);
            failedLoanRepaymentOnDataMigration.setErrorMsg(errorMsg);
            failedLoanRepaymentOnDataMigration.setJsonObject(jsonObject);

            failedLoanRepaymentOnDataMigrationRepository.saveAndFlush(failedLoanRepaymentOnDataMigration);
        }

    }

}
