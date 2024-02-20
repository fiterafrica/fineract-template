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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.fineract.accounting.journalentry.domain.JournalEntry;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.infrastructure.Odoo.exception.OdooFailedException;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({ "unchecked", "rawtypes", "cast" })
public class OdooServiceImpl implements OdooService {

    private static final Logger LOG = LoggerFactory.getLogger(OdooServiceImpl.class);

    @Value("${fineract.integrations.odoo.db}")
    private String odooDB;

    @Value("${fineract.integrations.odoo.username}")
    private String username;

    @Value("${fineract.integrations.odoo.password}")
    private String password;

    @Value("${fineract.integrations.odoo.url}")
    private String url;
    private ClientRepositoryWrapper clientRepository;
    private ConfigurationDomainService configurationDomainService;

    private final JournalEntryRepository journalEntryRepository;

    @Autowired
    public OdooServiceImpl(ClientRepositoryWrapper clientRepository, ConfigurationDomainService configurationDomainService,
            JournalEntryRepository journalEntryRepository) {
        this.clientRepository = clientRepository;
        this.configurationDomainService = configurationDomainService;
        this.journalEntryRepository = journalEntryRepository;
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
    public Integer createJournalEntryToOddo(List<JournalEntry> list) {
        try {
            final Integer uid = loginToOddo();
            if (uid > 0) {
                XmlRpcClient models = getCommonConfig();

                Integer currencyId = getCurrency(list.get(0), uid, models);
                Integer id = 0;

                JournalEntry debitJournalEntry = null;
                JournalEntry creditJournalEntry = null;

                for (JournalEntry journalEntry : list) {
                    if (journalEntry.isDebitEntry()) {
                        debitJournalEntry = journalEntry;
                    } else {
                        creditJournalEntry = journalEntry;
                    }
                }
                JournalEntry debit = debitJournalEntry != null ? debitJournalEntry : list.get(0);
                JournalEntry credit = creditJournalEntry != null ? creditJournalEntry : list.get(1);

                Integer debitAccountId = getGlAccounts(debit, uid, models);
                Integer creditAccountId = getGlAccounts(credit, uid, models);

                if (debitAccountId == null || creditAccountId == null) {
                    return null;
                }

                Integer debitPartnerId = getPartner((debit.getClient() != null ? debit.getClient().getId() : null), uid, models);
                Integer creditPartnerId = getPartner((credit.getClient() != null ? credit.getClient().getId() : null), uid, models);

                // Create journal entry
                if (debit.getAmount().doubleValue() != 0 || credit.getAmount().doubleValue() != 0) {
                    id = (Integer) models.execute("execute_kw",
                            Arrays.asList(
                                    odooDB, uid, password, "account.move", "create", Arrays.asList(Map.of("line_ids", Arrays.asList(
                                            Arrays.asList(0, 0,
                                                    Map.of("account_id", debitAccountId, "amount_currency", currencyId, "credit", 0,
                                                            "debit", debit.getAmount().doubleValue(), "partner_id",
                                                            debitPartnerId != null ? debitPartnerId
                                                                    : false,
                                                            "name", debit.getEntityId() != null ? debit.getEntityId().toString() : false)),
                                            Arrays.asList(0, 0,
                                                    Map.of("account_id", creditAccountId, "amount_currency", currencyId, "credit",
                                                            credit.getAmount().doubleValue(), "debit", 0, "partner_id",
                                                            creditPartnerId != null ? creditPartnerId : false, "name",
                                                            credit.getEntityId() != null ? credit.getEntityId().toString() : false)))))));
                    LOG.info("Odoo Journal Entry created with id " + id);
                    Boolean status = (Boolean) models.execute("execute_kw",
                            Arrays.asList(odooDB, uid, password, "account.move", "action_post", Arrays.asList(Arrays.asList(id))));
                    LOG.info("Odoo Journal Entry posted Successfully " + status);
                    return id;
                }
            }

        } catch (XmlRpcException e) {
            throw new OdooFailedException(e);
        }
        return null;
    }

    @Override
    @CronTarget(jobName = JobName.POST_JOURNAL_ENTRY_TO_ODDO)
    public void postJournalEntryToOddo() throws JobExecutionException {
        Boolean isOdooEnabled = this.configurationDomainService.isOdooIntegrationEnabled();
        if (isOdooEnabled) {
            List<JournalEntry> JE = this.journalEntryRepository.findJournalEntriesByIsOddoPosted(false);

            List<Throwable> errors = new ArrayList<>();

            List<JournalEntry> journalEntryDebitCredit = new ArrayList<>();
            if (JE != null && JE.size() > 0) {
                for (JournalEntry entry : JE) {

                    try {
                        journalEntryDebitCredit.add(entry);

                        if (journalEntryDebitCredit.size() == 2) {
                            Integer id = createJournalEntryToOddo(journalEntryDebitCredit);
                            if (id != null) {
                                for (JournalEntry je : journalEntryDebitCredit) {
                                    je.setOddoPosted(true);
                                    je.setOdooJournalId(id);
                                    this.journalEntryRepository.saveAndFlush(je);
                                }
                                journalEntryDebitCredit.clear();
                            }
                            journalEntryDebitCredit.clear();
                        }
                    } catch (Exception e) {
                        Throwable realCause = e;
                        if (e.getCause() != null) {
                            realCause = e.getCause();
                        }
                        LOG.error("Error occurred while updating Journals to Odoo with entity id  " + entry.getEntityId() + " message "
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

    private Integer getCurrency(JournalEntry entry, Integer uid, XmlRpcClient models) {
        List currency;
        try {
            if (uid > 0) {
                currency = Arrays.asList((Object[]) models.execute("execute_kw",
                        Arrays.asList(odooDB, uid, password, "res.currency", "search_read",
                                Arrays.asList(Arrays.asList(Arrays.asList("name", "=", entry.getCurrencyCode()))), Map.of(

                                        "fields", Arrays.asList("id"), "limit", 5))));
                Integer currencyId = null;
                if (currency != null && currency.size() > 0) {
                    HashMap currencyData = (HashMap) currency.get(0);
                    currencyId = (Integer) currencyData.get("id");
                }

                return currencyId;
            }
        } catch (XmlRpcException e) {
            throw new OdooFailedException(e);
        }
        return null;
    }

    private Integer getGlAccounts(JournalEntry entry, Integer uid, XmlRpcClient models) {
        Integer id = null;
        try {
            if (uid > 0) {
                final List glAccount = Arrays.asList((Object[]) models.execute("execute_kw",
                        Arrays.asList(odooDB, uid, password, "account.account", "search_read",
                                Arrays.asList(Arrays.asList(Arrays.asList("code", "=", extractGlCode(entry.getGlAccount().getGlCode())))),
                                Map.of("fields", Arrays.asList("id"), "limit", 5))));
                if (!CollectionUtils.isEmpty(glAccount)) {
                    HashMap account = (HashMap) glAccount.get(0);
                    id = (Integer) account.get("id");
                }

                return id;
            }
        } catch (XmlRpcException e) {
            throw new OdooFailedException(e);
        }
        return id;
    }

    /*
     * This will help extract the GL Code from the concatenated GL code with id posted to fineract during data
     * migration. The Code on Odoo is not unique so we concatenate the GL code with the GL id {code_id} so we need to
     * extract the GLCode from the code again if we want the integration with Odoo from Fineract work as expected. We
     * accommodated the aspect of the GL code not concatenated with the id if this is created in fineract direct
     */
    private Integer extractGlCode(String glCode) {
        if (glCode.contains("_")) {
            List<String> parts = Splitter.on(Pattern.compile("-", Pattern.LITERAL)).splitToList(glCode);

            try {
                return Integer.parseInt(parts.get(0));
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            try {
                return Integer.parseInt(glCode);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

}
