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
package org.apache.fineract.portfolio.accountdetails.service;

import com.google.common.base.Splitter;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.accountdetails.data.AccountSummaryCollectionData;
import org.apache.fineract.portfolio.accountdetails.data.GuarantorAccountSummaryData;
import org.apache.fineract.portfolio.accountdetails.data.LoanAccountSummaryData;
import org.apache.fineract.portfolio.accountdetails.data.SavingsAccountSummaryData;
import org.apache.fineract.portfolio.accountdetails.data.ShareAccountSummaryData;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.group.service.GroupReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.LoanApplicationTimelineData;
import org.apache.fineract.portfolio.loanaccount.data.LoanStatusEnumData;
import org.apache.fineract.portfolio.loanaccount.data.LoanSummaryData;
import org.apache.fineract.portfolio.loanproduct.service.LoanEnumerations;
import org.apache.fineract.portfolio.savings.data.SavingsAccountApplicationTimelineData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountStatusEnumData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountSubStatusEnumData;
import org.apache.fineract.portfolio.savings.service.SavingsEnumerations;
import org.apache.fineract.portfolio.shareaccounts.data.ShareAccountApplicationTimelineData;
import org.apache.fineract.portfolio.shareaccounts.data.ShareAccountStatusEnumData;
import org.apache.fineract.portfolio.shareaccounts.service.SharesEnumerations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccountDetailsReadPlatformServiceJpaRepositoryImpl implements AccountDetailsReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final ClientReadPlatformService clientReadPlatformService;
    private final GroupReadPlatformService groupReadPlatformService;
    private final ColumnValidator columnValidator;

    private final DatabaseSpecificSQLGenerator sqlGenerator;

    @Autowired
    public AccountDetailsReadPlatformServiceJpaRepositoryImpl(final ClientReadPlatformService clientReadPlatformService,
            final JdbcTemplate jdbcTemplate, final GroupReadPlatformService groupReadPlatformService, final ColumnValidator columnValidator,
            DatabaseSpecificSQLGenerator sqlGenerator) {
        this.clientReadPlatformService = clientReadPlatformService;
        this.jdbcTemplate = jdbcTemplate;
        this.groupReadPlatformService = groupReadPlatformService;
        this.columnValidator = columnValidator;
        this.sqlGenerator = sqlGenerator;
    }

    @Override
    public AccountSummaryCollectionData retrieveClientAccountDetails(final Long clientId, String filter) {

        // Supported search fields are "loanAccounts, savingsAccounts, shareAccounts, glimAccounts,
        // guarantorLoanAccounts"
        final List<String> requiredFilter = getRequiredAccountFilters(5, filter);

        this.clientReadPlatformService.validateClient(clientId);
        String loanwhereClause = null;
        String glimLoanClause = null;
        String savingswhereClause = null;
        String guarantorWhereClause = null;
        String shareWhereClause = null;

        for (final String requiredAccount : requiredFilter) {
            switch (requiredAccount.toLowerCase()) {

                case "loanaccounts":
                    loanwhereClause = " where l.client_id = ?";
                break;
                case "savingsaccounts":
                    savingswhereClause = " where sa.client_id = ? order by sa.status_enum ASC, sa.account_no ASC";
                break;
                case "shareaccounts":
                    shareWhereClause = " where sa.client_id = ? ";
                break;
                case "glimaccounts":
                    glimLoanClause = " where l.client_id = ? and l.loan_type_enum=4";
                break;
                case "guarantorloanaccounts":
                    guarantorWhereClause = " where g.entity_id = ? and g.is_active = true order by l.account_no ASC";
                break;
                default:
                    throw new PlatformDataIntegrityException("error.msg.account.data.integrity.issue",
                            "Invalid filter option: " + requiredAccount);

            }
        }

        final AccountSummaryCollectionData.AccountSummaryCollectionDataBuilder builder = AccountSummaryCollectionData.builder();

        if (glimLoanClause != null) {
            final List<LoanAccountSummaryData> glimAccounts = retrieveLoanAccountDetails(glimLoanClause, new Object[] { clientId });
            builder.groupLoanIndividualMonitoringAccounts(glimAccounts);
        }

        if (savingswhereClause != null) {
            final List<SavingsAccountSummaryData> savingsAccounts = retrieveAccountDetails(savingswhereClause, new Object[] { clientId });
            builder.savingsAccounts(savingsAccounts);
        }

        if (loanwhereClause != null) {
            final List<LoanAccountSummaryData> loanAccounts = retrieveLoanAccountDetails(loanwhereClause, new Object[] { clientId });
            builder.loanAccounts(loanAccounts);
        }

        if (shareWhereClause != null) {
            final List<ShareAccountSummaryData> shareAccounts = retrieveShareAccountDetails(shareWhereClause, new Object[] { clientId });
            builder.shareAccounts(shareAccounts);
        }

        if (guarantorWhereClause != null) {
            final List<GuarantorAccountSummaryData> guarantorloanAccounts = retrieveGuarantorLoanAccountDetails(guarantorWhereClause,
                    new Object[] { clientId });
            builder.guarantorAccounts(guarantorloanAccounts);
        }

        return builder.build();
    }

    @Override
    public AccountSummaryCollectionData retrieveGroupAccountDetails(final Long groupId, String fields) {

        // Supported search fields are "loanAccounts, savingsAccounts, shareAccounts, glimAccounts,
        // guarantorLoanAccounts"
        final List<String> requiredFilter = getRequiredAccountFilters(7, fields);
        // Check if group exists
        this.groupReadPlatformService.validateGroup(groupId);

        String loanWhereClauseForGroup = null;
        String loanWhereClauseForGroupAndLoanType = null;
        String loanWhereClauseForMembers = null;
        String savingswhereClauseForGroup = null;
        String savingswhereClauseForMembers = null;

        String guarantorWhereClauseForGroup = null;
        String guarantorWhereClauseForMembers = null;

        for (String requiredAccount : requiredFilter) {
            switch (requiredAccount.toLowerCase()) {
                case "loanaccounts":
                    loanWhereClauseForGroup = " where l.group_id = ? and l.client_id is null";
                break;
                case "grouploanindividualmonitoringaccounts":
                    loanWhereClauseForGroupAndLoanType = " where l.group_id = ? and l.loan_type_enum=4";
                break;
                case "savingsaccounts":
                    savingswhereClauseForGroup = " where sa.group_id = ? and sa.client_id is null order by sa.status_enum ASC, sa.account_no ASC";
                break;
                case "memberloanaccounts":
                    loanWhereClauseForMembers = " where l.group_id = ? and l.client_id is not null";
                break;
                case "membersavingsaccounts":
                    savingswhereClauseForMembers = " where sa.group_id = ? and sa.client_id is not null order by sa.status_enum ASC, sa.account_no ASC";
                break;
                case "guarantorloanaccounts":
                    guarantorWhereClauseForGroup = " where l.group_id = ? and l.client_id is null and g.is_active = true order by l.account_no ASC";
                break;
                case "memberguarantorloanaccounts":
                    guarantorWhereClauseForMembers = " where l.group_id = ? and l.client_id is not null and g.is_active = true order by l.account_no ASC";
                break;
                default:
                    throw new PlatformDataIntegrityException("error.msg.account.data.integrity.issue",
                            "Invalid filter option: " + requiredAccount);
            }
        }

        final AccountSummaryCollectionData.AccountSummaryCollectionDataBuilder builder = AccountSummaryCollectionData.builder();
        if (loanWhereClauseForGroupAndLoanType != null) {
            final List<LoanAccountSummaryData> glimAccounts = retrieveLoanAccountDetails(loanWhereClauseForGroupAndLoanType,
                    new Object[] { groupId });
            builder.groupLoanIndividualMonitoringAccounts(glimAccounts);
        }

        if (loanWhereClauseForGroup != null) {
            final List<LoanAccountSummaryData> groupLoanAccounts = retrieveLoanAccountDetails(loanWhereClauseForGroup,
                    new Object[] { groupId });
            builder.loanAccounts(groupLoanAccounts);
        }

        if (savingswhereClauseForGroup != null) {
            final List<SavingsAccountSummaryData> groupSavingsAccounts = retrieveAccountDetails(savingswhereClauseForGroup,
                    new Object[] { groupId });
            builder.savingsAccounts(groupSavingsAccounts);
        }

        if (guarantorWhereClauseForGroup != null) {
            final List<GuarantorAccountSummaryData> groupGuarantorloanAccounts = retrieveGuarantorLoanAccountDetails(
                    guarantorWhereClauseForGroup, new Object[] { groupId });
            builder.guarantorAccounts(groupGuarantorloanAccounts);
        }

        if (loanWhereClauseForMembers != null) {
            final List<LoanAccountSummaryData> memberLoanAccounts = retrieveLoanAccountDetails(loanWhereClauseForMembers,
                    new Object[] { groupId });
            builder.memberLoanAccounts(memberLoanAccounts);
        }

        if (savingswhereClauseForMembers != null) {
            final List<SavingsAccountSummaryData> memberSavingsAccounts = retrieveAccountDetails(savingswhereClauseForMembers,
                    new Object[] { groupId });
            builder.memberSavingsAccounts(memberSavingsAccounts);
        }

        if (guarantorWhereClauseForMembers != null) {
            final List<GuarantorAccountSummaryData> memberGuarantorloanAccounts = retrieveGuarantorLoanAccountDetails(
                    guarantorWhereClauseForMembers, new Object[] { groupId });
            builder.memberGuarantorAccounts(memberGuarantorloanAccounts);
        }

        return builder.build();

    }

    @Override
    public AccountSummaryCollectionData retrieveGroupAccountDetails(final Long groupId, final Long gsimId) {
        // Check if group exists
        this.groupReadPlatformService.retrieveOne(groupId);
        final String loanWhereClauseForGroup = " where l.group_id = ? and l.client_id is null";
        final String loanWhereClauseForGroupAndLoanType = " where l.group_id = ? and l.loan_type_enum=4";
        final String loanWhereClauseForMembers = " where l.group_id = ? and l.client_id is not null";
        final String savingswhereClauseForGroup = " where sa.group_id = ? and sa.gsim_id = ? sa.client_id is null order by sa.status_enum ASC, sa.account_no ASC";
        final String savingswhereClauseForMembers = " where sa.group_id = ? and sa.client_id is not null order by sa.status_enum ASC, sa.account_no ASC";

        final List<LoanAccountSummaryData> glimAccounts = retrieveLoanAccountDetails(loanWhereClauseForGroupAndLoanType,
                new Object[] { groupId });
        final List<LoanAccountSummaryData> groupLoanAccounts = retrieveLoanAccountDetails(loanWhereClauseForGroup,
                new Object[] { groupId });
        final List<SavingsAccountSummaryData> gsimSavingsAccounts = retrieveAccountDetails(savingswhereClauseForGroup,
                new Object[] { groupId, gsimId });
        final List<LoanAccountSummaryData> memberLoanAccounts = retrieveLoanAccountDetails(loanWhereClauseForMembers,
                new Object[] { groupId });
        final List<SavingsAccountSummaryData> memberSavingsAccounts = retrieveAccountDetails(savingswhereClauseForMembers,
                new Object[] { groupId });
        return new AccountSummaryCollectionData(groupLoanAccounts, glimAccounts, gsimSavingsAccounts, null, memberLoanAccounts,
                memberSavingsAccounts, null);
    }

    @Override
    public Collection<LoanAccountSummaryData> retrieveClientLoanAccountsByLoanOfficerId(final Long clientId, final Long loanOfficerId) {
        // Check if client exists
        this.clientReadPlatformService.retrieveOne(clientId);
        final String loanWhereClause = " where l.client_id = ? and l.loan_officer_id = ?";
        return retrieveLoanAccountDetails(loanWhereClause, new Object[] { clientId, loanOfficerId });
    }

    @Override
    public Collection<LoanAccountSummaryData> retrieveGroupLoanAccountsByLoanOfficerId(final Long groupId, final Long loanOfficerId) {
        // Check if group exists
        this.groupReadPlatformService.retrieveOne(groupId);
        final String loanWhereClause = " where l.group_id = ? and l.client_id is null and l.loan_officer_id = ?";
        return retrieveLoanAccountDetails(loanWhereClause, new Object[] { groupId, loanOfficerId });
    }

    @Override
    public Collection<LoanAccountSummaryData> retrieveClientActiveLoanAccountSummary(final Long clientId) {
        final String loanWhereClause = " where l.client_id = ? and l.loan_status_id = 300 ";
        return retrieveLoanAccountDetails(loanWhereClause, new Object[] { clientId });
    }

    @Override
    public List<LoanAccountSummaryData> retrieveLoanAccountDetailsByGroupIdAndGlimAccountNumber(final Long groupId,
            final String glimAccount) {
        final LoanAccountSummaryDataMapper rm = new LoanAccountSummaryDataMapper(this.sqlGenerator);
        final String loanWhereClauseForGroupAndLoanType = " where l.group_id =? and glim.account_number=? and l.loan_type_enum=4";
        final String sql = "select " + rm.loanAccountSummarySchema() + loanWhereClauseForGroupAndLoanType;
        return this.jdbcTemplate.query(sql, rm, new Object[] { groupId, glimAccount }); // NOSONAR
    }

    @Override
    public Collection<LoanAccountSummaryData> retrieveGroupActiveLoanAccountSummary(final Long groupId) {
        final String loanWhereClause = " where l.group_id = ? and l.loan_status_id = 300 and l.client_id is null";
        return retrieveLoanAccountDetails(loanWhereClause, new Object[] { groupId });
    }

    private List<LoanAccountSummaryData> retrieveLoanAccountDetails(final String loanwhereClause, final Object[] inputs) {
        final LoanAccountSummaryDataMapper rm = new LoanAccountSummaryDataMapper(this.sqlGenerator);
        final String sql = "select " + rm.loanAccountSummarySchema() + loanwhereClause;
        this.columnValidator.validateSqlInjection(rm.loanAccountSummarySchema(), loanwhereClause);
        return this.jdbcTemplate.query(sql, rm, inputs); // NOSONAR
    }

    /**
     * @param savingswhereClause
     * @param inputs
     * @return
     */
    private List<SavingsAccountSummaryData> retrieveAccountDetails(final String savingswhereClause, final Object[] inputs) {
        final SavingsAccountSummaryDataMapper savingsAccountSummaryDataMapper = new SavingsAccountSummaryDataMapper();
        final String savingsSql = "select " + savingsAccountSummaryDataMapper.schema() + savingswhereClause;
        this.columnValidator.validateSqlInjection(savingsAccountSummaryDataMapper.schema(), savingswhereClause);
        return this.jdbcTemplate.query(savingsSql, savingsAccountSummaryDataMapper, inputs); // NOSONAR
    }

    private List<ShareAccountSummaryData> retrieveShareAccountDetails(final String whereClause, final Object[] inputs) {
        final ShareAccountSummaryDataMapper mapper = new ShareAccountSummaryDataMapper();
        final String query = "select " + mapper.schema() + whereClause;
        return this.jdbcTemplate.query(query, mapper, inputs); // NOSONAR
    }

    private List<GuarantorAccountSummaryData> retrieveGuarantorLoanAccountDetails(final String loanwhereClause, final Object[] inputs) {
        final GuarantorLoanAccountSummaryDataMapper rm = new GuarantorLoanAccountSummaryDataMapper();
        final String sql = "select " + rm.guarantorLoanAccountSummarySchema() + loanwhereClause;
        return this.jdbcTemplate.query(sql, rm, inputs); // NOSONAR
    }

    private static final class ShareAccountSummaryDataMapper implements RowMapper<ShareAccountSummaryData> {

        private final String schema;

        ShareAccountSummaryDataMapper() {
            final StringBuilder buff = new StringBuilder()
                    .append("sa.id as id, sa.external_id as externalId, sa.status_enum as statusEnum, ")
                    .append("sa.account_no as accountNo, sa.total_approved_shares as approvedShares, sa.total_pending_shares as pendingShares, ")
                    .append("sa.savings_account_id as savingsAccountNo, sa.minimum_active_period_frequency as minimumactivePeriod,")
                    .append("sa.minimum_active_period_frequency_enum as minimumactivePeriodEnum,")
                    .append("sa.lockin_period_frequency as lockinPeriod, sa.lockin_period_frequency_enum as lockinPeriodEnum, ")
                    .append("sa.submitted_date as submittedDate, sbu.username as submittedByUsername, ")
                    .append("sbu.firstname as submittedByFirstname, sbu.lastname as submittedByLastname, ")
                    .append("sa.rejected_date as rejectedDate, rbu.username as rejectedByUsername, ")
                    .append("rbu.firstname as rejectedByFirstname, rbu.lastname as rejectedByLastname, ")
                    .append("sa.approved_date as approvedDate, abu.username as approvedByUsername, ")
                    .append("abu.firstname as approvedByFirstname, abu.lastname as approvedByLastname, ")
                    .append("sa.activated_date as activatedDate, avbu.username as activatedByUsername, ")
                    .append("avbu.firstname as activatedByFirstname, avbu.lastname as activatedByLastname, ")
                    .append("sa.closed_date as closedDate, cbu.username as closedByUsername, ")
                    .append("cbu.firstname as closedByFirstname, cbu.lastname as closedByLastname, ")
                    .append("sa.currency_code as currencyCode, sa.currency_digits as currencyDigits, sa.currency_multiplesof as inMultiplesOf, ")
                    .append("curr.name as currencyName, curr.internationalized_name_code as currencyNameCode, ")
                    .append("curr.display_symbol as currencyDisplaySymbol, sa.product_id as productId, p.name as productName, p.short_name as shortProductName ")
                    .append("from m_share_account sa ").append("join m_share_product as p on p.id = sa.product_id ")
                    .append("join m_currency curr on curr.code = sa.currency_code ")
                    .append("left join m_appuser sbu on sbu.id = sa.submitted_userid ")
                    .append("left join m_appuser rbu on rbu.id = sa.rejected_userid ")
                    .append("left join m_appuser abu on abu.id = sa.approved_userid ")
                    .append("left join m_appuser avbu on avbu.id = sa.activated_userid ")
                    .append("left join m_appuser cbu on cbu.id = sa.closed_userid ");
            schema = buff.toString();
        }

        @Override
        public ShareAccountSummaryData mapRow(ResultSet rs, int rowNum) throws SQLException {

            final Long id = JdbcSupport.getLong(rs, "id");
            final String accountNo = rs.getString("accountNo");
            final Long approvedShares = JdbcSupport.getLong(rs, "approvedShares");
            final Long pendingShares = JdbcSupport.getLong(rs, "pendingShares");
            final String externalId = rs.getString("externalId");
            final Long productId = JdbcSupport.getLong(rs, "productId");
            final String productName = rs.getString("productName");
            final String shortProductName = rs.getString("shortProductName");
            final Integer statusId = JdbcSupport.getInteger(rs, "statusEnum");
            final ShareAccountStatusEnumData status = SharesEnumerations.status(statusId);
            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final CurrencyData currency = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf, currencyDisplaySymbol,
                    currencyNameCode);

            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedDate");
            final String submittedByUsername = rs.getString("submittedByUsername");
            final String submittedByFirstname = rs.getString("submittedByFirstname");
            final String submittedByLastname = rs.getString("submittedByLastname");

            final LocalDate rejectedOnDate = JdbcSupport.getLocalDate(rs, "rejectedDate");
            final String rejectedByUsername = rs.getString("rejectedByUsername");
            final String rejectedByFirstname = rs.getString("rejectedByFirstname");
            final String rejectedByLastname = rs.getString("rejectedByLastname");

            final LocalDate approvedOnDate = JdbcSupport.getLocalDate(rs, "approvedDate");
            final String approvedByUsername = rs.getString("approvedByUsername");
            final String approvedByFirstname = rs.getString("approvedByFirstname");
            final String approvedByLastname = rs.getString("approvedByLastname");

            final LocalDate activatedOnDate = JdbcSupport.getLocalDate(rs, "activatedDate");
            final String activatedByUsername = rs.getString("activatedByUsername");
            final String activatedByFirstname = rs.getString("activatedByFirstname");
            final String activatedByLastname = rs.getString("activatedByLastname");

            final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedDate");
            final String closedByUsername = rs.getString("closedByUsername");
            final String closedByFirstname = rs.getString("closedByFirstname");
            final String closedByLastname = rs.getString("closedByLastname");

            final ShareAccountApplicationTimelineData timeline = new ShareAccountApplicationTimelineData(submittedOnDate,
                    submittedByUsername, submittedByFirstname, submittedByLastname, rejectedOnDate, rejectedByUsername, rejectedByFirstname,
                    rejectedByLastname, approvedOnDate, approvedByUsername, approvedByFirstname, approvedByLastname, activatedOnDate,
                    activatedByUsername, activatedByFirstname, activatedByLastname, closedOnDate, closedByUsername, closedByFirstname,
                    closedByLastname);

            return new ShareAccountSummaryData(id, accountNo, externalId, productId, productName, shortProductName, status, currency,
                    approvedShares, pendingShares, timeline);
        }

        public String schema() {
            return this.schema;
        }
    }

    private static final class SavingsAccountSummaryDataMapper implements RowMapper<SavingsAccountSummaryData> {

        final String schemaSql;

        SavingsAccountSummaryDataMapper() {
            final StringBuilder accountsSummary = new StringBuilder();
            accountsSummary.append("sa.id as id, sa.account_no as accountNo, sa.external_id as externalId, sa.status_enum as statusEnum, ");
            accountsSummary.append("sa.account_type_enum as accountType, ");
            accountsSummary.append(
                    "sa.account_balance_derived as accountBalance, sa.closed_fixed_deposit_account_no as closedFixedDepositAccountNumber,");

            accountsSummary.append("sa.submittedon_date as submittedOnDate,");
            accountsSummary.append("sbu.username as submittedByUsername,");
            accountsSummary.append("sbu.firstname as submittedByFirstname, sbu.lastname as submittedByLastname,");

            accountsSummary.append("sa.rejectedon_date as rejectedOnDate,");
            accountsSummary.append("rbu.username as rejectedByUsername,");
            accountsSummary.append("rbu.firstname as rejectedByFirstname, rbu.lastname as rejectedByLastname,");

            accountsSummary.append("sa.withdrawnon_date as withdrawnOnDate,");
            accountsSummary.append("wbu.username as withdrawnByUsername,");
            accountsSummary.append("wbu.firstname as withdrawnByFirstname, wbu.lastname as withdrawnByLastname,");

            accountsSummary.append("sa.approvedon_date as approvedOnDate,");
            accountsSummary.append("abu.username as approvedByUsername,");
            accountsSummary.append("abu.firstname as approvedByFirstname, abu.lastname as approvedByLastname,");

            accountsSummary.append("sa.activatedon_date as activatedOnDate,");
            accountsSummary.append("avbu.username as activatedByUsername,");
            accountsSummary.append("avbu.firstname as activatedByFirstname, avbu.lastname as activatedByLastname,");

            accountsSummary.append("sa.sub_status_enum as subStatusEnum, ");
            accountsSummary.append("(select coalesce(max(sat.transaction_date),sa.activatedon_date) ");
            accountsSummary.append("from m_savings_account_transaction as sat ");
            accountsSummary.append("where sat.is_reversed = false ");
            accountsSummary.append("and sat.transaction_type_enum in (1,2) ");
            accountsSummary.append("and sat.savings_account_id = sa.id) as lastActiveTransactionDate, ");

            accountsSummary.append("sa.closedon_date as closedOnDate,");
            accountsSummary.append("cbu.username as closedByUsername,");
            accountsSummary.append("cbu.firstname as closedByFirstname, cbu.lastname as closedByLastname,");

            accountsSummary.append(
                    "sa.currency_code as currencyCode, sa.currency_digits as currencyDigits, sa.currency_multiplesof as inMultiplesOf, ");
            accountsSummary.append("curr.name as currencyName, curr.internationalized_name_code as currencyNameCode, ");
            accountsSummary.append("curr.display_symbol as currencyDisplaySymbol, ");
            accountsSummary.append("sa.product_id as productId, p.name as productName, p.short_name as shortProductName, ");
            accountsSummary.append("sa.deposit_type_enum as depositType ");
            accountsSummary.append("from m_savings_account sa ");
            accountsSummary.append("join m_savings_product as p on p.id = sa.product_id ");
            accountsSummary.append("join m_currency curr on curr.code = sa.currency_code ");
            accountsSummary.append("left join m_appuser sbu on sbu.id = sa.submittedon_userid ");
            accountsSummary.append("left join m_appuser rbu on rbu.id = sa.rejectedon_userid ");
            accountsSummary.append("left join m_appuser wbu on wbu.id = sa.withdrawnon_userid ");
            accountsSummary.append("left join m_appuser abu on abu.id = sa.approvedon_userid ");
            accountsSummary.append("left join m_appuser avbu on rbu.id = sa.activatedon_userid ");
            accountsSummary.append("left join m_appuser cbu on cbu.id = sa.closedon_userid ");

            this.schemaSql = accountsSummary.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public SavingsAccountSummaryData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = JdbcSupport.getLong(rs, "id");
            final String accountNo = rs.getString("accountNo");
            final String closedFixedDepositAccountNumber = rs.getString("closedFixedDepositAccountNumber");
            final String externalId = rs.getString("externalId");
            final Long productId = JdbcSupport.getLong(rs, "productId");
            final String productName = rs.getString("productName");
            final String shortProductName = rs.getString("shortProductName");
            final Integer statusId = JdbcSupport.getInteger(rs, "statusEnum");
            final BigDecimal accountBalance = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "accountBalance");
            final SavingsAccountStatusEnumData status = SavingsEnumerations.status(statusId);
            final Integer accountType = JdbcSupport.getInteger(rs, "accountType");
            final EnumOptionData accountTypeData = AccountEnumerations.loanType(accountType);
            final Integer depositTypeId = JdbcSupport.getInteger(rs, "depositType");
            final EnumOptionData depositTypeData = SavingsEnumerations.depositType(depositTypeId);

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final CurrencyData currency = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf, currencyDisplaySymbol,
                    currencyNameCode);

            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
            final String submittedByUsername = rs.getString("submittedByUsername");
            final String submittedByFirstname = rs.getString("submittedByFirstname");
            final String submittedByLastname = rs.getString("submittedByLastname");

            final LocalDate rejectedOnDate = JdbcSupport.getLocalDate(rs, "rejectedOnDate");
            final String rejectedByUsername = rs.getString("rejectedByUsername");
            final String rejectedByFirstname = rs.getString("rejectedByFirstname");
            final String rejectedByLastname = rs.getString("rejectedByLastname");

            final LocalDate withdrawnOnDate = JdbcSupport.getLocalDate(rs, "withdrawnOnDate");
            final String withdrawnByUsername = rs.getString("withdrawnByUsername");
            final String withdrawnByFirstname = rs.getString("withdrawnByFirstname");
            final String withdrawnByLastname = rs.getString("withdrawnByLastname");

            final LocalDate approvedOnDate = JdbcSupport.getLocalDate(rs, "approvedOnDate");
            final String approvedByUsername = rs.getString("approvedByUsername");
            final String approvedByFirstname = rs.getString("approvedByFirstname");
            final String approvedByLastname = rs.getString("approvedByLastname");

            final LocalDate activatedOnDate = JdbcSupport.getLocalDate(rs, "activatedOnDate");
            final String activatedByUsername = rs.getString("activatedByUsername");
            final String activatedByFirstname = rs.getString("activatedByFirstname");
            final String activatedByLastname = rs.getString("activatedByLastname");

            final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedOnDate");
            final String closedByUsername = rs.getString("closedByUsername");
            final String closedByFirstname = rs.getString("closedByFirstname");
            final String closedByLastname = rs.getString("closedByLastname");
            final Integer subStatusEnum = JdbcSupport.getInteger(rs, "subStatusEnum");
            final SavingsAccountSubStatusEnumData subStatus = SavingsEnumerations.subStatus(subStatusEnum);

            final LocalDate lastActiveTransactionDate = JdbcSupport.getLocalDate(rs, "lastActiveTransactionDate");

            final SavingsAccountApplicationTimelineData timeline = new SavingsAccountApplicationTimelineData(submittedOnDate,
                    submittedByUsername, submittedByFirstname, submittedByLastname, rejectedOnDate, rejectedByUsername, rejectedByFirstname,
                    rejectedByLastname, withdrawnOnDate, withdrawnByUsername, withdrawnByFirstname, withdrawnByLastname, approvedOnDate,
                    approvedByUsername, approvedByFirstname, approvedByLastname, activatedOnDate, activatedByUsername, activatedByFirstname,
                    activatedByLastname, closedOnDate, closedByUsername, closedByFirstname, closedByLastname);

            return new SavingsAccountSummaryData(id, accountNo, externalId, productId, productName, shortProductName, status, currency,
                    accountBalance, accountTypeData, timeline, depositTypeData, subStatus, lastActiveTransactionDate,
                    closedFixedDepositAccountNumber);
        }
    }

    private static final class LoanAccountSummaryDataMapper implements RowMapper<LoanAccountSummaryData> {

        private final DatabaseSpecificSQLGenerator sqlGenerator;

        LoanAccountSummaryDataMapper(DatabaseSpecificSQLGenerator sqlGenerator) {
            this.sqlGenerator = sqlGenerator;
        }

        public String loanAccountSummarySchema() {

            final StringBuilder accountsSummary = new StringBuilder("l.id as id, l.account_no as accountNo, l.external_id as externalId,");
            accountsSummary.append(" l.product_id as productId, lp.name as productName, lp.short_name as shortProductName,")
                    .append(" lp.description as loanProductDescription, l.loan_status_id as statusId, l.loan_type_enum as loanType,")

                    .append(" glim.account_number as parentAccountNumber,")

                    .append("l.principal_disbursed_derived as originalLoan,").append("l.total_outstanding_derived as loanBalance,")
                    .append("l.total_repayment_derived as amountPaid,")

                    .append(" l.loan_product_counter as loanCycle,")

                    .append(" l.submittedon_date as submittedOnDate,")
                    .append(" sbu.username as submittedByUsername, sbu.firstname as submittedByFirstname, sbu.lastname as submittedByLastname,")

                    .append(" l.rejectedon_date as rejectedOnDate,")
                    .append(" rbu.username as rejectedByUsername, rbu.firstname as rejectedByFirstname, rbu.lastname as rejectedByLastname,")

                    .append(" l.withdrawnon_date as withdrawnOnDate,")
                    .append(" wbu.username as withdrawnByUsername, wbu.firstname as withdrawnByFirstname, wbu.lastname as withdrawnByLastname,")

                    .append(" l.approvedon_date as approvedOnDate,")
                    .append(" abu.username as approvedByUsername, abu.firstname as approvedByFirstname, abu.lastname as approvedByLastname,")

                    // Currency
                    .append(" l.currency_code as currencyCode, l.currency_digits as currencyDigits, l.currency_multiplesof as inMultiplesOf, rc.")
                    .append(sqlGenerator.escape("name"))
                    .append(" as currencyName, rc.display_symbol as currencyDisplaySymbol, rc.internationalized_name_code as currencyNameCode, ")

                    // Loan summary
                    .append(" l.expected_disbursedon_date as expectedDisbursementDate, l.disbursedon_date as actualDisbursementDate,")
                    .append(" dbu.username as disbursedByUsername, dbu.firstname as disbursedByFirstname, dbu.lastname as disbursedByLastname,")
                    .append(" l.principal_disbursed_derived as principalDisbursed, l.principal_repaid_derived as principalPaid,")
                    .append(" l.principal_writtenoff_derived as principalWrittenOff,")
                    .append(" l.principal_outstanding_derived as principalOutstanding, l.interest_charged_derived as interestCharged,")
                    .append(" l.interest_repaid_derived as interestPaid, l.interest_waived_derived as interestWaived,")
                    .append(" l.interest_writtenoff_derived as interestWrittenOff, l.interest_outstanding_derived as interestOutstanding,")
                    .append(" l.fee_charges_charged_derived as feeChargesCharged,")
                    .append(" l.total_charges_due_at_disbursement_derived as feeChargesDueAtDisbursementCharged,")
                    .append(" l.fee_charges_repaid_derived as feeChargesPaid, l.fee_charges_waived_derived as feeChargesWaived,")
                    .append(" l.fee_charges_writtenoff_derived as feeChargesWrittenOff,")
                    .append(" l.fee_charges_outstanding_derived as feeChargesOutstanding,")
                    .append(" l.penalty_charges_charged_derived as penaltyChargesCharged,")
                    .append(" l.penalty_charges_repaid_derived as penaltyChargesPaid,")
                    .append(" l.penalty_charges_waived_derived as penaltyChargesWaived,")
                    .append(" l.penalty_charges_writtenoff_derived as penaltyChargesWrittenOff,")
                    .append(" l.penalty_charges_outstanding_derived as penaltyChargesOutstanding,")
                    .append(" l.total_expected_repayment_derived as totalExpectedRepayment,")
                    .append(" l.total_repayment_derived as totalRepayment,")
                    .append(" l.total_expected_costofloan_derived as totalExpectedCostOfLoan,")
                    .append(" l.total_costofloan_derived as totalCostOfLoan,").append(" l.total_waived_derived as totalWaived,")
                    .append(" l.total_writtenoff_derived as totalWrittenOff,").append(" l.writeoff_reason_cv_id as writeoffReasonId,")
                    .append(" codev.code_value as writeoffReason,").append(" l.total_outstanding_derived as totalOutstanding,")
                    .append(" l.total_overpaid_derived as totalOverpaid,")
                    .append(" l.max_outstanding_loan_balance as outstandingLoanBalance,")
                    .append(" la.principal_overdue_derived as principalOverdue,").append(" la.interest_overdue_derived as interestOverdue,")
                    .append(" la.fee_charges_overdue_derived as feeChargesOverdue,")
                    .append(" la.penalty_charges_overdue_derived as penaltyChargesOverdue,")
                    .append(" la.total_overdue_derived as totalOverdue,").append(" l.total_recovered_derived as totalRecovered,")

                    .append(" l.closedon_date as closedOnDate,")
                    .append(" cbu.username as closedByUsername, cbu.firstname as closedByFirstname, cbu.lastname as closedByLastname,")
                    .append(" la.overdue_since_date_derived as overdueSinceDate,")
                    .append(" l.writtenoffon_date as writtenOffOnDate, l.expected_maturedon_date as expectedMaturityDate, ")
                    .append(" ds.loan_decision_state as loanDecisionState , ").append(" l.loan_status_id as lifeCycleStatusId, ")
                    .append(" glim.actual_principal_amount as actualPrincipalAmount ")

                    .append(" from m_loan l ").append("LEFT JOIN m_product_loan AS lp ON lp.id = l.product_id")
                    .append(" left join m_appuser sbu on sbu.id = l.created_by")
                    .append(" left join m_appuser rbu on rbu.id = l.rejectedon_userid")
                    .append(" left join m_appuser wbu on wbu.id = l.withdrawnon_userid")
                    .append(" left join m_appuser abu on abu.id = l.approvedon_userid")
                    .append(" left join m_appuser dbu on dbu.id = l.disbursedon_userid")
                    .append(" left join m_appuser cbu on cbu.id = l.closedon_userid")
                    .append(" left join m_loan_arrears_aging la on la.loan_id = l.id")
                    .append(" left join glim_accounts glim on glim.id=l.glim_id")
                    .append(" left join m_loan_decision as ds on l.id = ds.loan_id")
                    .append(" left join m_code_value codev on codev.id = l.writeoff_reason_cv_id")
                    .append(" join m_currency rc on rc." + sqlGenerator.escape("code") + " = l.currency_code");

            return accountsSummary.toString();
        }

        @Override
        public LoanAccountSummaryData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = JdbcSupport.getLong(rs, "id");
            final String accountNo = rs.getString("accountNo");
            final String parentAccountNumber = rs.getString("parentAccountNumber");
            final String externalId = rs.getString("externalId");
            final Long productId = JdbcSupport.getLong(rs, "productId");
            final String loanProductName = rs.getString("productName");
            final String shortLoanProductName = rs.getString("shortProductName");
            final String loanProductDescription = rs.getString("loanProductDescription");
            final Integer loanStatusId = JdbcSupport.getInteger(rs, "statusId");
            final LoanStatusEnumData loanStatus = LoanEnumerations.status(loanStatusId);
            final Integer loanTypeId = JdbcSupport.getInteger(rs, "loanType");
            final EnumOptionData loanType = AccountEnumerations.loanType(loanTypeId);
            final Integer loanCycle = JdbcSupport.getInteger(rs, "loanCycle");

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final CurrencyData currencyData = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf,
                    currencyDisplaySymbol, currencyNameCode);

            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
            final String submittedByUsername = rs.getString("submittedByUsername");
            final String submittedByFirstname = rs.getString("submittedByFirstname");
            final String submittedByLastname = rs.getString("submittedByLastname");

            final LocalDate rejectedOnDate = JdbcSupport.getLocalDate(rs, "rejectedOnDate");
            final String rejectedByUsername = rs.getString("rejectedByUsername");
            final String rejectedByFirstname = rs.getString("rejectedByFirstname");
            final String rejectedByLastname = rs.getString("rejectedByLastname");

            final LocalDate withdrawnOnDate = JdbcSupport.getLocalDate(rs, "withdrawnOnDate");
            final String withdrawnByUsername = rs.getString("withdrawnByUsername");
            final String withdrawnByFirstname = rs.getString("withdrawnByFirstname");
            final String withdrawnByLastname = rs.getString("withdrawnByLastname");

            final LocalDate approvedOnDate = JdbcSupport.getLocalDate(rs, "approvedOnDate");
            final String approvedByUsername = rs.getString("approvedByUsername");
            final String approvedByFirstname = rs.getString("approvedByFirstname");
            final String approvedByLastname = rs.getString("approvedByLastname");

            final LocalDate expectedDisbursementDate = JdbcSupport.getLocalDate(rs, "expectedDisbursementDate");
            final LocalDate actualDisbursementDate = JdbcSupport.getLocalDate(rs, "actualDisbursementDate");
            final String disbursedByUsername = rs.getString("disbursedByUsername");
            final String disbursedByFirstname = rs.getString("disbursedByFirstname");
            final String disbursedByLastname = rs.getString("disbursedByLastname");

            final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedOnDate");
            final String closedByUsername = rs.getString("closedByUsername");
            final String closedByFirstname = rs.getString("closedByFirstname");
            final String closedByLastname = rs.getString("closedByLastname");

            final BigDecimal originalLoan = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "originalLoan");
            final BigDecimal loanBalance = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "loanBalance");
            final BigDecimal amountPaid = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "amountPaid");
            final BigDecimal actualPrincipalAmount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "actualPrincipalAmount");

            final LocalDate writtenOffOnDate = JdbcSupport.getLocalDate(rs, "writtenOffOnDate");

            final LocalDate expectedMaturityDate = JdbcSupport.getLocalDate(rs, "expectedMaturityDate");

            final LocalDate overdueSinceDate = JdbcSupport.getLocalDate(rs, "overdueSinceDate");
            Boolean inArrears = true;
            if (overdueSinceDate == null) {
                inArrears = false;
            }

            final BigDecimal feeChargesDueAtDisbursementCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs,
                    "feeChargesDueAtDisbursementCharged");
            final Integer lifeCycleStatusId = JdbcSupport.getInteger(rs, "lifeCycleStatusId");
            final LoanStatusEnumData status = LoanEnumerations.status(lifeCycleStatusId);
            LoanSummaryData loanSummary = null;
            if (status.id().intValue() >= 300) {

                // loan summary
                final BigDecimal principalDisbursed = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalDisbursed");
                final BigDecimal principalPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalPaid");
                final BigDecimal principalWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalWrittenOff");
                final BigDecimal principalOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalOutstanding");
                final BigDecimal principalOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalOverdue");

                final BigDecimal interestCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestCharged");
                final BigDecimal interestPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestPaid");
                final BigDecimal interestWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWaived");
                final BigDecimal interestWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWrittenOff");
                final BigDecimal interestOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestOutstanding");
                final BigDecimal interestOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestOverdue");

                final BigDecimal feeChargesCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesCharged");
                final BigDecimal feeChargesPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesPaid");
                final BigDecimal feeChargesWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesWaived");
                final BigDecimal feeChargesWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesWrittenOff");
                final BigDecimal feeChargesOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesOutstanding");
                final BigDecimal feeChargesOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeChargesOverdue");

                final BigDecimal penaltyChargesCharged = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesCharged");
                final BigDecimal penaltyChargesPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesPaid");
                final BigDecimal penaltyChargesWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesWaived");
                final BigDecimal penaltyChargesWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesWrittenOff");
                final BigDecimal penaltyChargesOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesOutstanding");
                final BigDecimal penaltyChargesOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyChargesOverdue");

                final BigDecimal totalExpectedRepayment = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalExpectedRepayment");
                final BigDecimal totalRepayment = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalRepayment");
                final BigDecimal totalExpectedCostOfLoan = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalExpectedCostOfLoan");
                final BigDecimal totalCostOfLoan = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalCostOfLoan");
                final BigDecimal totalWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalWaived");
                final BigDecimal totalWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalWrittenOff");
                final BigDecimal totalOutstanding = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalOutstanding");
                final BigDecimal totalOverdue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalOverdue");
                final BigDecimal totalRecovered = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalRecovered");
                final Long writeOffReasonId = JdbcSupport.getLong(rs, "writeOffReasonId");
                final String writeOffReason = rs.getString("writeOffReason");

                loanSummary = new LoanSummaryData(currencyData, principalDisbursed, principalPaid, principalWrittenOff,
                        principalOutstanding, principalOverdue, interestCharged, interestPaid, interestWaived, interestWrittenOff,
                        interestOutstanding, interestOverdue, feeChargesCharged, feeChargesDueAtDisbursementCharged, feeChargesPaid,
                        feeChargesWaived, feeChargesWrittenOff, feeChargesOutstanding, feeChargesOverdue, penaltyChargesCharged,
                        penaltyChargesPaid, penaltyChargesWaived, penaltyChargesWrittenOff, penaltyChargesOutstanding,
                        penaltyChargesOverdue, totalExpectedRepayment, totalRepayment, totalExpectedCostOfLoan, totalCostOfLoan,
                        totalWaived, totalWrittenOff, totalOutstanding, totalOverdue, overdueSinceDate, writeOffReasonId, writeOffReason,
                        totalRecovered);
            }

            final Long loanDecisionStateId = JdbcSupport.getLong(rs, "loanDecisionState");
            EnumOptionData loanDecisionStateEnumData = null;
            if (loanDecisionStateId != null) {
                loanDecisionStateEnumData = LoanEnumerations.loanDecisionState(loanDecisionStateId.intValue());
            }

            final LoanApplicationTimelineData timeline = new LoanApplicationTimelineData(submittedOnDate, submittedByUsername,
                    submittedByFirstname, submittedByLastname, rejectedOnDate, rejectedByUsername, rejectedByFirstname, rejectedByLastname,
                    withdrawnOnDate, withdrawnByUsername, withdrawnByFirstname, withdrawnByLastname, approvedOnDate, approvedByUsername,
                    approvedByFirstname, approvedByLastname, expectedDisbursementDate, actualDisbursementDate, disbursedByUsername,
                    disbursedByFirstname, disbursedByLastname, closedOnDate, closedByUsername, closedByFirstname, closedByLastname,
                    expectedMaturityDate, writtenOffOnDate, closedByUsername, closedByFirstname, closedByLastname);

            LoanAccountSummaryData loanAccountSummaryData = new LoanAccountSummaryData(id, accountNo, parentAccountNumber, externalId,
                    productId, loanProductName, shortLoanProductName, loanStatus, loanType, loanCycle, timeline, inArrears, originalLoan,
                    loanBalance, amountPaid, loanDecisionStateEnumData, actualPrincipalAmount);
            loanAccountSummaryData.setLoanProductDescription(loanProductDescription);
            loanAccountSummaryData.setSummary(loanSummary);
            return loanAccountSummaryData;
        }

    }

    private static final class GuarantorLoanAccountSummaryDataMapper implements RowMapper<GuarantorAccountSummaryData> {

        public String guarantorLoanAccountSummarySchema() {

            final StringBuilder accountsSummary = new StringBuilder("l.id as id, l.account_no as accountNo, l.external_id as externalId,");
            accountsSummary.append(" l.product_id as productId, lp.name as productName, lp.short_name as shortProductName,")
                    .append(" l.loan_status_id as statusId, l.loan_type_enum as loanType,")

                    .append("l.principal_disbursed_derived as originalLoan,").append("l.total_outstanding_derived as loanBalance,")
                    .append("l.total_repayment_derived as amountPaid,")

                    .append(" l.loan_product_counter as loanCycle,")

                    .append(" l.submittedon_date as submittedOnDate,")

                    .append(" l.rejectedon_date as rejectedOnDate,").append(" l.withdrawnon_date as withdrawnOnDate,")
                    .append(" l.approvedon_date as approvedOnDate,")
                    .append(" l.expected_disbursedon_date as expectedDisbursementDate, l.disbursedon_date as actualDisbursementDate,")
                    .append(" l.closedon_date as closedOnDate,").append(" la.overdue_since_date_derived as overdueSinceDate,")
                    .append(" l.writtenoffon_date as writtenOffOnDate, l.expected_maturedon_date as expectedMaturityDate,")
                    .append(" g.is_active as isActive,").append(" cv.code_value as relationship,").append(" sa.on_hold_funds_derived")
                    .append(" from m_loan l ").append(" join m_guarantor as g on g.loan_id = l.id ")
                    .append(" join m_client as c on c.id = g.entity_id ").append(" LEFT JOIN m_product_loan AS lp ON lp.id = l.product_id")
                    .append(" left join m_loan_arrears_aging la on la.loan_id = l.id")
                    .append(" left join m_code_value cv ON cv.id = g.client_reln_cv_id")
                    .append(" left join m_savings_account sa on sa.client_id = c.id");

            return accountsSummary.toString();
        }

        @Override
        public GuarantorAccountSummaryData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = JdbcSupport.getLong(rs, "id");
            final String accountNo = rs.getString("accountNo");
            final String externalId = rs.getString("externalId");
            final Long productId = JdbcSupport.getLong(rs, "productId");
            final String loanProductName = rs.getString("productName");
            final String shortLoanProductName = rs.getString("shortProductName");
            final Integer loanStatusId = JdbcSupport.getInteger(rs, "statusId");
            final LoanStatusEnumData loanStatus = LoanEnumerations.status(loanStatusId);
            final Integer loanTypeId = JdbcSupport.getInteger(rs, "loanType");
            final EnumOptionData loanType = AccountEnumerations.loanType(loanTypeId);
            final Integer loanCycle = JdbcSupport.getInteger(rs, "loanCycle");

            final BigDecimal originalLoan = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "originalLoan");
            final BigDecimal loanBalance = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "loanBalance");
            final BigDecimal amountPaid = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "amountPaid");
            final BigDecimal onHoldAmount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "on_hold_funds_derived");

            final LocalDate overdueSinceDate = JdbcSupport.getLocalDate(rs, "overdueSinceDate");
            Boolean inArrears = true;
            if (overdueSinceDate == null) {
                inArrears = false;
            }

            final Boolean isActive = rs.getBoolean("isActive");

            final String relationship = rs.getString("relationship");
            return new GuarantorAccountSummaryData(id, accountNo, externalId, productId, loanProductName, shortLoanProductName, loanStatus,
                    loanType, loanCycle, inArrears, originalLoan, loanBalance, amountPaid, isActive, relationship, onHoldAmount);
        }

    }

    private List<String> getRequiredAccountFilters(final Integer expectedParameters, String fields) {

        if (StringUtils.isEmpty(fields)) {
            throw new PlatformDataIntegrityException("error.msg.account.data.integrity.issue",
                    "No fields specified to populate the account resource.");
        }

        final List<String> fieldList = Splitter.on(',').splitToList(fields);
        if (fieldList.size() > expectedParameters) {
            throw new IllegalArgumentException(
                    "Invalid filter option, a maximum of " + expectedParameters + " filter options supported: " + fields);
        }

        return fieldList;
    }

}
