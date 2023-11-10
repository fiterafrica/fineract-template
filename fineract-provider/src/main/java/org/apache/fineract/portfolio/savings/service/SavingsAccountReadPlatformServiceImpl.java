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
package org.apache.fineract.portfolio.savings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.filters.FilterConstraint;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.dataqueries.data.DatatableData;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksReadService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.organisation.staff.service.StaffReadPlatformService;
import org.apache.fineract.portfolio.account.data.AccountTransferData;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.group.service.GroupReadPlatformService;
import org.apache.fineract.portfolio.paymentdetail.data.PaymentDetailData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.SavingsCompoundingInterestPeriodType;
import org.apache.fineract.portfolio.savings.SavingsInterestCalculationDaysInYearType;
import org.apache.fineract.portfolio.savings.SavingsInterestCalculationType;
import org.apache.fineract.portfolio.savings.SavingsPeriodFrequencyType;
import org.apache.fineract.portfolio.savings.SavingsPostingInterestPeriodType;
import org.apache.fineract.portfolio.savings.WithdrawalFrequency;
import org.apache.fineract.portfolio.savings.data.RecurringMissedTargetData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountApplicationTimelineData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountBlockNarrationHistoryData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountChargeData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountFloatingInterestRateData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountStatusEnumData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountSubStatusEnumData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountSummaryData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionEnumData;
import org.apache.fineract.portfolio.savings.data.SavingsProductData;
import org.apache.fineract.portfolio.savings.data.SavingsProductFloatingInterestRateData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountChargesPaidByData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountSubStatusEnum;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountNotFoundException;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountSearchParameterNotProvidedException;
import org.apache.fineract.portfolio.savings.request.FilterSelection;
import org.apache.fineract.portfolio.search.service.SearchReadPlatformService;
import org.apache.fineract.portfolio.tax.data.TaxComponentData;
import org.apache.fineract.portfolio.tax.data.TaxDetailsData;
import org.apache.fineract.portfolio.tax.data.TaxGroupData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class SavingsAccountReadPlatformServiceImpl implements SavingsAccountReadPlatformService {

    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;
    private final ClientReadPlatformService clientReadPlatformService;
    private final GroupReadPlatformService groupReadPlatformService;
    private final SavingsProductReadPlatformService savingsProductReadPlatformService;
    private final StaffReadPlatformService staffReadPlatformService;
    private final SavingsDropdownReadPlatformService dropdownReadPlatformService;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final ChargeReadPlatformService chargeReadPlatformService;

    // mappers
    private final SavingsAccountTransactionTemplateMapper transactionTemplateMapper;
    private final RecurringMissedTargetTemplateMapper recurringMissedTargetTemplateMapper;
    private final SavingsAccountTransactionsMapper transactionsMapper;
    private final SavingsAccountTransactionsForBatchMapper savingsAccountTransactionsForBatchMapper;
    private final SavingAccountMapper savingAccountMapper;
    private final SavingAccountMapperForInterestPosting savingAccountMapperForInterestPosting;

    // pagination
    private final PaginationHelper paginationHelper;

    private final EntityDatatableChecksReadService entityDatatableChecksReadService;
    private final ColumnValidator columnValidator;
    private final SavingsAccountAssembler savingAccountAssembler;
    private final CodeValueReadPlatformService codeValueReadPlatformService;

    private final SavingsAccountBlockNarrationHistoryMapper savingsAccountBlockNarrationHistoryMapper;
    private final SavingsProductFloatingInterestRateReadPlatformService savingsProductFloatingInterestRateReadPlatformService;

    private final SearchReadPlatformService searchReadPlatformService;

    @Autowired
    public SavingsAccountReadPlatformServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate,
            final ClientReadPlatformService clientReadPlatformService, final GroupReadPlatformService groupReadPlatformService,
            final SavingsProductReadPlatformService savingProductReadPlatformService,
            final StaffReadPlatformService staffReadPlatformService, final SavingsDropdownReadPlatformService dropdownReadPlatformService,
            final ChargeReadPlatformService chargeReadPlatformService,
            final EntityDatatableChecksReadService entityDatatableChecksReadService, final ColumnValidator columnValidator,
            final SavingsAccountAssembler savingAccountAssembler, PaginationHelper paginationHelper,
            DatabaseSpecificSQLGenerator sqlGenerator, final CodeValueReadPlatformService codeValueReadPlatformService,
            final SavingsProductFloatingInterestRateReadPlatformService savingsProductFloatingInterestRateReadPlatformService,
            SearchReadPlatformService searchReadPlatformService) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
        this.clientReadPlatformService = clientReadPlatformService;
        this.groupReadPlatformService = groupReadPlatformService;
        this.savingsProductReadPlatformService = savingProductReadPlatformService;
        this.staffReadPlatformService = staffReadPlatformService;
        this.dropdownReadPlatformService = dropdownReadPlatformService;
        this.sqlGenerator = sqlGenerator;
        this.searchReadPlatformService = searchReadPlatformService;
        this.transactionTemplateMapper = new SavingsAccountTransactionTemplateMapper();
        this.transactionsMapper = new SavingsAccountTransactionsMapper();
        this.savingsAccountTransactionsForBatchMapper = new SavingsAccountTransactionsForBatchMapper();
        this.savingAccountMapper = new SavingAccountMapper();
        this.chargeReadPlatformService = chargeReadPlatformService;
        this.entityDatatableChecksReadService = entityDatatableChecksReadService;
        this.columnValidator = columnValidator;
        this.paginationHelper = paginationHelper;
        this.savingAccountMapperForInterestPosting = new SavingAccountMapperForInterestPosting();
        this.savingAccountAssembler = savingAccountAssembler;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.savingsAccountBlockNarrationHistoryMapper = new SavingsAccountBlockNarrationHistoryMapper();
        this.savingsProductFloatingInterestRateReadPlatformService = savingsProductFloatingInterestRateReadPlatformService;
        this.recurringMissedTargetTemplateMapper = new RecurringMissedTargetTemplateMapper();

    }

    @Override
    public Collection<SavingsAccountData> retrieveAllForLookup(final Long clientId) {

        final StringBuilder sqlBuilder = new StringBuilder("select " + this.savingAccountMapper.schema());
        sqlBuilder.append(" where sa.client_id = ? and sa.status_enum = 300 ");

        final Object[] queryParameters = new Object[] { clientId };
        return this.jdbcTemplate.query(sqlBuilder.toString(), this.savingAccountMapper, queryParameters);
    }

    @Override
    public Collection<SavingsAccountData> retrieveActiveForLookup(final Long clientId, DepositAccountType depositAccountType) {

        final StringBuilder sqlBuilder = new StringBuilder("select " + this.savingAccountMapper.schema());
        sqlBuilder.append(" where sa.client_id = ? and sa.status_enum = 300 and sa.deposit_type_enum = ? ");

        final Object[] queryParameters = new Object[] { clientId, depositAccountType.getValue() };
        return this.jdbcTemplate.query(sqlBuilder.toString(), this.savingAccountMapper, queryParameters);
    }

    @Override
    public Collection<SavingsAccountData> retrieveActiveForLookup(final Long clientId, DepositAccountType depositAccountType,
            String currencyCode) {
        final StringBuilder sqlBuilder = new StringBuilder("select " + this.savingAccountMapper.schema());
        sqlBuilder.append(" where sa.client_id = ? and sa.status_enum = 300 and sa.deposit_type_enum = ? and sa.currency_code = ? ");

        final Object[] queryParameters = new Object[] { clientId, depositAccountType.getValue(), currencyCode };
        return this.jdbcTemplate.query(sqlBuilder.toString(), this.savingAccountMapper, queryParameters);
    }

    @Override
    public Page<SavingsAccountData> retrieveAll(final SearchParameters searchParameters) {

        final AppUser currentUser = this.context.authenticatedUser();
        final String hierarchy = currentUser.getOffice().getHierarchy();
        final String hierarchySearchString = hierarchy + "%";

        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select " + sqlGenerator.calcFoundRows() + " ");
        sqlBuilder.append(this.savingAccountMapper.schema());

        sqlBuilder.append(" join m_office o on (o.id = c.office_id or o.id = g.office_id)");
        sqlBuilder.append(" where o.hierarchy like ?");

        final Object[] objectArray = new Object[2];
        objectArray[0] = hierarchySearchString;
        int arrayPos = 1;
        if (searchParameters != null) {
            String sqlQueryCriteria = searchParameters.getSqlSearch();
            if (StringUtils.isNotBlank(sqlQueryCriteria)) {
                sqlQueryCriteria = sqlQueryCriteria.replaceAll("accountNo", "sa.account_no");
                this.columnValidator.validateSqlInjection(sqlBuilder.toString(), sqlQueryCriteria);
                sqlBuilder.append(" and (").append(sqlQueryCriteria).append(")");
            }

            if (StringUtils.isNotBlank(searchParameters.getExternalId())) {
                sqlBuilder.append(" and sa.external_id = ?");
                objectArray[arrayPos] = searchParameters.getExternalId();
                arrayPos = arrayPos + 1;
            }
            if (StringUtils.isNotBlank(searchParameters.getAccountNo())) {
                sqlBuilder.append(" and sa.account_no = ?");
                objectArray[arrayPos] = searchParameters.getAccountNo();
                arrayPos = arrayPos + 1;
            }
            if (searchParameters.getOfficeId() != null) {
                sqlBuilder.append("and c.office_id =?");
                objectArray[arrayPos] = searchParameters.getOfficeId();
                arrayPos = arrayPos + 1;
            }
            if (searchParameters.isOrderByRequested()) {
                sqlBuilder.append(" order by ").append(searchParameters.getOrderBy());
                this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getOrderBy());

                if (searchParameters.isSortOrderProvided()) {
                    sqlBuilder.append(' ').append(searchParameters.getSortOrder());
                    this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getSortOrder());
                }
            }

            if (searchParameters.isLimited()) {
                sqlBuilder.append(" ");
                if (searchParameters.isOffset()) {
                    sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit(), searchParameters.getOffset()));
                } else {
                    sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit()));
                }
            }
        }
        final Object[] finalObjectArray = Arrays.copyOf(objectArray, arrayPos);
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), finalObjectArray, this.savingAccountMapper);
    }

    @Override
    public SavingsAccountData retrieveOne(final Long accountId) {

        try {
            final String sql = "select " + this.savingAccountMapper.schema() + " where sa.id = ?";

            return this.jdbcTemplate.queryForObject(sql, this.savingAccountMapper, new Object[] { accountId }); // NOSONAR
        } catch (final EmptyResultDataAccessException e) {
            throw new SavingsAccountNotFoundException(accountId, e);
        }
    }

    @Override
    public List<SavingsAccountTransactionData> retrieveAllTransactionData(final List<String> refNo) throws DataAccessException {
        String inSql = String.join(",", Collections.nCopies(refNo.size(), "?"));
        String sql = "select " + this.savingsAccountTransactionsForBatchMapper.schema() + " where tr.ref_no in (%s)";
        Object[] params = new Object[refNo.size()];
        int i = 0;
        for (String element : refNo) {
            params[i] = element;
            i++;
        }
        return this.jdbcTemplate.query(String.format(sql, inSql), this.savingsAccountTransactionsForBatchMapper, params);
    }

    @Override
    public List<SavingsAccountData> retrieveAllSavingsDataForInterestPosting(final boolean backdatedTxnsAllowedTill, final int pageSize,
            final Integer status, final Long maxSavingsId) {
        LocalDate yesterday = DateUtils.getBusinessLocalDate().minusDays(1);
        String sql = "select " + this.savingAccountMapperForInterestPosting.schema()
                + "join (select a.id from m_savings_account a where a.id > ? and a.status_enum = ? limit ?) b on b.id = sa.id ";
        if (backdatedTxnsAllowedTill) {
            sql = sql
                    + "where (CASE WHEN sa.interest_posted_till_date is not null THEN tr.transaction_date >= sa.interest_posted_till_date ELSE tr.transaction_date >= sa.activatedon_date END) ";
        }

        sql = sql + " and (sa.interest_posted_till_date is null or sa.interest_posted_till_date <= ? ) ";
        sql = sql + " order by sa.id, tr.transaction_date, tr.created_date, tr.id";

        List<SavingsAccountData> savingsAccountDataList = this.jdbcTemplate.query(sql, this.savingAccountMapperForInterestPosting, // NOSONAR
                new Object[] { maxSavingsId, status, pageSize, yesterday });
        for (SavingsAccountData savingsAccountData : savingsAccountDataList) {
            this.savingAccountAssembler.assembleSavings(savingsAccountData);
        }
        return savingsAccountDataList;
    }

    @Override
    public SavingsAccountData retrieveTemplate(final Long clientId, final Long groupId, final Long productId,
            final boolean staffInSelectedOfficeOnly) {

        final AppUser loggedInUser = this.context.authenticatedUser();
        Long officeId = loggedInUser.getOffice().getId();

        ClientData client = null;
        if (clientId != null) {
            client = this.clientReadPlatformService.retrieveOne(clientId);
            officeId = client.officeId();
        }

        GroupGeneralData group = null;
        if (groupId != null) {
            group = this.groupReadPlatformService.retrieveOne(groupId);
            officeId = group.officeId();
        }

        final Collection<SavingsProductData> productOptions = this.savingsProductReadPlatformService.retrieveAllForLookup();
        SavingsAccountData template = null;
        if (productId != null) {

            final SavingAccountTemplateMapper mapper = new SavingAccountTemplateMapper(client, group);

            final String sql = "select " + mapper.schema() + " where sp.id = ?";
            template = this.jdbcTemplate.queryForObject(sql, mapper, new Object[] { productId }); // NOSONAR
            boolean useFloatingInterestRateFromProduct = template.getUseFloatingInterestRate();

            final Collection<EnumOptionData> interestCompoundingPeriodTypeOptions = this.dropdownReadPlatformService
                    .retrieveCompoundingInterestPeriodTypeOptions();

            final Collection<EnumOptionData> interestPostingPeriodTypeOptions = this.dropdownReadPlatformService
                    .retrieveInterestPostingPeriodTypeOptions();

            final Collection<EnumOptionData> interestCalculationTypeOptions = this.dropdownReadPlatformService
                    .retrieveInterestCalculationTypeOptions();

            final Collection<EnumOptionData> interestCalculationDaysInYearTypeOptions = this.dropdownReadPlatformService
                    .retrieveInterestCalculationDaysInYearTypeOptions();

            final Collection<EnumOptionData> lockinPeriodFrequencyTypeOptions = this.dropdownReadPlatformService
                    .retrieveLockinPeriodFrequencyTypeOptions();

            final Collection<EnumOptionData> withdrawalFeeTypeOptions = this.dropdownReadPlatformService.retrievewithdrawalFeeTypeOptions();
            final Collection<EnumOptionData> withdrawalFrequencyOptions = this.dropdownReadPlatformService
                    .retrieveWithdrawalFrequencyOptions();

            final Collection<SavingsAccountTransactionData> transactions = null;
            final Collection<ChargeData> productCharges = this.chargeReadPlatformService.retrieveSavingsProductCharges(productId);
            // update charges from Product charges
            final Collection<SavingsAccountChargeData> charges = fromChargesToSavingsCharges(productCharges);

            // retrieve floating interest rates from saving product
            final Collection<SavingsProductFloatingInterestRateData> productFloatingInterestRates = this.savingsProductFloatingInterestRateReadPlatformService
                    .getSavingsProductFloatingInterestRateForSavingsProduct(productId);
            final Collection<SavingsAccountFloatingInterestRateData> accountFloatingInterestRates = fromSavingsProductFloatingInterestRatesToSavingsAccountFloatingInterestRates(
                    productFloatingInterestRates);

            final boolean feeChargesOnly = false;
            final Collection<ChargeData> chargeOptions = this.chargeReadPlatformService
                    .retrieveSavingsProductApplicableCharges(feeChargesOnly);

            Collection<StaffData> fieldOfficerOptions = null;

            if (officeId != null) {

                if (staffInSelectedOfficeOnly) {
                    // only bring back loan officers in selected branch/office
                    final Collection<StaffData> fieldOfficersInBranch = this.staffReadPlatformService
                            .retrieveAllLoanOfficersInOfficeById(officeId);

                    if (!CollectionUtils.isEmpty(fieldOfficersInBranch)) {
                        fieldOfficerOptions = new ArrayList<>(fieldOfficersInBranch);
                    }
                } else {
                    // by default bring back all officers in selected
                    // branch/office as well as officers in office above
                    // this office
                    final boolean restrictToLoanOfficersOnly = true;
                    final Collection<StaffData> loanOfficersInHierarchy = this.staffReadPlatformService
                            .retrieveAllStaffInOfficeAndItsParentOfficeHierarchy(officeId, restrictToLoanOfficersOnly);

                    if (!CollectionUtils.isEmpty(loanOfficersInHierarchy)) {
                        fieldOfficerOptions = new ArrayList<>(loanOfficersInHierarchy);
                    }
                }
            }
            boolean postOverdraftInterestOnDeposit = template.isPostOverdraftInterestOnDeposit();
            template = SavingsAccountData.withTemplateOptions(template, productOptions, fieldOfficerOptions,
                    interestCompoundingPeriodTypeOptions, interestPostingPeriodTypeOptions, interestCalculationTypeOptions,
                    interestCalculationDaysInYearTypeOptions, lockinPeriodFrequencyTypeOptions, withdrawalFeeTypeOptions, transactions,
                    charges, chargeOptions, null, null);
            template.setFloatingInterestRates(accountFloatingInterestRates);
            template.setUseFloatingInterestRate(useFloatingInterestRateFromProduct);
            template.setWithdrawalFrequencyOptions(withdrawalFrequencyOptions);
            template.setPostOverdraftInterestOnDeposit(postOverdraftInterestOnDeposit);
        } else {

            String clientName = null;
            if (client != null) {
                clientName = client.displayName();
            }

            String groupName = null;
            if (group != null) {
                groupName = group.getName();
            }

            template = SavingsAccountData.withClientTemplate(clientId, clientName, groupId, groupName);

            final Collection<StaffData> fieldOfficerOptions = null;
            final Collection<EnumOptionData> interestCompoundingPeriodTypeOptions = null;
            final Collection<EnumOptionData> interestPostingPeriodTypeOptions = null;
            final Collection<EnumOptionData> interestCalculationTypeOptions = null;
            final Collection<EnumOptionData> interestCalculationDaysInYearTypeOptions = null;
            final Collection<EnumOptionData> lockinPeriodFrequencyTypeOptions = null;
            final Collection<EnumOptionData> withdrawalFeeTypeOptions = null;

            final Collection<SavingsAccountTransactionData> transactions = null;
            final Collection<SavingsAccountChargeData> charges = null;

            final boolean feeChargesOnly = false;
            final Collection<ChargeData> chargeOptions = this.chargeReadPlatformService
                    .retrieveSavingsProductApplicableCharges(feeChargesOnly);

            template = SavingsAccountData.withTemplateOptions(template, productOptions, fieldOfficerOptions,
                    interestCompoundingPeriodTypeOptions, interestPostingPeriodTypeOptions, interestCalculationTypeOptions,
                    interestCalculationDaysInYearTypeOptions, lockinPeriodFrequencyTypeOptions, withdrawalFeeTypeOptions, transactions,
                    charges, chargeOptions, null, null);
        }

        final List<DatatableData> datatableTemplates = this.entityDatatableChecksReadService
                .retrieveTemplates(StatusEnum.CREATE.getCode().longValue(), EntityTables.SAVING.getName(), productId);
        template.setDatatables(datatableTemplates);

        return template;
    }

    private Collection<SavingsAccountChargeData> fromChargesToSavingsCharges(final Collection<ChargeData> productCharges) {
        final Collection<SavingsAccountChargeData> savingsCharges = new ArrayList<>();
        for (final ChargeData chargeData : productCharges) {
            final SavingsAccountChargeData savingsCharge = chargeData.toSavingsAccountChargeData();
            savingsCharges.add(savingsCharge);
        }
        return savingsCharges;
    }

    private Collection<SavingsAccountFloatingInterestRateData> fromSavingsProductFloatingInterestRatesToSavingsAccountFloatingInterestRates(
            final Collection<SavingsProductFloatingInterestRateData> productFloatingInterestRates) {
        final Collection<SavingsAccountFloatingInterestRateData> savingsAccountFloatingInterestRates = new ArrayList<>();
        for (final SavingsProductFloatingInterestRateData productData : productFloatingInterestRates) {
            final SavingsAccountFloatingInterestRateData savingsAccountFloatingInterestRateData = productData
                    .toSavingsAccountFloatingInterestRateData();
            savingsAccountFloatingInterestRates.add(savingsAccountFloatingInterestRateData);
        }
        return savingsAccountFloatingInterestRates;
    }

    @Override
    public SavingsAccountTransactionData retrieveDepositTransactionTemplate(final Long savingsId,
            final DepositAccountType depositAccountType) {

        try {
            final String sql = "select " + this.transactionTemplateMapper.schema() + " where sa.id = ? and sa.deposit_type_enum = ?";

            return this.jdbcTemplate.queryForObject(sql, this.transactionTemplateMapper, // NOSONAR
                    new Object[] { savingsId, depositAccountType.getValue() });
        } catch (final EmptyResultDataAccessException e) {
            throw new SavingsAccountNotFoundException(savingsId, e);
        }
    }

    @Override
    public Collection<SavingsAccountTransactionData> retrieveAllTransactions(final Long savingsId, Integer offset, Integer limit) {
        if (offset == null) {
            offset = 0;
        }
        if (limit == null) {
            limit = 15;
        }
        final String sql = "select " + this.transactionsMapper.schema()
                + " where sa.id = ? AND transaction_type_enum not in (22,25)  order by tr.transaction_date DESC, tr.created_date DESC, tr.id DESC LIMIT ? OFFSET ?  ";

        return this.jdbcTemplate.query(sql, this.transactionsMapper, new Object[] { savingsId, limit, offset }); // NOSONAR
    }

    @Override
    public Collection<SavingsAccountTransactionData> retrieveAccrualTransactions(final Long savingsId,
            DepositAccountType depositAccountType, Integer offset, Integer limit) {
        if (offset == null) {
            offset = 0;
        }
        if (limit == null) {
            limit = 15;
        }

        final String sql = "select " + this.transactionsMapper.schema()
                + " where sa.id = ? and sa.deposit_type_enum = ? AND transaction_type_enum in (22,25)  order by tr.transaction_date DESC, tr.created_date DESC, tr.id DESC LIMIT ? OFFSET ?   ";

        return this.jdbcTemplate.query(sql, this.transactionsMapper,
                new Object[] { savingsId, depositAccountType.getValue(), limit, offset });
    }

    @Override
    public SavingsAccountTransactionData retrieveSavingsTransaction(final Long savingsId, final Long transactionId,
            DepositAccountType depositAccountType) {

        final String sql = "select " + this.transactionsMapper.schema() + " where sa.id = ? and sa.deposit_type_enum = ? and tr.id= ?";

        return this.jdbcTemplate.queryForObject(sql, this.transactionsMapper, // NOSONAR
                new Object[] { savingsId, depositAccountType.getValue(), transactionId });
    }

    @Override
    public Collection<SavingsAccountData> retrieveForLookup(Long clientId, Boolean overdraft) {

        SavingAccountMapperForLookup accountMapperForLookup = new SavingAccountMapperForLookup();
        final StringBuilder sqlBuilder = new StringBuilder("select " + accountMapperForLookup.schema());
        sqlBuilder.append(" where sa.client_id = ? and sa.status_enum = 300");
        Object[] queryParameters = null;
        if (overdraft == null) {
            queryParameters = new Object[] { clientId };
        } else {
            sqlBuilder.append(" and sa.allow_overdraft = ?");
            queryParameters = new Object[] { clientId, overdraft };
        }
        return this.jdbcTemplate.query(sqlBuilder.toString(), accountMapperForLookup, queryParameters);

    }

    @Override
    public List<Long> retrieveSavingsIdsPendingInactive(LocalDate tenantLocalDate) {
        List<Long> ret = null;
        StringBuilder sql = new StringBuilder("select sa.id ");
        sql.append(" from m_savings_account as sa ");
        sql.append(" inner join m_savings_product as sp on (sa.product_id = sp.id and sp.is_dormancy_tracking_active = true) ");
        sql.append(" where sa.status_enum = 300 ");
        sql.append(" and sa.sub_status_enum = 0 ");
        String compareDate = "(select COALESCE(max(sat.transaction_date), sa.activatedon_date) "
                + "from m_savings_account_transaction as sat where sat.is_reversed = false and sat.is_reversal = false"
                + " and sat.transaction_type_enum in (1,2) and sat.savings_account_id = sa.id)";
        sql.append(" and ").append(sqlGenerator.dateDiff("?", compareDate)).append(" >= sp.days_to_inactive ");

        try {
            ret = this.jdbcTemplate.queryForList(sql.toString(), new Object[] { tenantLocalDate }, Long.class);
        } catch (EmptyResultDataAccessException e) {
            // ignore empty result scenario
        } catch (DataAccessException e) {
            throw e;
        }

        return ret;
    }

    @Override
    public List<Long> retrieveSavingsIdsPendingDormant(LocalDate tenantLocalDate) {
        List<Long> ret = null;
        StringBuilder sql = new StringBuilder("select sa.id ");
        sql.append(" from m_savings_account as sa ");
        sql.append(" inner join m_savings_product as sp on (sa.product_id = sp.id and sp.is_dormancy_tracking_active = true) ");
        sql.append(" where sa.status_enum = 300 ");
        sql.append(" and sa.sub_status_enum = 100 ");
        sql.append(" and " + sqlGenerator.dateDiff("?",
                "(select COALESCE(max(sat.transaction_date),sa.activatedon_date) from m_savings_account_transaction as sat where sat.is_reversed = false and sat.is_reversal = false and sat.transaction_type_enum in (1,2) and sat.savings_account_id = sa.id)")
                + " ");
        sql.append(" >= sp.days_to_dormancy ");

        try {
            ret = this.jdbcTemplate.queryForList(sql.toString(), new Object[] { tenantLocalDate }, Long.class);
        } catch (EmptyResultDataAccessException e) {
            // ignore empty result scenario
        } catch (DataAccessException e) {
            throw e;
        }

        return ret;
    }

    @Override
    public List<Long> retrieveSavingsIdsPendingEscheat(LocalDate tenantLocalDate) {
        List<Long> ret = null;
        StringBuilder sql = new StringBuilder("select sa.id ");
        sql.append(" from m_savings_account as sa ");
        sql.append(" inner join m_savings_product as sp on (sa.product_id = sp.id and sp.is_dormancy_tracking_active = true) ");
        sql.append(" where sa.status_enum = 300 ");
        sql.append(" and sa.sub_status_enum = 200 ");
        sql.append(" and " + sqlGenerator.dateDiff("?",
                "(select COALESCE(max(sat.transaction_date),sa.activatedon_date) from m_savings_account_transaction as sat where sat.is_reversed = false and sat.is_reversal = false and sat.transaction_type_enum in (1,2) and sat.savings_account_id = sa.id)")
                + " ");
        sql.append(" >= sp.days_to_escheat ");

        try {
            ret = this.jdbcTemplate.queryForList(sql.toString(), Long.class, new Object[] { tenantLocalDate });
        } catch (EmptyResultDataAccessException e) {
            // ignore empty result scenario
        } catch (DataAccessException e) {
            throw e;
        }

        return ret;
    }

    /*
     * @Override public Collection<SavingsAccountAnnualFeeData> retrieveAccountsWithAnnualFeeDue() { final String sql =
     * "select " + this.annualFeeMapper.schema() +
     * " where sa.annual_fee_next_due_date is not null and sa.annual_fee_next_due_date <= NOW()" ;
     *
     * return this.jdbcTemplate.query(sql, this.annualFeeMapper, new Object[] {}); }
     */

    @Override
    public boolean isAccountBelongsToClient(final Long clientId, final Long accountId, final DepositAccountType depositAccountType,
            final String currencyCode) {
        try {
            final StringBuilder buff = new StringBuilder("select count(*) from m_savings_account sa ");
            buff.append(
                    " where sa.id = ? and sa.client_id = ? and sa.deposit_type_enum = ? and sa.currency_code = ? and sa.status_enum = 300");
            return this.jdbcTemplate.queryForObject(buff.toString(), Integer.class, accountId, clientId, depositAccountType.getValue(),
                    currencyCode) > 0;
        } catch (final EmptyResultDataAccessException e) {
            throw new SavingsAccountNotFoundException(accountId, e);
        }
    }

    @Override
    public String retrieveAccountNumberByAccountId(Long accountId) {
        try {
            final String sql = "select s.account_no from m_savings_account s where s.id = ?";
            return this.jdbcTemplate.queryForObject(sql, String.class, accountId);
        } catch (final EmptyResultDataAccessException e) {
            throw new SavingsAccountNotFoundException(accountId, e);
        }
    }

    @Override
    public Long getSavingsAccountTransactionTotalFiltered(final Long savingsId, DepositAccountType depositAccountType,
            Boolean hideAccrualTransactions) {
        StringBuilder sqlBuilder = new StringBuilder().append(
                " SELECT COUNT(tr.id) FROM m_savings_account sa  JOIN m_savings_account_transaction tr ON tr.savings_account_id = sa.id ")
                .append(" where sa.id = ? and sa.deposit_type_enum = ? ");
        if (hideAccrualTransactions) {
            sqlBuilder.append(" AND transaction_type_enum not in (?,?) ");
        } else {
            sqlBuilder.append(" AND transaction_type_enum in (?,?) ");
        }

        try {
            return this.jdbcTemplate.queryForObject(sqlBuilder.toString(), Long.class,
                    new Object[] { savingsId, depositAccountType.getValue(),
                            SavingsAccountTransactionType.ACCRUAL_INTEREST_POSTING.getValue(),
                            SavingsAccountTransactionType.OVERDRAFT_ACCRUAL_INTEREST.getValue() });
        } catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }

    @Override
    public Collection<SavingsAccountBlockNarrationHistoryData> retrieveSavingsAccountBlockNarrationHistory(Long savingsId) {

        String sql = "select " + this.savingsAccountBlockNarrationHistoryMapper.schema()
                + " where sa.id = ? order by blockNarrationHistory.start_date DESC";
        return this.jdbcTemplate.query(sql, this.savingsAccountBlockNarrationHistoryMapper, new Object[] { savingsId });
    }

    @Override
    public List<Long> retrieveActiveSavingsAccrualAccounts(Long accountType) {
        StringBuilder sql = new StringBuilder(" SELECT distinct msa.id ");
        sql.append(" FROM m_savings_account msa ");
        sql.append(" JOIN m_savings_product msp ON msp.id = msa.product_id ");
        sql.append(" WHERE msa.status_enum = 300 AND msp.accounting_type = 3 ");
        sql.append(" and (msa.nominal_annual_interest_rate != 0 or msa.allow_overdraft = true or msa.account_balance_derived <= 0) ");
        sql.append(" AND msa.deposit_type_enum = ? ");

        return this.jdbcTemplate.queryForList(sql.toString(), Long.class, accountType);
    }

    @Override
    public List<Long> retrieveActiveSavingAccountsForInterestPosting(Long maxSavingsIdInList, int pageSize) {
        String sql = "select id from m_savings_account where status_enum = 300 and (nominal_annual_interest_rate != 0 or (allow_overdraft = true or account_balance_derived <= 0)) "
                + " and id > ? order by id asc limit ?";
        try {
            return Collections.synchronizedList(this.jdbcTemplate.queryForList(sql, Long.class, maxSavingsIdInList, pageSize));
        } catch (final EmptyResultDataAccessException e) {
            return new ArrayList<Long>();
        }
    }


    @Override
    public RecurringMissedTargetData findRecurringDepositAccountWithMissedTarget(Long savingsAccountId) {
        RecurringMissedTargetData result = null;
        final String sql = "select " + this.recurringMissedTargetTemplateMapper.schema();
        try {
            result = this.jdbcTemplate.queryForObject(sql, this.recurringMissedTargetTemplateMapper, new Object[] { savingsAccountId });
        } catch (EmptyResultDataAccessException e) {
            // ignore empty result scenario
        } catch (DataAccessException e) {
            throw e;
        }

        return result;
    }

    @Override
    public Collection<SavingsAccountTransactionData> retrieveSavingsTransactions(String filterConstraintJson, Integer limit,
            Integer offset) {

        ObjectMapper mapper = new ObjectMapper();
        if (StringUtils.isEmpty(filterConstraintJson)) {
            throw new SavingsAccountSearchParameterNotProvidedException();
        }

        try {

            List<Object> params = new ArrayList<>();
            StringBuilder queryBuilder = new StringBuilder(
                    "select " + this.transactionsMapper.schema() + " where transaction_type_enum not in (22,25) ");
            FilterConstraint[] filterConstraints = mapper.readValue(filterConstraintJson, FilterConstraint[].class);
            final String extraCriteria = searchReadPlatformService.buildSqlStringFromFilterConstraints(filterConstraints, params,
                    FilterSelection.SAVINGS_SEARCH_REQUEST_MAP);
            queryBuilder.append(extraCriteria);
            queryBuilder.append(" order by tr.transaction_date DESC, tr.created_date DESC, tr.id DESC LIMIT ? OFFSET ?");
            params.add(limit);
            params.add(offset);

            return this.jdbcTemplate.query(queryBuilder.toString(), this.transactionsMapper, params.toArray());

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class SavingAccountMapperForInterestPosting implements ResultSetExtractor<List<SavingsAccountData>> {

        private final String schemaSql;

        SavingAccountMapperForInterestPosting() {
            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append("sa.id as id, sa.account_no as accountNo, sa.external_id as externalId, ");
            sqlBuilder.append("sa.deposit_type_enum as depositType, ");
            sqlBuilder.append("c.id as clientId, c.office_id as clientOfficeId, ");
            sqlBuilder.append("g.id as groupId, g.office_id as groupOfficeId, ");
            sqlBuilder.append("sa.status_enum as statusEnum, ");
            sqlBuilder.append("sa.sub_status_enum as subStatusEnum, ");
            sqlBuilder.append("sa.submittedon_date as submittedOnDate,");
            sqlBuilder.append("sa.rejectedon_date as rejectedOnDate,");
            sqlBuilder.append("sa.withdrawnon_date as withdrawnOnDate,");
            sqlBuilder.append("sa.approvedon_date as approvedOnDate,");
            sqlBuilder.append("sa.activatedon_date as activatedOnDate,");
            sqlBuilder.append("sa.closedon_date as closedOnDate,");

            sqlBuilder.append(
                    "sa.currency_code as currencyCode, sa.currency_digits as currencyDigits, sa.currency_multiplesof as inMultiplesOf, ");
            sqlBuilder.append("sa.nominal_annual_interest_rate as nominalAnnualInterestRate, ");
            sqlBuilder.append("sa.interest_compounding_period_enum as interestCompoundingPeriodType, ");
            sqlBuilder.append("sa.interest_posting_period_enum as interestPostingPeriodType, ");
            sqlBuilder.append("sa.interest_calculation_type_enum as interestCalculationType, ");
            sqlBuilder.append("sa.interest_calculation_days_in_year_type_enum as interestCalculationDaysInYearType, ");
            sqlBuilder.append("sa.min_required_opening_balance as minRequiredOpeningBalance, ");
            sqlBuilder.append("sa.lockin_period_frequency as lockinPeriodFrequency,");
            sqlBuilder.append("sa.lockin_period_frequency_enum as lockinPeriodFrequencyType, ");
            sqlBuilder.append("sa.withdrawal_fee_for_transfer as withdrawalFeeForTransfers, ");
            sqlBuilder.append("sa.allow_overdraft as allowOverdraft, ");
            sqlBuilder.append("sa.overdraft_limit as overdraftLimit, ");
            sqlBuilder.append("sa.nominal_annual_interest_rate_overdraft as nominalAnnualInterestRateOverdraft, ");
            sqlBuilder.append("sa.min_overdraft_for_interest_calculation as minOverdraftForInterestCalculation, ");
            sqlBuilder.append("sa.total_deposits_derived as totalDeposits, ");
            sqlBuilder.append("sa.total_withdrawals_derived as totalWithdrawals, ");
            sqlBuilder.append("sa.total_withdrawal_fees_derived as totalWithdrawalFees, ");
            sqlBuilder.append("sa.total_annual_fees_derived as totalAnnualFees, ");
            sqlBuilder.append("sa.total_interest_earned_derived as totalInterestEarned, ");
            sqlBuilder.append("sa.total_interest_posted_derived as totalInterestPosted, ");
            sqlBuilder.append("sa.total_overdraft_interest_derived as totalOverdraftInterestDerived, ");
            sqlBuilder.append("sa.account_balance_derived as accountBalance, ");
            sqlBuilder.append("sa.total_fees_charge_derived as totalFeeCharge, ");
            sqlBuilder.append("sa.total_penalty_charge_derived as totalPenaltyCharge, ");
            sqlBuilder.append("sa.min_balance_for_interest_calculation as minBalanceForInterestCalculation,");
            sqlBuilder.append("sa.min_required_balance as minRequiredBalance, ");
            sqlBuilder.append("sa.enforce_min_required_balance as enforceMinRequiredBalance, ");
            sqlBuilder.append("sa.max_allowed_lien_limit as maxAllowedLienLimit, ");
            sqlBuilder.append("sa.is_lien_allowed as lienAllowed, ");
            sqlBuilder.append("sa.on_hold_funds_derived as onHoldFunds, ");
            sqlBuilder.append("sa.withhold_tax as withHoldTax, ");
            sqlBuilder.append("sa.total_withhold_tax_derived as totalWithholdTax, ");
            sqlBuilder.append("sa.last_interest_calculation_date as lastInterestCalculationDate, ");
            sqlBuilder.append("sa.total_savings_amount_on_hold as onHoldAmount, ");
            sqlBuilder.append("sa.interest_posted_till_date as interestPostedTillDate, ");
            sqlBuilder.append("tg.id as taxGroupId, ");
            sqlBuilder.append("(select COALESCE(max(sat.transaction_date),sa.activatedon_date) ");
            sqlBuilder.append("from m_savings_account_transaction as sat ");
            sqlBuilder.append("where sat.is_reversed = false and sat.is_reversal = false ");
            sqlBuilder.append("and sat.transaction_type_enum in (1,2) ");
            sqlBuilder.append("and sat.savings_account_id = sa.id) as lastActiveTransactionDate, ");
            sqlBuilder.append("sp.id as productId, ");
            sqlBuilder.append("sp.is_dormancy_tracking_active as isDormancyTrackingActive, ");
            sqlBuilder.append("sp.days_to_inactive as daysToInactive, ");
            sqlBuilder.append("sp.days_to_dormancy as daysToDormancy, ");
            sqlBuilder.append("sp.days_to_escheat as daysToEscheat, ");
            sqlBuilder.append("sp.num_of_credit_transaction as numOfCreditTransaction,");
            sqlBuilder.append("sp.num_of_debit_transaction as numOfDebitTransaction,");
            // sqlBuilder.append("sa.block_narration_id as blockNarrationId, ");
            // sqlBuilder.append("cvn.code_value as blockNarrationValue, ");
            sqlBuilder.append("sp.accounting_type as accountingType, ");
            sqlBuilder.append("tr.id as transactionId, tr.transaction_type_enum as transactionType, ");
            sqlBuilder.append("tr.transaction_date as transactionDate, tr.amount as transactionAmount,");
            sqlBuilder.append("tr.created_date as createdDate,tr.cumulative_balance_derived as cumulativeBalance,");
            sqlBuilder.append("tr.running_balance_derived as runningBalance, tr.is_reversed as reversed,");
            sqlBuilder.append("tr.balance_end_date_derived as balanceEndDate, tr.overdraft_amount_derived as overdraftAmount,");
            sqlBuilder.append("tr.is_manual as manualTransaction,tr.office_id as officeId, ");
            sqlBuilder.append("pd.payment_type_id as paymentType,pd.account_number as accountNumber,pd.check_number as checkNumber, ");
            sqlBuilder.append("pd.receipt_number as receiptNumber, pd.bank_number as bankNumber,pd.routing_code as routingCode, ");
            sqlBuilder.append("pt.value as paymentTypeName, ");
            sqlBuilder.append("msacpb.amount as paidByAmount, msacpb.id as chargesPaidById, ");
            sqlBuilder.append(
                    "msac.id as chargeId, msac.amount as chargeAmount, msac.charge_time_enum as chargeTimeType, msac.is_penalty as isPenaltyCharge, ");
            sqlBuilder.append("txd.id as taxDetailsId, txd.amount as taxAmount, ");
            sqlBuilder.append("apm.gl_account_id as glAccountIdForInterestOnSavings, apm1.gl_account_id as glAccountIdForSavingsControl, ");
            sqlBuilder.append(
                    "mtc.id as taxComponentId, mtc.debit_account_id as debitAccountId, mtc.credit_account_id as creditAccountId, mtc.percentage as taxPercentage ");
            sqlBuilder.append("from m_savings_account sa ");
            sqlBuilder.append("join m_savings_product sp ON sa.product_id = sp.id ");
            sqlBuilder.append("join m_currency curr on curr.code = sa.currency_code ");
            sqlBuilder.append("join m_savings_account_transaction tr on sa.id = tr.savings_account_id ");
            sqlBuilder.append("left join m_payment_detail pd on pd.id = tr.payment_detail_id ");
            sqlBuilder.append("left join m_payment_type pt on pt.id = pd.payment_type_id ");
            sqlBuilder.append("left join m_savings_account_charge_paid_by msacpb on msacpb.savings_account_transaction_id = tr.id ");
            sqlBuilder.append("left join m_savings_account_charge msac on msac.id = msacpb.savings_account_charge_id ");
            sqlBuilder.append("left join m_client c ON c.id = sa.client_id ");
            sqlBuilder.append("left join m_group g ON g.id = sa.group_id ");
            sqlBuilder.append("left join m_tax_group tg on tg.id = sa.tax_group_id ");
            sqlBuilder.append("left join m_savings_account_transaction_tax_details txd on txd.savings_transaction_id = tr.id ");
            sqlBuilder.append("left join m_tax_component mtc on mtc.id = txd.tax_component_id ");
            sqlBuilder.append("left join acc_product_mapping apm on apm.product_id = sp.id and apm.financial_account_type=3 ");
            sqlBuilder.append("left join acc_product_mapping apm1 on apm1.product_id = sp.id and apm1.financial_account_type=2 ");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public List<SavingsAccountData> extractData(final ResultSet rs) throws SQLException {

            List<SavingsAccountData> savingsAccountDataList = new ArrayList<>();
            HashMap<String, Long> savingsMap = new HashMap<>();
            String currencyCode = null;
            Integer currencyDigits = null;
            Integer inMultiplesOf = null;
            CurrencyData currency = null;
            HashMap<String, Long> transMap = new HashMap<>();
            HashMap<String, Long> taxDetails = new HashMap<>();
            HashMap<String, Long> chargeDetails = new HashMap<>();
            SavingsAccountTransactionData savingsAccountTransactionData = null;
            SavingsAccountData savingsAccountData = null;
            int count = 0;

            while (rs.next()) {
                final Long id = rs.getLong("id");
                final Long transactionId = rs.getLong("transactionId");
                final Long taxDetailId = JdbcSupport.getLongDefaultToNullIfZero(rs, "taxDetailsId");
                final Long taxComponentId = JdbcSupport.getLongDefaultToNullIfZero(rs, "taxComponentId");
                final String accountNo = rs.getString("accountNo");
                final Long chargeId = rs.getLong("chargeId");

                if (!savingsMap.containsValue(id)) {
                    if (count > 0) {
                        savingsAccountDataList.add(savingsAccountData);
                    }
                    count++;
                    savingsMap.put("id", id);

                    final String externalId = rs.getString("externalId");
                    final Integer depositTypeId = rs.getInt("depositType");
                    final EnumOptionData depositType = SavingsEnumerations.depositType(depositTypeId);
                    final Long groupId = JdbcSupport.getLong(rs, "groupId");
                    final Long groupOfficeId = JdbcSupport.getLong(rs, "groupOfficeId");
                    final GroupGeneralData groupGeneralData = new GroupGeneralData(groupId, groupOfficeId);

                    final Long clientId = JdbcSupport.getLong(rs, "clientId");
                    final Long clientOfficeId = JdbcSupport.getLong(rs, "clientOfficeId");
                    final ClientData clientData = ClientData.createClientForInterestPosting(clientId, clientOfficeId);

                    final Long glAccountIdForInterestOnSavings = rs.getLong("glAccountIdForInterestOnSavings");
                    final Long glAccountIdForSavingsControl = rs.getLong("glAccountIdForSavingsControl");

                    final Long productId = rs.getLong("productId");
                    final Integer accountType = rs.getInt("accountingType");
                    final AccountingRuleType accountingRuleType = AccountingRuleType.fromInt(accountType);
                    final EnumOptionData enumOptionDataForAccounting = new EnumOptionData(accountType.longValue(),
                            accountingRuleType.getCode(), accountingRuleType.getValue().toString());
                    final SavingsProductData savingsProductData = SavingsProductData.createForInterestPosting(productId,
                            enumOptionDataForAccounting);

                    final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
                    final SavingsAccountStatusEnumData status = SavingsEnumerations.status(statusEnum);
                    final Integer subStatusEnum = JdbcSupport.getInteger(rs, "subStatusEnum");
                    final SavingsAccountSubStatusEnumData subStatus = SavingsEnumerations.subStatus(subStatusEnum);
                    final LocalDate lastActiveTransactionDate = JdbcSupport.getLocalDate(rs, "lastActiveTransactionDate");
                    final boolean isDormancyTrackingActive = rs.getBoolean("isDormancyTrackingActive");
                    final Integer numDaysToInactive = JdbcSupport.getInteger(rs, "daysToInactive");
                    final Integer numDaysToDormancy = JdbcSupport.getInteger(rs, "daysToDormancy");
                    final Integer numDaysToEscheat = JdbcSupport.getInteger(rs, "daysToEscheat");
                    Integer daysToInactive = null;
                    Integer daysToDormancy = null;
                    Integer daysToEscheat = null;

                    LocalDate localTenantDate = DateUtils.getBusinessLocalDate();
                    if (isDormancyTrackingActive && statusEnum.equals(SavingsAccountStatusType.ACTIVE.getValue())) {
                        if (subStatusEnum < SavingsAccountSubStatusEnum.ESCHEAT.getValue()) {
                            daysToEscheat = Math.toIntExact(
                                    ChronoUnit.DAYS.between(localTenantDate, lastActiveTransactionDate.plusDays(numDaysToEscheat)));
                        }
                        if (subStatusEnum < SavingsAccountSubStatusEnum.DORMANT.getValue()) {
                            daysToDormancy = Math.toIntExact(
                                    ChronoUnit.DAYS.between(localTenantDate, lastActiveTransactionDate.plusDays(numDaysToDormancy)));
                        }
                        if (subStatusEnum < SavingsAccountSubStatusEnum.INACTIVE.getValue()) {
                            daysToInactive = Math.toIntExact(
                                    ChronoUnit.DAYS.between(localTenantDate, lastActiveTransactionDate.plusDays(numDaysToInactive)));
                        }
                    }
                    final LocalDate approvedOnDate = JdbcSupport.getLocalDate(rs, "approvedOnDate");
                    final LocalDate withdrawnOnDate = JdbcSupport.getLocalDate(rs, "withdrawnOnDate");
                    final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
                    final LocalDate activatedOnDate = JdbcSupport.getLocalDate(rs, "activatedOnDate");
                    final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedOnDate");
                    final SavingsAccountApplicationTimelineData timeline = new SavingsAccountApplicationTimelineData(submittedOnDate, null,
                            null, null, null, null, null, null, withdrawnOnDate, null, null, null, approvedOnDate, null, null, null,
                            activatedOnDate, null, null, null, closedOnDate, null, null, null);

                    currencyCode = rs.getString("currencyCode");
                    currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
                    inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
                    currency = new CurrencyData(currencyCode, currencyDigits, inMultiplesOf);

                    final BigDecimal totalDeposits = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalDeposits");
                    final BigDecimal totalWithdrawals = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalWithdrawals");
                    final BigDecimal totalWithdrawalFees = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalWithdrawalFees");
                    final BigDecimal totalAnnualFees = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalAnnualFees");

                    final BigDecimal totalInterestEarned = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalInterestEarned");
                    final BigDecimal totalInterestPosted = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalInterestPosted");
                    final BigDecimal accountBalance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "accountBalance");
                    final BigDecimal totalFeeCharge = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalFeeCharge");
                    final BigDecimal totalPenaltyCharge = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalPenaltyCharge");
                    final BigDecimal totalOverdraftInterestDerived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs,
                            "totalOverdraftInterestDerived");
                    final BigDecimal totalWithholdTax = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalWithholdTax");
                    final LocalDate interestPostedTillDate = JdbcSupport.getLocalDate(rs, "interestPostedTillDate");

                    final BigDecimal minBalanceForInterestCalculation = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
                            "minBalanceForInterestCalculation");
                    final BigDecimal onHoldFunds = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "onHoldFunds");

                    final BigDecimal onHoldAmount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "onHoldAmount");

                    BigDecimal availableBalance = accountBalance;
                    if (availableBalance != null && onHoldFunds != null) {

                        availableBalance = availableBalance.subtract(onHoldFunds);
                    }

                    if (availableBalance != null && onHoldAmount != null) {

                        availableBalance = availableBalance.subtract(onHoldAmount);
                    }

                    BigDecimal interestNotPosted = BigDecimal.ZERO;
                    LocalDate lastInterestCalculationDate = null;
                    if (totalInterestEarned != null) {
                        interestNotPosted = totalInterestEarned.subtract(totalInterestPosted).add(totalOverdraftInterestDerived);
                        lastInterestCalculationDate = JdbcSupport.getLocalDate(rs, "lastInterestCalculationDate");
                    }

                    final SavingsAccountSummaryData summary = new SavingsAccountSummaryData(currency, totalDeposits, totalWithdrawals,
                            totalWithdrawalFees, totalAnnualFees, totalInterestEarned, totalInterestPosted, accountBalance, totalFeeCharge,
                            totalPenaltyCharge, totalOverdraftInterestDerived, totalWithholdTax, interestNotPosted,
                            lastInterestCalculationDate, availableBalance, interestPostedTillDate);
                    summary.setPrevInterestPostedTillDate(interestPostedTillDate);

                    final boolean withHoldTax = rs.getBoolean("withHoldTax");
                    final Long taxGroupId = JdbcSupport.getLongDefaultToNullIfZero(rs, "taxGroupId");
                    TaxGroupData taxGroupData = null;
                    if (taxGroupId != null) {
                        taxGroupData = TaxGroupData.lookup(taxGroupId, null);
                    }

                    final BigDecimal nominalAnnualInterestRate = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
                            "nominalAnnualInterestRate");

                    final EnumOptionData interestCompoundingPeriodType = SavingsEnumerations.compoundingInterestPeriodType(
                            SavingsCompoundingInterestPeriodType.fromInt(JdbcSupport.getInteger(rs, "interestCompoundingPeriodType")));

                    final EnumOptionData interestPostingPeriodType = SavingsEnumerations.interestPostingPeriodType(
                            SavingsPostingInterestPeriodType.fromInt(JdbcSupport.getInteger(rs, "interestPostingPeriodType")));

                    final EnumOptionData interestCalculationType = SavingsEnumerations.interestCalculationType(
                            SavingsInterestCalculationType.fromInt(JdbcSupport.getInteger(rs, "interestCalculationType")));

                    final EnumOptionData interestCalculationDaysInYearType = SavingsEnumerations
                            .interestCalculationDaysInYearType(SavingsInterestCalculationDaysInYearType
                                    .fromInt(JdbcSupport.getInteger(rs, "interestCalculationDaysInYearType")));

                    final BigDecimal minRequiredOpeningBalance = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
                            "minRequiredOpeningBalance");

                    final Integer lockinPeriodFrequency = JdbcSupport.getInteger(rs, "lockinPeriodFrequency");
                    EnumOptionData lockinPeriodFrequencyType = null;
                    final Integer lockinPeriodFrequencyTypeValue = JdbcSupport.getInteger(rs, "lockinPeriodFrequencyType");
                    if (lockinPeriodFrequencyTypeValue != null) {
                        final SavingsPeriodFrequencyType lockinPeriodType = SavingsPeriodFrequencyType
                                .fromInt(lockinPeriodFrequencyTypeValue);
                        lockinPeriodFrequencyType = SavingsEnumerations.lockinPeriodFrequencyType(lockinPeriodType);
                    }

                    final boolean withdrawalFeeForTransfers = rs.getBoolean("withdrawalFeeForTransfers");

                    final boolean allowOverdraft = rs.getBoolean("allowOverdraft");
                    final BigDecimal overdraftLimit = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "overdraftLimit");
                    final BigDecimal nominalAnnualInterestRateOverdraft = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
                            "nominalAnnualInterestRateOverdraft");
                    final BigDecimal minOverdraftForInterestCalculation = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
                            "minOverdraftForInterestCalculation");

                    final BigDecimal minRequiredBalance = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "minRequiredBalance");
                    final boolean enforceMinRequiredBalance = rs.getBoolean("enforceMinRequiredBalance");
                    final BigDecimal maxAllowedLienLimit = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "maxAllowedLienLimit");
                    final boolean lienAllowed = rs.getBoolean("lienAllowed");
                    // final Long blockNarrationId = JdbcSupport.getLong(rs, "blockNarrationId");
                    // final String blockNarrationValue = rs.getString("blockNarrationValue");

                    final CodeValueData blockNarration = null;
                    final Long numOfCreditTransaction = JdbcSupport.getLong(rs, "numOfCreditTransaction");
                    final Long numOfDebitTransaction = JdbcSupport.getLong(rs, "numOfDebitTransaction");
                    savingsAccountData = SavingsAccountData.instance(id, accountNo, depositType, externalId, null, null, null, null,
                            productId, null, null, null, status, subStatus, null, timeline, currency, nominalAnnualInterestRate,
                            interestCompoundingPeriodType, interestPostingPeriodType, interestCalculationType,
                            interestCalculationDaysInYearType, minRequiredOpeningBalance, lockinPeriodFrequency, lockinPeriodFrequencyType,
                            withdrawalFeeForTransfers, summary, allowOverdraft, overdraftLimit, minRequiredBalance,
                            enforceMinRequiredBalance, maxAllowedLienLimit, lienAllowed, minBalanceForInterestCalculation, onHoldFunds,
                            nominalAnnualInterestRateOverdraft, minOverdraftForInterestCalculation, withHoldTax, taxGroupData,
                            lastActiveTransactionDate, isDormancyTrackingActive, daysToInactive, daysToDormancy, daysToEscheat,
                            onHoldAmount, numOfCreditTransaction, numOfDebitTransaction, blockNarration, null, null, null, null, null);

                    savingsAccountData.setClientData(clientData);
                    savingsAccountData.setGroupGeneralData(groupGeneralData);
                    savingsAccountData.setSavingsProduct(savingsProductData);
                    savingsAccountData.setGlAccountIdForInterestOnSavings(glAccountIdForInterestOnSavings);
                    savingsAccountData.setGlAccountIdForSavingsControl(glAccountIdForSavingsControl);
                }

                if (!transMap.containsValue(transactionId)) {

                    final int transactionTypeInt = JdbcSupport.getInteger(rs, "transactionType");
                    final SavingsAccountTransactionEnumData transactionType = SavingsEnumerations.transactionType(transactionTypeInt);

                    final LocalDate date = JdbcSupport.getLocalDate(rs, "transactionDate");
                    final LocalDate balanceEndDate = JdbcSupport.getLocalDate(rs, "balanceEndDate");
                    final LocalDate transSubmittedOnDate = JdbcSupport.getLocalDate(rs, "createdDate");
                    final BigDecimal amount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "transactionAmount");
                    final BigDecimal overdraftAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "overdraftAmount");
                    final BigDecimal outstandingChargeAmount = null;
                    final BigDecimal runningBalance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "runningBalance");
                    final boolean reversed = rs.getBoolean("reversed");
                    final boolean isManualTransaction = rs.getBoolean("manualTransaction");
                    final Long officeId = rs.getLong("officeId");
                    final BigDecimal cumulativeBalance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "cumulativeBalance");

                    final boolean postInterestAsOn = false;

                    PaymentDetailData paymentDetailData = null;
                    if (transactionType.isDepositOrWithdrawal()) {
                        final Long paymentTypeId = JdbcSupport.getLong(rs, "paymentType");
                        if (paymentTypeId != null) {
                            final String typeName = rs.getString("paymentTypeName");
                            final PaymentTypeData paymentTypeData = new PaymentTypeData(paymentTypeId, typeName, null, false, null);
                            paymentDetailData = new PaymentDetailData(id, paymentTypeData, null, null, null, null, null);
                        }
                    }

                    savingsAccountTransactionData = SavingsAccountTransactionData.create(transactionId, transactionType, paymentDetailData,
                            id, accountNo, date, currency, amount, outstandingChargeAmount, runningBalance, reversed, transSubmittedOnDate,
                            postInterestAsOn, cumulativeBalance, balanceEndDate);
                    savingsAccountTransactionData.setOverdraftAmount(overdraftAmount);

                    transMap.put("id", transactionId);
                    if (savingsAccountData.getOfficeId() == null) {
                        savingsAccountData.setOfficeId(officeId);
                    }

                    savingsAccountData.setSavingsAccountTransactionData(savingsAccountTransactionData);
                }

                if (chargeId != null && !chargeDetails.containsValue(chargeId)) {
                    final boolean isPenalty = rs.getBoolean("isPenaltyCharge");
                    final BigDecimal chargeAmount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "chargeAmount");
                    final Integer chargesTimeType = rs.getInt("chargeTimeType");
                    final EnumOptionData enumOptionDataForChargesTimeType = new EnumOptionData(chargesTimeType.longValue(), null, null);
                    final SavingsAccountChargeData savingsAccountChargeData = new SavingsAccountChargeData(chargeId, chargeAmount,
                            enumOptionDataForChargesTimeType, isPenalty);

                    final Long chargesPaidById = rs.getLong("chargesPaidById");
                    final BigDecimal chargesPaid = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "paidByAmount");
                    final SavingsAccountChargesPaidByData savingsAccountChargesPaidByData = new SavingsAccountChargesPaidByData(
                            chargesPaidById, chargesPaid);
                    savingsAccountChargesPaidByData.setSavingsAccountChargeData(savingsAccountChargeData);
                    if (savingsAccountChargesPaidByData != null) {
                        savingsAccountTransactionData.setChargesPaidByData(savingsAccountChargesPaidByData);
                    }

                    chargeDetails.put("id", chargeId);
                }

                if (taxDetailId != null && !taxDetails.containsValue(taxDetailId)) {
                    final BigDecimal amount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "taxAmount");
                    final BigDecimal percentage = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "taxPercentage");
                    final Long debitId = rs.getLong("debitAccountId");
                    final Long creditId = rs.getLong("creditAccountId");
                    final GLAccountData debitAccount = GLAccountData.createFrom(debitId);
                    final GLAccountData creditAccount = GLAccountData.createFrom(creditId);

                    if (taxComponentId != null) {
                        final TaxComponentData taxComponent = TaxComponentData.createTaxComponent(taxComponentId, percentage, debitAccount,
                                creditAccount);
                        savingsAccountTransactionData.setTaxDetails(new TaxDetailsData(taxComponent, amount));
                    }

                    taxDetails.put("id", taxDetailId);
                }

            }
            if (savingsAccountData != null) {
                savingsAccountDataList.add(savingsAccountData);
            }
            return savingsAccountDataList;

        }
    }

    private static final class SavingAccountMapper implements RowMapper<SavingsAccountData> {

        private final String schemaSql;

        public SavingAccountMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append("sa.id as id, sa.account_no as accountNo, sa.external_id as externalId, ");
            sqlBuilder.append("sa.deposit_type_enum as depositType, ");
            sqlBuilder.append("c.id as clientId, c.display_name as clientName, ");
            sqlBuilder.append("g.id as groupId, g.display_name as groupName, ");
            sqlBuilder.append("sp.id as productId, sp.name as productName, ");
            sqlBuilder.append("s.id fieldOfficerId, s.display_name as fieldOfficerName, ");
            sqlBuilder.append("sa.status_enum as statusEnum, ");
            sqlBuilder.append("sa.sub_status_enum as subStatusEnum, ");
            sqlBuilder.append("sa.reason_for_block as reasonForBlock, ");
            sqlBuilder.append("sa.submittedon_date as submittedOnDate,");
            sqlBuilder.append("sbu.username as submittedByUsername,");
            sqlBuilder.append("sbu.firstname as submittedByFirstname, sbu.lastname as submittedByLastname,");

            sqlBuilder.append("sa.rejectedon_date as rejectedOnDate,");
            sqlBuilder.append("rbu.username as rejectedByUsername,");
            sqlBuilder.append("rbu.firstname as rejectedByFirstname, rbu.lastname as rejectedByLastname,");

            sqlBuilder.append("sa.withdrawnon_date as withdrawnOnDate,");
            sqlBuilder.append("wbu.username as withdrawnByUsername,");
            sqlBuilder.append("wbu.firstname as withdrawnByFirstname, wbu.lastname as withdrawnByLastname,");

            sqlBuilder.append("sa.approvedon_date as approvedOnDate,");
            sqlBuilder.append("abu.username as approvedByUsername,");
            sqlBuilder.append("abu.firstname as approvedByFirstname, abu.lastname as approvedByLastname,");

            sqlBuilder.append("sa.activatedon_date as activatedOnDate,");
            sqlBuilder.append("avbu.username as activatedByUsername,");
            sqlBuilder.append("avbu.firstname as activatedByFirstname, avbu.lastname as activatedByLastname,");

            sqlBuilder.append("sa.closedon_date as closedOnDate,");
            sqlBuilder.append("cbu.username as closedByUsername,");
            sqlBuilder.append("cbu.firstname as closedByFirstname, cbu.lastname as closedByLastname,");

            sqlBuilder.append(
                    "sa.currency_code as currencyCode, sa.currency_digits as currencyDigits, sa.currency_multiplesof as inMultiplesOf, ");
            sqlBuilder.append("curr.name as currencyName, curr.internationalized_name_code as currencyNameCode, ");
            sqlBuilder.append("curr.display_symbol as currencyDisplaySymbol, ");

            sqlBuilder.append("sa.nominal_annual_interest_rate as nominalAnnualInterestRate, ");
            sqlBuilder.append("sa.interest_compounding_period_enum as interestCompoundingPeriodType, ");
            sqlBuilder.append("sa.interest_posting_period_enum as interestPostingPeriodType, ");
            sqlBuilder.append("sa.interest_calculation_type_enum as interestCalculationType, ");
            sqlBuilder.append("sa.interest_calculation_days_in_year_type_enum as interestCalculationDaysInYearType, ");
            sqlBuilder.append("sa.min_required_opening_balance as minRequiredOpeningBalance, ");
            sqlBuilder.append("sa.lockin_period_frequency as lockinPeriodFrequency,");
            sqlBuilder.append("sa.lockin_period_frequency_enum as lockinPeriodFrequencyType, ");
            sqlBuilder.append("sa.allow_overdraft as allowOverdraft, ");
            sqlBuilder.append("sa.overdraft_limit as overdraftLimit, ");
            sqlBuilder.append("sa.nominal_annual_interest_rate_overdraft as nominalAnnualInterestRateOverdraft, ");
            sqlBuilder.append("sa.min_overdraft_for_interest_calculation as minOverdraftForInterestCalculation, ");
            sqlBuilder.append("sa.total_deposits_derived as totalDeposits, ");
            sqlBuilder.append("sa.total_withdrawals_derived as totalWithdrawals, ");
            sqlBuilder.append("sa.total_withdrawal_fees_derived as totalWithdrawalFees, ");
            sqlBuilder.append("sa.total_annual_fees_derived as totalAnnualFees, ");
            sqlBuilder.append("sa.total_interest_earned_derived as totalInterestEarned, ");
            sqlBuilder.append("sa.total_interest_posted_derived as totalInterestPosted, ");
            sqlBuilder.append("sa.total_overdraft_interest_derived as totalOverdraftInterestDerived, ");
            sqlBuilder.append("sa.account_balance_derived as accountBalance, ");
            sqlBuilder.append("sa.total_fees_charge_derived as totalFeeCharge, ");
            sqlBuilder.append("sa.total_penalty_charge_derived as totalPenaltyCharge, ");
            sqlBuilder.append("sa.min_balance_for_interest_calculation as minBalanceForInterestCalculation,");
            sqlBuilder.append("sa.min_required_balance as minRequiredBalance, ");
            sqlBuilder.append("sa.enforce_min_required_balance as enforceMinRequiredBalance, ");
            sqlBuilder.append("sa.max_allowed_lien_limit as maxAllowedLienLimit, ");
            sqlBuilder.append("sa.is_lien_allowed as lienAllowed, ");
            sqlBuilder.append("sa.on_hold_funds_derived as onHoldFunds, ");
            sqlBuilder.append("sa.withhold_tax as withHoldTax, ");
            sqlBuilder.append("sa.total_withhold_tax_derived as totalWithholdTax, ");
            sqlBuilder.append("sa.last_interest_calculation_date as lastInterestCalculationDate, ");
            sqlBuilder.append("sa.interest_posted_till_date as interestPostedTillDate, ");
            sqlBuilder.append("sa.total_savings_amount_on_hold as onHoldAmount, ");
            sqlBuilder.append("sa.use_floating_interest_rate as useFloatingInterestRate, ");
            sqlBuilder.append("sa.withdrawal_fee_for_transfer as withdrawalFeeForTransfers, ");
            sqlBuilder.append("tg.id as taxGroupId, tg.name as taxGroupName, ");
            sqlBuilder.append("(select COALESCE(max(sat.transaction_date),sa.activatedon_date) ");
            sqlBuilder.append("from m_savings_account_transaction as sat ");
            sqlBuilder.append("where sat.is_reversed = false and sat.is_reversal = false ");
            sqlBuilder.append("and sat.transaction_type_enum in (1,2) ");
            sqlBuilder.append("and sat.savings_account_id = sa.id) as lastActiveTransactionDate, ");
            sqlBuilder.append("sp.is_dormancy_tracking_active as isDormancyTrackingActive, ");
            sqlBuilder.append("sp.days_to_inactive as daysToInactive, ");
            sqlBuilder.append("sp.days_to_dormancy as daysToDormancy, ");
            sqlBuilder.append("sp.days_to_escheat as daysToEscheat, ");
            sqlBuilder.append("sa.block_narration_id as blockNarrationId, ");
            sqlBuilder.append("sp.num_of_credit_transaction as numOfCreditTransaction,");
            sqlBuilder.append("sp.num_of_debit_transaction as numOfDebitTransaction,");
            sqlBuilder.append("cvn.code_value as blockNarrationValue, ");
            sqlBuilder.append("sa.vault_target_amount as vaultTargetAmount , sa.vault_target_date as vaultTargetDate, ");
            sqlBuilder.append("sa.account_type_enum as accountType, ");
            sqlBuilder.append("sa.lockedin_until_date_derived as lockedInUntilDate, ");
            sqlBuilder.append("sa.withdrawal_frequency as withdrawalFrequency, ");
            sqlBuilder.append("sa.withdrawal_frequency_enum as withdrawalFrequencyEnum, ");
            sqlBuilder.append("sa.previous_flex_withdrawal_date as previousFlexWithdrawalDate, ");
            sqlBuilder.append("sa.next_flex_withdrawal_date as nextFlexWithdrawalDate, ");
            sqlBuilder.append("sa.post_overdraft_interest_on_deposit as postOverdraftInterestOnDeposit ");
            sqlBuilder.append("from m_savings_account sa ");
            sqlBuilder.append("join m_savings_product sp ON sa.product_id = sp.id ");
            sqlBuilder.append("join m_currency curr on curr.code = sa.currency_code ");
            sqlBuilder.append("left join m_client c ON c.id = sa.client_id ");
            sqlBuilder.append("left join m_group g ON g.id = sa.group_id ");
            sqlBuilder.append("left join m_staff s ON s.id = sa.field_officer_id ");
            sqlBuilder.append("left join m_appuser sbu on sbu.id = sa.submittedon_userid ");
            sqlBuilder.append("left join m_appuser rbu on rbu.id = sa.rejectedon_userid ");
            sqlBuilder.append("left join m_appuser wbu on wbu.id = sa.withdrawnon_userid ");
            sqlBuilder.append("left join m_appuser abu on abu.id = sa.approvedon_userid ");
            sqlBuilder.append("left join m_appuser avbu on avbu.id = sa.activatedon_userid ");
            sqlBuilder.append("left join m_appuser cbu on cbu.id = sa.closedon_userid ");
            sqlBuilder.append("left join m_tax_group tg on tg.id = sa.tax_group_id  ");
            sqlBuilder.append("left join m_code_value cvn on cvn.id = sa.block_narration_id  ");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public SavingsAccountData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String accountNo = rs.getString("accountNo");
            final String externalId = rs.getString("externalId");
            final Integer depositTypeId = rs.getInt("depositType");
            final Integer accountTypeId = rs.getInt("accountType");
            final LocalDate lockedInUntilDate = JdbcSupport.getLocalDate(rs, "lockedInUntilDate");
            final EnumOptionData depositType = SavingsEnumerations.depositType(depositTypeId);
            final AccountType accountType = AccountType.fromInt(accountTypeId);

            final Long groupId = JdbcSupport.getLong(rs, "groupId");
            final String groupName = rs.getString("groupName");

            final Long clientId = JdbcSupport.getLong(rs, "clientId");
            final String clientName = rs.getString("clientName");

            final Long productId = rs.getLong("productId");
            final String productName = rs.getString("productName");

            final Long fieldOfficerId = rs.getLong("fieldOfficerId");
            final String fieldOfficerName = rs.getString("fieldOfficerName");

            final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
            final SavingsAccountStatusEnumData status = SavingsEnumerations.status(statusEnum);

            final Integer subStatusEnum = JdbcSupport.getInteger(rs, "subStatusEnum");
            final SavingsAccountSubStatusEnumData subStatus = SavingsEnumerations.subStatus(subStatusEnum);
            final String reasonForBlock = rs.getString("reasonForBlock");

            final LocalDate lastActiveTransactionDate = JdbcSupport.getLocalDate(rs, "lastActiveTransactionDate");
            final boolean isDormancyTrackingActive = rs.getBoolean("isDormancyTrackingActive");
            final Integer numDaysToInactive = JdbcSupport.getInteger(rs, "daysToInactive");
            final Integer numDaysToDormancy = JdbcSupport.getInteger(rs, "daysToDormancy");
            final Integer numDaysToEscheat = JdbcSupport.getInteger(rs, "daysToEscheat");
            Integer daysToInactive = null;
            Integer daysToDormancy = null;
            Integer daysToEscheat = null;

            LocalDate localTenantDate = DateUtils.getBusinessLocalDate();
            if (isDormancyTrackingActive && statusEnum.equals(SavingsAccountStatusType.ACTIVE.getValue())) {
                if (subStatusEnum < SavingsAccountSubStatusEnum.ESCHEAT.getValue()) {
                    daysToEscheat = Math
                            .toIntExact(ChronoUnit.DAYS.between(localTenantDate, lastActiveTransactionDate.plusDays(numDaysToEscheat)));
                }
                if (subStatusEnum < SavingsAccountSubStatusEnum.DORMANT.getValue()) {
                    daysToDormancy = Math
                            .toIntExact(ChronoUnit.DAYS.between(localTenantDate, lastActiveTransactionDate.plusDays(numDaysToDormancy)));
                }
                if (subStatusEnum < SavingsAccountSubStatusEnum.INACTIVE.getValue()) {
                    daysToInactive = Math
                            .toIntExact(ChronoUnit.DAYS.between(localTenantDate, lastActiveTransactionDate.plusDays(numDaysToInactive)));
                }
            }

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

            final SavingsAccountApplicationTimelineData timeline = new SavingsAccountApplicationTimelineData(submittedOnDate,
                    submittedByUsername, submittedByFirstname, submittedByLastname, rejectedOnDate, rejectedByUsername, rejectedByFirstname,
                    rejectedByLastname, withdrawnOnDate, withdrawnByUsername, withdrawnByFirstname, withdrawnByLastname, approvedOnDate,
                    approvedByUsername, approvedByFirstname, approvedByLastname, activatedOnDate, activatedByUsername, activatedByFirstname,
                    activatedByLastname, closedOnDate, closedByUsername, closedByFirstname, closedByLastname);

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final CurrencyData currency = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf, currencyDisplaySymbol,
                    currencyNameCode);

            final BigDecimal nominalAnnualInterestRate = rs.getBigDecimal("nominalAnnualInterestRate");

            final EnumOptionData interestCompoundingPeriodType = SavingsEnumerations.compoundingInterestPeriodType(
                    SavingsCompoundingInterestPeriodType.fromInt(JdbcSupport.getInteger(rs, "interestCompoundingPeriodType")));

            final EnumOptionData interestPostingPeriodType = SavingsEnumerations.interestPostingPeriodType(
                    SavingsPostingInterestPeriodType.fromInt(JdbcSupport.getInteger(rs, "interestPostingPeriodType")));

            final EnumOptionData interestCalculationType = SavingsEnumerations
                    .interestCalculationType(SavingsInterestCalculationType.fromInt(JdbcSupport.getInteger(rs, "interestCalculationType")));

            final EnumOptionData interestCalculationDaysInYearType = SavingsEnumerations.interestCalculationDaysInYearType(
                    SavingsInterestCalculationDaysInYearType.fromInt(JdbcSupport.getInteger(rs, "interestCalculationDaysInYearType")));

            final BigDecimal minRequiredOpeningBalance = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "minRequiredOpeningBalance");

            final Integer lockinPeriodFrequency = JdbcSupport.getInteger(rs, "lockinPeriodFrequency");
            EnumOptionData lockinPeriodFrequencyType = null;
            final Integer lockinPeriodFrequencyTypeValue = JdbcSupport.getInteger(rs, "lockinPeriodFrequencyType");
            if (lockinPeriodFrequencyTypeValue != null) {
                final SavingsPeriodFrequencyType lockinPeriodType = SavingsPeriodFrequencyType.fromInt(lockinPeriodFrequencyTypeValue);
                lockinPeriodFrequencyType = SavingsEnumerations.lockinPeriodFrequencyType(lockinPeriodType);
            }

            final boolean withdrawalFeeForTransfers = rs.getBoolean("withdrawalFeeForTransfers");

            final boolean allowOverdraft = rs.getBoolean("allowOverdraft");
            final BigDecimal overdraftLimit = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "overdraftLimit");
            final BigDecimal nominalAnnualInterestRateOverdraft = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
                    "nominalAnnualInterestRateOverdraft");
            final BigDecimal minOverdraftForInterestCalculation = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
                    "minOverdraftForInterestCalculation");

            final BigDecimal minRequiredBalance = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "minRequiredBalance");
            final boolean enforceMinRequiredBalance = rs.getBoolean("enforceMinRequiredBalance");

            final BigDecimal maxAllowedLienLimit = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "maxAllowedLienLimit");
            final boolean lienAllowed = rs.getBoolean("lienAllowed");

            final BigDecimal totalDeposits = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalDeposits");
            final BigDecimal totalWithdrawals = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalWithdrawals");
            final BigDecimal totalWithdrawalFees = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalWithdrawalFees");
            final BigDecimal totalAnnualFees = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalAnnualFees");

            final BigDecimal totalInterestEarned = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalInterestEarned");
            final BigDecimal totalInterestPosted = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalInterestPosted");
            final BigDecimal accountBalance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "accountBalance");
            final BigDecimal totalFeeCharge = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalFeeCharge");
            final BigDecimal totalPenaltyCharge = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalPenaltyCharge");
            final BigDecimal totalOverdraftInterestDerived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs,
                    "totalOverdraftInterestDerived");
            final BigDecimal totalWithholdTax = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "totalWithholdTax");
            final LocalDate interestPostedTillDate = JdbcSupport.getLocalDate(rs, "interestPostedTillDate");

            final BigDecimal minBalanceForInterestCalculation = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
                    "minBalanceForInterestCalculation");
            final BigDecimal onHoldFunds = rs.getBigDecimal("onHoldFunds");

            final BigDecimal onHoldAmount = rs.getBigDecimal("onHoldAmount");

            BigDecimal availableBalance = accountBalance;
            if (availableBalance != null && onHoldFunds != null) {
                availableBalance = availableBalance.subtract(onHoldFunds);
            }

            if (availableBalance != null && onHoldAmount != null) {

                availableBalance = availableBalance.subtract(onHoldAmount);
            }

            BigDecimal interestNotPosted = BigDecimal.ZERO;
            LocalDate lastInterestCalculationDate = null;
            if (totalInterestEarned != null) {
                interestNotPosted = totalInterestEarned.subtract(totalInterestPosted).add(totalOverdraftInterestDerived);
                lastInterestCalculationDate = JdbcSupport.getLocalDate(rs, "lastInterestCalculationDate");
            }

            final SavingsAccountSummaryData summary = new SavingsAccountSummaryData(currency, totalDeposits, totalWithdrawals,
                    totalWithdrawalFees, totalAnnualFees, totalInterestEarned, totalInterestPosted, accountBalance, totalFeeCharge,
                    totalPenaltyCharge, totalOverdraftInterestDerived, totalWithholdTax, interestNotPosted, lastInterestCalculationDate,
                    availableBalance, interestPostedTillDate);

            final boolean withHoldTax = rs.getBoolean("withHoldTax");
            final Long taxGroupId = JdbcSupport.getLong(rs, "taxGroupId");
            final String taxGroupName = rs.getString("taxGroupName");
            TaxGroupData taxGroupData = null;
            if (taxGroupId != null) {
                taxGroupData = TaxGroupData.lookup(taxGroupId, taxGroupName);
            }

            final Long blockNarrationId = JdbcSupport.getLong(rs, "blockNarrationId");
            final String blockNarrationValue = rs.getString("blockNarrationValue");

            final CodeValueData blockNarration = CodeValueData.instance(blockNarrationId, blockNarrationValue);

            final Long numOfCreditTransaction = JdbcSupport.getLong(rs, "numOfCreditTransaction");
            final Long numOfDebitTransaction = JdbcSupport.getLong(rs, "numOfDebitTransaction");
            final LocalDate vaultTargetDate = JdbcSupport.getLocalDate(rs, "vaultTargetDate");
            final BigDecimal vaultTargetAmount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "vaultTargetAmount");
            final boolean useFloatingInterestRate = rs.getBoolean("useFloatingInterestRate");
            final Integer withdrawalFrequency = JdbcSupport.getInteger(rs, "withdrawalFrequency");

            EnumOptionData withdrawalFrequencyEnum = null;
            final Integer withdrawalFrequencyEnumValue = JdbcSupport.getInteger(rs, "withdrawalFrequencyEnum");
            if (withdrawalFrequencyEnumValue != null) {
                withdrawalFrequencyEnum = SavingsEnumerations
                        .withdrawalFrequency(WithdrawalFrequency.fromInt(withdrawalFrequencyEnumValue));
            }
            final LocalDate previousFlexWithdrawalDate = JdbcSupport.getLocalDate(rs, "previousFlexWithdrawalDate");
            final LocalDate nextFlexWithdrawalDate = JdbcSupport.getLocalDate(rs, "nextFlexWithdrawalDate");
            final boolean postOverdraftInterestOnDeposit = rs.getBoolean("postOverdraftInterestOnDeposit");

            SavingsAccountData savingsAccountData = SavingsAccountData.instance(id, accountNo, depositType, externalId, groupId, groupName,
                    clientId, clientName, productId, productName, fieldOfficerId, fieldOfficerName, status, subStatus, reasonForBlock,
                    timeline, currency, nominalAnnualInterestRate, interestCompoundingPeriodType, interestPostingPeriodType,
                    interestCalculationType, interestCalculationDaysInYearType, minRequiredOpeningBalance, lockinPeriodFrequency,
                    lockinPeriodFrequencyType, withdrawalFeeForTransfers, summary, allowOverdraft, overdraftLimit, minRequiredBalance,
                    enforceMinRequiredBalance, maxAllowedLienLimit, lienAllowed, minBalanceForInterestCalculation, onHoldFunds,
                    nominalAnnualInterestRateOverdraft, minOverdraftForInterestCalculation, withHoldTax, taxGroupData,
                    lastActiveTransactionDate, isDormancyTrackingActive, daysToInactive, daysToDormancy, daysToEscheat, onHoldAmount,
                    numOfCreditTransaction, numOfDebitTransaction, blockNarration, vaultTargetDate, vaultTargetAmount, accountType,
                    lockedInUntilDate, null);
            savingsAccountData.setUseFloatingInterestRate(useFloatingInterestRate);
            savingsAccountData.setWithdrawalFrequency(withdrawalFrequency);
            savingsAccountData.setWithdrawalFrequencyEnum(withdrawalFrequencyEnum);
            savingsAccountData.setPreviousFlexWithdrawalDate(previousFlexWithdrawalDate);
            savingsAccountData.setNextFlexWithdrawalDate(nextFlexWithdrawalDate);
            savingsAccountData.setPostOverdraftInterestOnDeposit(postOverdraftInterestOnDeposit);
            return savingsAccountData;
        }
    }

    private static final class SavingAccountMapperForLookup implements RowMapper<SavingsAccountData> {

        private final String schemaSql;

        SavingAccountMapperForLookup() {
            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append("sa.id as id, sa.account_no as accountNo, ");
            sqlBuilder.append("sa.deposit_type_enum as depositType, ");
            sqlBuilder.append("sp.id as productId, sp.name as productName, ");
            sqlBuilder.append("sa.status_enum as statusEnum ");

            sqlBuilder.append("from m_savings_account sa ");
            sqlBuilder.append("join m_savings_product sp ON sa.product_id = sp.id ");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public SavingsAccountData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String accountNo = rs.getString("accountNo");
            final Integer depositTypeId = rs.getInt("depositType");
            final EnumOptionData depositType = SavingsEnumerations.depositType(depositTypeId);

            final Long productId = rs.getLong("productId");
            final String productName = rs.getString("productName");

            final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
            final SavingsAccountStatusEnumData status = SavingsEnumerations.status(statusEnum);

            return SavingsAccountData.lookupWithProductDetails(id, accountNo, depositType, productId, productName, status);
        }
    }

    private static final class SavingsAccountTransactionsForBatchMapper implements RowMapper<SavingsAccountTransactionData> {

        private final String schemaSql;

        SavingsAccountTransactionsForBatchMapper() {

            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append("tr.id as transactionId, tr.ref_no as refNo ");
            sqlBuilder.append("from m_savings_account_transaction tr");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public SavingsAccountTransactionData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("transactionId");
            SavingsAccountTransactionData savingsAccountTransactionData = SavingsAccountTransactionData.create(id);
            final String refNo = rs.getString("refNo");
            savingsAccountTransactionData.setRefNo(refNo);
            return savingsAccountTransactionData;
        }
    }

    private static final class SavingsAccountTransactionsMapper implements RowMapper<SavingsAccountTransactionData> {

        private final String schemaSql;

        SavingsAccountTransactionsMapper() {

            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append("tr.id as transactionId,");
            sqlBuilder.append(
                    "CASE WHEN tr.transaction_type_enum = 2 AND  pd.actual_transaction_type = 'REVOKED_INTEREST' THEN 72  ELSE tr.transaction_type_enum END as transactionType, ");
            sqlBuilder.append("tr.transaction_date as transactionDate, tr.amount as transactionAmount,");
            sqlBuilder.append(" tr.release_id_of_hold_amount as releaseTransactionId,sp.id productId,mc.office_id officeId,");
            sqlBuilder.append(" tr.reason_for_block as reasonForBlock,sp.nominal_annual_interest_rate interestRate,");
            sqlBuilder.append("tr.created_date as submittedOnDate,sp.overdraft_limit overdraftInterestLimit,");
            sqlBuilder.append("sp.nominal_annual_interest_rate_overdraft overdraftInterestRate,");
            sqlBuilder.append(" au.username as submittedByUsername,au.id userId, ");
            sqlBuilder.append(" nt.note as transactionNote, tr.overdraft_amount_derived overdraftAmount, ");
            sqlBuilder.append("tr.running_balance_derived as runningBalance, tr.is_reversed as reversed,");
            sqlBuilder.append(
                    "tr.is_reversal as isReversal, tr.original_transaction_id as originalTransactionId, tr.is_lien_transaction as lienTransaction, ");
            sqlBuilder.append("fromtran.id as fromTransferId, fromtran.is_reversed as fromTransferReversed,");
            sqlBuilder.append("fromtran.transaction_date as fromTransferDate, fromtran.amount as fromTransferAmount,");
            sqlBuilder.append("fromtran.description as fromTransferDescription,");
            sqlBuilder.append("totran.id as toTransferId, totran.is_reversed as toTransferReversed,tr.office_id officeId,");
            sqlBuilder.append("totran.transaction_date as toTransferDate, totran.amount as toTransferAmount,");
            sqlBuilder.append("totran.description as toTransferDescription,");
            sqlBuilder.append("sa.id as savingsId, sa.account_no as accountNo,");
            sqlBuilder.append("pd.payment_type_id as paymentType,pd.account_number as accountNumber,pd.check_number as checkNumber, ");
            sqlBuilder.append("pd.receipt_number as receiptNumber, pd.bank_number as bankNumber,pd.routing_code as routingCode, ");
            sqlBuilder.append(
                    "sa.currency_code as currencyCode, sa.currency_digits as currencyDigits, sa.currency_multiplesof as inMultiplesOf, ");
            sqlBuilder.append("curr.name as currencyName, curr.internationalized_name_code as currencyNameCode, ");
            sqlBuilder.append("curr.display_symbol as currencyDisplaySymbol, ");
            sqlBuilder.append("pt.value as paymentTypeName, ");
            sqlBuilder.append("tr.is_manual as postInterestAsOn ");
            sqlBuilder.append("from m_savings_account sa ");
            sqlBuilder.append(" join m_savings_account_transaction tr on tr.savings_account_id = sa.id ");
            sqlBuilder.append(" left join m_savings_product sp on sp.id = sa.product_id ");
            sqlBuilder.append(" left join m_office mo on mo.id=tr.office_id ");
            sqlBuilder.append(" left join m_client mc on mc.id = sa.client_id ");
            sqlBuilder.append(" left join m_currency curr on curr.code = sa.currency_code ");
            sqlBuilder.append("left join m_account_transfer_transaction fromtran on fromtran.from_savings_transaction_id = tr.id ");
            sqlBuilder.append("left join m_account_transfer_transaction totran on totran.to_savings_transaction_id = tr.id ");
            sqlBuilder.append("left join m_payment_detail pd on tr.payment_detail_id = pd.id ");
            sqlBuilder.append("left join m_payment_type pt on pd.payment_type_id = pt.id ");
            sqlBuilder.append(" left join m_appuser au on au.id=tr.appuser_id ");
            sqlBuilder.append(" left join m_note nt ON nt.savings_account_transaction_id=tr.id ");
            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public SavingsAccountTransactionData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Long id = rs.getLong("transactionId");
            final int transactionTypeInt = JdbcSupport.getInteger(rs, "transactionType");
            final SavingsAccountTransactionEnumData transactionType = SavingsEnumerations.transactionType(transactionTypeInt);

            final LocalDate date = JdbcSupport.getLocalDate(rs, "transactionDate");
            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
            final LocalDateTime createdDate = JdbcSupport.getLocalDateTime(rs, "submittedOnDate");
            final BigDecimal amount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "transactionAmount");
            final Long releaseTransactionId = rs.getLong("releaseTransactionId");
            final String reasonForBlock = rs.getString("reasonForBlock");
            final BigDecimal outstandingChargeAmount = null;
            final BigDecimal runningBalance = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "runningBalance");
            final boolean reversed = rs.getBoolean("reversed");
            final boolean isReversal = rs.getBoolean("isReversal");
            final Long originalTransactionId = rs.getLong("originalTransactionId");
            final Boolean lienTransaction = rs.getBoolean("lienTransaction");

            final Long savingsId = rs.getLong("savingsId");
            final String accountNo = rs.getString("accountNo");
            final boolean postInterestAsOn = rs.getBoolean("postInterestAsOn");

            PaymentDetailData paymentDetailData = null;
            if (transactionType.isDepositOrWithdrawal()) {
                final Long paymentTypeId = JdbcSupport.getLong(rs, "paymentType");
                if (paymentTypeId != null) {
                    final String typeName = rs.getString("paymentTypeName");
                    final PaymentTypeData paymentType = PaymentTypeData.instance(paymentTypeId, typeName);
                    final String accountNumber = rs.getString("accountNumber");
                    final String checkNumber = rs.getString("checkNumber");
                    final String routingCode = rs.getString("routingCode");
                    final String receiptNumber = rs.getString("receiptNumber");
                    final String bankNumber = rs.getString("bankNumber");
                    paymentDetailData = new PaymentDetailData(id, paymentType, accountNumber, checkNumber, routingCode, receiptNumber,
                            bankNumber);
                }
            }

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final CurrencyData currency = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf, currencyDisplaySymbol,
                    currencyNameCode);

            AccountTransferData transfer = null;
            final Long fromTransferId = JdbcSupport.getLong(rs, "fromTransferId");
            final Long toTransferId = JdbcSupport.getLong(rs, "toTransferId");
            if (fromTransferId != null) {
                final LocalDate fromTransferDate = JdbcSupport.getLocalDate(rs, "fromTransferDate");
                final BigDecimal fromTransferAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "fromTransferAmount");
                final boolean fromTransferReversed = rs.getBoolean("fromTransferReversed");
                final String fromTransferDescription = rs.getString("fromTransferDescription");

                transfer = AccountTransferData.transferBasicDetails(fromTransferId, currency, fromTransferAmount, fromTransferDate,
                        fromTransferDescription, fromTransferReversed);
            } else if (toTransferId != null) {
                final LocalDate toTransferDate = JdbcSupport.getLocalDate(rs, "toTransferDate");
                final BigDecimal toTransferAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "toTransferAmount");
                final boolean toTransferReversed = rs.getBoolean("toTransferReversed");
                final String toTransferDescription = rs.getString("toTransferDescription");

                transfer = AccountTransferData.transferBasicDetails(toTransferId, currency, toTransferAmount, toTransferDate,
                        toTransferDescription, toTransferReversed);
            }
            final String submittedByUsername = rs.getString("submittedByUsername");
            final String note = rs.getString("transactionNote");
            return SavingsAccountTransactionData.create(id, transactionType, paymentDetailData, savingsId, accountNo, date, currency,
                    amount, outstandingChargeAmount, runningBalance, reversed, transfer, submittedOnDate, postInterestAsOn,
                    submittedByUsername, note, isReversal, originalTransactionId, lienTransaction, releaseTransactionId, reasonForBlock,
                    createdDate);
        }
    }

    private static final class SavingsAccountTransactionTemplateMapper implements RowMapper<SavingsAccountTransactionData> {

        private final String schemaSql;

        SavingsAccountTransactionTemplateMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append("sa.id as id, sa.account_no as accountNo, ");
            sqlBuilder.append(
                    "sa.currency_code as currencyCode, sa.currency_digits as currencyDigits, sa.currency_multiplesof as inMultiplesOf, ");
            sqlBuilder.append("curr.name as currencyName, curr.internationalized_name_code as currencyNameCode, ");
            sqlBuilder.append("curr.display_symbol as currencyDisplaySymbol, ");
            sqlBuilder.append("sa.min_required_opening_balance as minRequiredOpeningBalance ");
            sqlBuilder.append("from m_savings_account sa ");
            sqlBuilder.append("join m_currency curr on curr.code = sa.currency_code ");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public SavingsAccountTransactionData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long savingsId = rs.getLong("id");
            final String accountNo = rs.getString("accountNo");

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final CurrencyData currency = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf, currencyDisplaySymbol,
                    currencyNameCode);

            return SavingsAccountTransactionData.template(savingsId, accountNo, DateUtils.getBusinessLocalDate(), currency,
                    DateUtils.getLocalDateTimeOfTenant());
        }
    }

    private static final class SavingAccountTemplateMapper implements RowMapper<SavingsAccountData> {

        private final ClientData client;
        private final GroupGeneralData group;

        private final String schemaSql;

        SavingAccountTemplateMapper(final ClientData client, final GroupGeneralData group) {
            this.client = client;
            this.group = group;

            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append("sp.id as productId, sp.name as productName, sp.name as productName, ");
            sqlBuilder.append(
                    "sp.currency_code as currencyCode, sp.currency_digits as currencyDigits, sp.currency_multiplesof as inMultiplesOf, ");
            sqlBuilder.append("curr.name as currencyName, curr.internationalized_name_code as currencyNameCode, ");
            sqlBuilder.append("curr.display_symbol as currencyDisplaySymbol, ");
            sqlBuilder.append("sp.nominal_annual_interest_rate as nominalAnnualIterestRate, ");
            sqlBuilder.append("sp.interest_compounding_period_enum as interestCompoundingPeriodType, ");
            sqlBuilder.append("sp.interest_posting_period_enum as interestPostingPeriodType, ");
            sqlBuilder.append("sp.interest_calculation_type_enum as interestCalculationType, ");
            sqlBuilder.append("sp.interest_calculation_days_in_year_type_enum as interestCalculationDaysInYearType, ");
            sqlBuilder.append("sp.min_required_opening_balance as minRequiredOpeningBalance, ");
            sqlBuilder.append("sp.lockin_period_frequency as lockinPeriodFrequency,");
            sqlBuilder.append("sp.lockin_period_frequency_enum as lockinPeriodFrequencyType, ");
            sqlBuilder.append("sp.withdrawal_fee_for_transfer as withdrawalFeeForTransfers, ");
            sqlBuilder.append("sp.min_balance_for_interest_calculation as minBalanceForInterestCalculation, ");
            sqlBuilder.append("sp.allow_overdraft as allowOverdraft, ");
            sqlBuilder.append("sp.overdraft_limit as overdraftLimit, ");
            sqlBuilder.append("sp.nominal_annual_interest_rate_overdraft as nominalAnnualInterestRateOverdraft, ");
            sqlBuilder.append("sp.min_overdraft_for_interest_calculation as minOverdraftForInterestCalculation, ");
            sqlBuilder.append("sp.withhold_tax as withHoldTax,");
            sqlBuilder.append("tg.id as taxGroupId, tg.name as taxGroupName, ");

            sqlBuilder.append("sp.num_of_credit_transaction as numOfCreditTransaction,");
            sqlBuilder.append("sp.num_of_debit_transaction as numOfDebitTransaction,");
            sqlBuilder.append("sp.min_required_balance as minRequiredBalance, ");
            sqlBuilder.append("sp.enforce_min_required_balance as enforceMinRequiredBalance, ");
            sqlBuilder.append("sp.max_allowed_lien_limit as maxAllowedLienLimit, ");
            sqlBuilder.append("sp.use_floating_interest_rate as useFloatingInterestRate, ");
            sqlBuilder.append("sp.is_lien_allowed as lienAllowed, ");
            sqlBuilder.append("sp.withdrawal_frequency as withdrawalFrequency, ");
            sqlBuilder.append("sp.withdrawal_frequency_enum as withdrawalFrequencyEnum, ");
            sqlBuilder.append("sp.post_overdraft_interest_on_deposit as postOverdraftInterestOnDeposit ");
            sqlBuilder.append("from m_savings_product sp ");
            sqlBuilder.append("join m_currency curr on curr.code = sp.currency_code ");
            sqlBuilder.append("left join m_tax_group tg on tg.id = sp.tax_group_id  ");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public SavingsAccountData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long productId = rs.getLong("productId");
            final String productName = rs.getString("productName");

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("currencyNameCode");
            final String currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
            final Integer currencyDigits = JdbcSupport.getInteger(rs, "currencyDigits");
            final Integer inMultiplesOf = JdbcSupport.getInteger(rs, "inMultiplesOf");
            final CurrencyData currency = new CurrencyData(currencyCode, currencyName, currencyDigits, inMultiplesOf, currencyDisplaySymbol,
                    currencyNameCode);

            final BigDecimal nominalAnnualIterestRate = rs.getBigDecimal("nominalAnnualIterestRate");

            final EnumOptionData interestCompoundingPeriodType = SavingsEnumerations.compoundingInterestPeriodType(
                    SavingsCompoundingInterestPeriodType.fromInt(JdbcSupport.getInteger(rs, "interestCompoundingPeriodType")));

            final EnumOptionData interestPostingPeriodType = SavingsEnumerations.interestPostingPeriodType(
                    SavingsPostingInterestPeriodType.fromInt(JdbcSupport.getInteger(rs, "interestPostingPeriodType")));

            final EnumOptionData interestCalculationType = SavingsEnumerations
                    .interestCalculationType(SavingsInterestCalculationType.fromInt(JdbcSupport.getInteger(rs, "interestCalculationType")));

            final EnumOptionData interestCalculationDaysInYearType = SavingsEnumerations.interestCalculationDaysInYearType(
                    SavingsInterestCalculationDaysInYearType.fromInt(JdbcSupport.getInteger(rs, "interestCalculationDaysInYearType")));

            final BigDecimal minRequiredOpeningBalance = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "minRequiredOpeningBalance");

            final Integer lockinPeriodFrequency = JdbcSupport.getInteger(rs, "lockinPeriodFrequency");
            EnumOptionData lockinPeriodFrequencyType = null;
            final Integer lockinPeriodFrequencyTypeValue = JdbcSupport.getInteger(rs, "lockinPeriodFrequencyType");
            if (lockinPeriodFrequencyTypeValue != null) {
                final SavingsPeriodFrequencyType lockinPeriodType = SavingsPeriodFrequencyType.fromInt(lockinPeriodFrequencyTypeValue);
                lockinPeriodFrequencyType = SavingsEnumerations.lockinPeriodFrequencyType(lockinPeriodType);
            }

            final boolean withdrawalFeeForTransfers = rs.getBoolean("withdrawalFeeForTransfers");

            final boolean allowOverdraft = rs.getBoolean("allowOverdraft");
            final BigDecimal overdraftLimit = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "overdraftLimit");
            final BigDecimal nominalAnnualInterestRateOverdraft = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
                    "nominalAnnualInterestRateOverdraft");
            final BigDecimal minOverdraftForInterestCalculation = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
                    "minOverdraftForInterestCalculation");

            final BigDecimal minRequiredBalance = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "minRequiredBalance");
            final boolean enforceMinRequiredBalance = rs.getBoolean("enforceMinRequiredBalance");
            final BigDecimal maxAllowedLienLimit = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "maxAllowedLienLimit");
            final boolean lienAllowed = rs.getBoolean("lienAllowed");
            final BigDecimal minBalanceForInterestCalculation = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs,
                    "minBalanceForInterestCalculation");

            final boolean withHoldTax = rs.getBoolean("withHoldTax");
            final Long taxGroupId = JdbcSupport.getLong(rs, "taxGroupId");
            final String taxGroupName = rs.getString("taxGroupName");
            TaxGroupData taxGroupData = null;
            if (taxGroupId != null) {
                taxGroupData = TaxGroupData.lookup(taxGroupId, taxGroupName);
            }

            Long clientId = null;
            String clientName = null;
            if (this.client != null) {
                clientId = this.client.id();
                clientName = this.client.displayName();
            }

            Long groupId = null;
            String groupName = null;
            if (this.group != null) {
                groupId = this.group.getId();
                groupName = this.group.getName();
            }

            final Long fieldOfficerId = null;
            final String fieldOfficerName = null;
            final SavingsAccountStatusEnumData status = null;
            // final LocalDate annualFeeNextDueDate = null;
            final SavingsAccountSummaryData summary = null;
            final BigDecimal onHoldFunds = null;
            final BigDecimal savingsAmountOnHold = null;

            final SavingsAccountSubStatusEnumData subStatus = null;
            final String reasonForBlock = null;
            final LocalDate lastActiveTransactionDate = null;
            final boolean isDormancyTrackingActive = false;
            final Integer daysToInactive = null;
            final Integer daysToDormancy = null;
            final Integer daysToEscheat = null;
            final Long numOfCreditTransaction = JdbcSupport.getLong(rs, "numOfCreditTransaction");
            final Long numOfDebitTransaction = JdbcSupport.getLong(rs, "numOfDebitTransaction");

            final boolean useFloatingInterestRate = rs.getBoolean("useFloatingInterestRate");

            final SavingsAccountApplicationTimelineData timeline = SavingsAccountApplicationTimelineData.templateDefault();
            final EnumOptionData depositType = null;
            final Integer withdrawalFrequency = JdbcSupport.getInteger(rs, "withdrawalFrequency");

            EnumOptionData withdrawalFrequencyEnum = null;
            final Integer withdrawalFrequencyEnumValue = JdbcSupport.getInteger(rs, "withdrawalFrequencyEnum");
            if (withdrawalFrequencyEnumValue != null) {
                withdrawalFrequencyEnum = SavingsEnumerations
                        .withdrawalFrequency(WithdrawalFrequency.fromInt(withdrawalFrequencyEnumValue));
            }
            final boolean postOverdraftInterestOnDeposit = rs.getBoolean("postOverdraftInterestOnDeposit");

            SavingsAccountData savingsAccountData = SavingsAccountData.instance(null, null, depositType, null, groupId, groupName, clientId,
                    clientName, productId, productName, fieldOfficerId, fieldOfficerName, status, subStatus, reasonForBlock, timeline,
                    currency, nominalAnnualIterestRate, interestCompoundingPeriodType, interestPostingPeriodType, interestCalculationType,
                    interestCalculationDaysInYearType, minRequiredOpeningBalance, lockinPeriodFrequency, lockinPeriodFrequencyType,
                    withdrawalFeeForTransfers, summary, allowOverdraft, overdraftLimit, minRequiredBalance, enforceMinRequiredBalance,
                    maxAllowedLienLimit, lienAllowed, minBalanceForInterestCalculation, onHoldFunds, nominalAnnualInterestRateOverdraft,
                    minOverdraftForInterestCalculation, withHoldTax, taxGroupData, lastActiveTransactionDate, isDormancyTrackingActive,
                    daysToInactive, daysToDormancy, daysToEscheat, savingsAmountOnHold, numOfCreditTransaction, numOfDebitTransaction, null,
                    null, null, null, null, null);
            savingsAccountData.setUseFloatingInterestRate(useFloatingInterestRate);
            savingsAccountData.setWithdrawalFrequency(withdrawalFrequency);
            savingsAccountData.setWithdrawalFrequencyEnum(withdrawalFrequencyEnum);
            savingsAccountData.setPostOverdraftInterestOnDeposit(postOverdraftInterestOnDeposit);
            return savingsAccountData;
        }
    }

    private static final class SavingsAccountBlockNarrationHistoryMapper implements RowMapper<SavingsAccountBlockNarrationHistoryData> {

        private final String schemaSql;

        SavingsAccountBlockNarrationHistoryMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append(
                    "blockNarrationHistory.id as id, blockNarrationHistory.block_narration_id as blockNarrationId, au.username as submittedByUsername, ");
            sqlBuilder.append(
                    "blockNarrationHistory.start_date as startDate, blockNarrationHistory.end_date as endDate, blockNarrationHistory.sub_status as subStatus, ");
            sqlBuilder.append(
                    "blockNarrationHistory.block_narration_comment as blockNarrationComment, cvn.code_value as blockNarrationValue ");
            sqlBuilder.append("from m_savings_account sa ");
            sqlBuilder.append(
                    "join m_savings_account_block_narration_history blockNarrationHistory on blockNarrationHistory.account_id = sa.id ");
            sqlBuilder.append("left join m_appuser au on au.id=blockNarrationHistory.createdby_id  ");
            sqlBuilder.append("left join m_code_value cvn on cvn.id = blockNarrationHistory.block_narration_id  ");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public SavingsAccountBlockNarrationHistoryData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
                throws SQLException {

            final Long id = rs.getLong("id");
            final Long blockNarrationId = rs.getLong("blockNarrationId");
            final String blockNarrationValue = rs.getString("blockNarrationValue");
            final String blockNarrationComment = rs.getString("blockNarrationComment");
            final String subStatus = rs.getString("subStatus");
            final String submittedByUsername = rs.getString("submittedByUsername");
            final Timestamp startDate = rs.getTimestamp("startDate");
            final LocalDate endDate = JdbcSupport.getLocalDate(rs, "endDate");

            return new SavingsAccountBlockNarrationHistoryData(id, startDate, endDate, blockNarrationComment, blockNarrationId,
                    blockNarrationValue, subStatus, submittedByUsername);

        }
    }

    private static final class RecurringMissedTargetTemplateMapper implements RowMapper<RecurringMissedTargetData> {

        private final String schemaSql;

        RecurringMissedTargetTemplateMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append(" sa.id AS id, sa.account_no AS accountNo,sa.status_enum AS statusEnum, sa.total_deposits_derived ");
            sqlBuilder.append(
                    " AS  depositTillDate ,sa.total_interest_posted_derived AS totalInterest , cl.deposit_amount AS principalAmount, ");
            sqlBuilder.append(
                    " cl.maturity_date AS maturityDate, sp.add_penalty_on_missed_target_savings AS addPenaltyOnMissedTargetSavings ,sp.id   AS productId ");
            sqlBuilder.append(" FROM m_savings_account sa ");
            sqlBuilder.append(" INNER JOIN m_deposit_account_term_and_preclosure cl  ON sa.id = cl.savings_account_id ");
            sqlBuilder.append(" INNER JOIN m_savings_product sp ON sa.product_id = sp.id ");
            sqlBuilder.append(" WHERE sa.id = ? AND sa.status_enum = 300 AND ");
            sqlBuilder.append(" sp.add_penalty_on_missed_target_savings = true ");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public RecurringMissedTargetData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Integer savingsId = JdbcSupport.getInteger(rs, "id");
            final String accountNo = rs.getString("accountNo");
            final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
            final BigDecimal depositTillDate = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "depositTillDate");
            final BigDecimal totalInterest = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalInterest");
            final BigDecimal principalAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalAmount");
            final LocalDate maturityDate = JdbcSupport.getLocalDate(rs, "maturityDate");
            final boolean addPenaltyOnMissedTargetSavings = rs.getBoolean("addPenaltyOnMissedTargetSavings");
            final Long productId = JdbcSupport.getLong(rs, "productId");

            return new RecurringMissedTargetData(savingsId, accountNo, statusEnum, depositTillDate, totalInterest, principalAmount,
                    maturityDate, addPenaltyOnMissedTargetSavings, productId);
        }
    }

}
