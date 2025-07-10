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
package org.apache.fineract.accounting.provisioning.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.provisioning.data.LoanData;
import org.apache.fineract.accounting.provisioning.data.LoanProductProvisioningEntryData;
import org.apache.fineract.accounting.provisioning.data.ProvisioningEntryData;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvisioningEntriesReadPlatformServiceImpl implements ProvisioningEntriesReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    private final PaginationHelper loanProductProvisioningEntryDataPaginationHelper;
    private final PaginationHelper provisioningEntryDataPaginationHelper;
    private final DatabaseSpecificSQLGenerator sqlGenerator;

    @Override
    public Collection<LoanProductProvisioningEntryData> retrieveLoanProductsProvisioningData(LocalDate date) {
        String formattedDate = DateUtils.DEFAULT_DATE_FORMATER.format(date);
        LoanProductProvisioningEntryMapper mapper = new LoanProductProvisioningEntryMapper(sqlGenerator);
        final String sql = mapper.schema();
        return this.jdbcTemplate.query(sql, mapper, formattedDate, formattedDate, formattedDate, formattedDate, formattedDate, formattedDate,formattedDate, formattedDate, formattedDate, formattedDate);
    }

    private static final class LoanProductProvisioningEntryMapper implements RowMapper<LoanProductProvisioningEntryData> {

        private final StringBuilder sqlQuery;

        private LoanProductProvisioningEntryMapper(DatabaseSpecificSQLGenerator sqlGenerator) {

            sqlQuery = new StringBuilder().append("SELECT DISTINCT ")
                    .append("(CASE WHEN loan.loan_type_enum = 1 THEN mclient.office_id ELSE mgroup.office_id END) AS office_id, ")
                    .append("loan.loan_type_enum, ")
                    .append("pcd.criteria_id AS criteriaid, ")
                    .append("loan.product_id, loan.currency_code, loan.id AS loanId, loan.account_no, ")
                    .append("IFNULL(CASE WHEN max_date = min_date OR max_date > min_date ")
                    .append("THEN DATEDIFF(DATE(?), min_date) ELSE DATEDIFF(max_date, min_date) END, 0) AS numberofdaysoverdue, ")
                    .append("sch.duedate, pcd.category_id, pcd.provision_percentage, ")
                    .append("CASE WHEN loan.loan_status_id IN (300) THEN ")
                    .append("(loan.principal_disbursed_derived + loan.interest_charged_derived + IFNULL(chargesTbl.charge_amount,0) ")
                    .append("- IFNULL(paymentTbl.princ_paid, 0) - IFNULL(paymentTbl.int_paid, 0) - IFNULL(chargesTbl.charge_paid, 0) ")
                    .append("- IFNULL(waiverTbl.int_waived, 0) - IFNULL(chargesTbl.charge_waived, 0)) ELSE 0 END AS outstandingbalance, ")
                    .append("pcd.liability_account, pcd.expense_account ")
                    .append("FROM m_loan_repayment_schedule sch ")
                    .append("LEFT JOIN m_loan loan ON sch.loan_id = loan.id ")
                    .append("LEFT JOIN (SELECT COUNT(*) AS Instalment, lrs.loan_id, MAX(lrs.duedate) AS max_date, MIN(lrs.duedate) AS min_date ")
                    .append("FROM m_loan_repayment_schedule lrs ")
                    .append("WHERE lrs.duedate <= DATE(?) ")
                    .append("AND ((lrs.completed_derived = FALSE AND lrs.obligations_met_on_date IS NULL) OR lrs.obligations_met_on_date > DATE(?)) ")
                    .append("GROUP BY lrs.loan_id) as repaymentInstalmentTbl ON repaymentInstalmentTbl.loan_id = loan.id ")
                    .append("JOIN m_loanproduct_provisioning_mapping lpm ON lpm.product_id = loan.product_id ")
                    .append("JOIN m_provisioning_criteria_definition pcd ON pcd.criteria_id = lpm.criteria_id ")
                    .append("AND pcd.min_age <= IFNULL(CASE WHEN max_date = min_date OR max_date > min_date ")
                    .append("THEN DATEDIFF(DATE(?), min_date) ELSE DATEDIFF(max_date, min_date) END, 0) ")
                    .append("AND IFNULL(CASE WHEN max_date = min_date OR max_date > min_date ")
                    .append("THEN DATEDIFF(DATE(?), min_date) ELSE DATEDIFF(max_date, min_date) END, 0) <= pcd.max_age ")
                    .append("LEFT JOIN m_client mclient ON mclient.id = loan.client_id ")
                    .append("LEFT JOIN m_group mgroup ON mgroup.id = loan.group_id ")
                    .append("LEFT JOIN (SELECT t.loan_id, SUM(IFNULL(t.amount, 0)) AS Actual_paid, SUM(IFNULL(t.principal_portion_derived, 0)) AS princ_paid, ")
                    .append("SUM(IFNULL(t.interest_portion_derived, 0)) AS int_paid ")
                    .append("FROM m_loan_transaction t ")
                    .append("WHERE  (t.transaction_type_enum = 2 or t.transaction_type_enum = 5) AND t.is_reversed = 0 AND (DATE(t.transaction_date) <= DATE(?)) ")
                    .append("GROUP BY t.loan_id) as paymentTbl ON paymentTbl.loan_id = loan.id ")
                    .append("LEFT JOIN (SELECT t.loan_id, SUM(IFNULL(t.interest_portion_derived, 0)) AS int_waived ")
                    .append("FROM m_loan_transaction t ")
                    .append("WHERE t.transaction_type_enum = 4 AND t.is_reversed = 0 AND (DATE(t.transaction_date) <= DATE(?)) ")
                    .append("GROUP BY t.loan_id) as waiverTbl ON waiverTbl.loan_id = loan.id ")
                    .append("LEFT JOIN (SELECT t.loan_id, SUM(IFNULL(t.amount,0)) AS charge_amount, ")
                    .append("SUM(IFNULL(t.amount_waived_derived, 0)) AS charge_waived, SUM(IFNULL(t.amount_paid_derived, 0)) AS charge_paid ")
                    .append("FROM m_loan_charge t ")
                    .append("WHERE (DATE(t.due_for_collection_as_of_date) <= DATE(?)) ")
                    .append("GROUP BY t.loan_id) AS chargesTbl ON chargesTbl.loan_id = loan.id ")
                    .append("WHERE loan.loan_status_id IN (300) ")
                    .append("AND sch.duedate = (SELECT MIN(sch1.duedate) FROM m_loan_repayment_schedule sch1 ")
                    .append("WHERE sch1.loan_id = loan.id AND sch1.duedate <= DATE(?) ")
                    .append("AND ((sch1.completed_derived = FALSE AND sch1.obligations_met_on_date IS NULL) ")
                    .append("OR sch1.obligations_met_on_date > DATE(?))) order by numberofdaysoverdue");

        }

        @Override
        @SuppressWarnings("unused")
        public LoanProductProvisioningEntryData mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long officeId = rs.getLong("office_id");
            Long productId = rs.getLong("product_id");
            String currentcyCode = rs.getString("currency_code");
            Long overdueDays = rs.getLong("numberofdaysoverdue");
            Long categoryId = rs.getLong("category_id");
            BigDecimal percentage = rs.getBigDecimal("provision_percentage");
            BigDecimal outstandingBalance = rs.getBigDecimal("outstandingbalance");
            Long liabilityAccountCode = rs.getLong("liability_account");
            Long expenseAccountCode = rs.getLong("expense_account");
            Long criteriaId = rs.getLong("criteriaid");
            Long historyId = null;
            Long loanAccountNo = rs.getLong("loanId");

            return new LoanProductProvisioningEntryData(historyId, officeId, currentcyCode, productId, categoryId, overdueDays, percentage,
                    outstandingBalance, liabilityAccountCode, expenseAccountCode, criteriaId, loanAccountNo);
        }

        public String schema() {
            return sqlQuery.toString();
        }
    }

    @Override
    public ProvisioningEntryData retrieveProvisioningEntryData(Long entryId) {
        ProvisioningEntryDataMapperWithSumReserved mapper1 = new ProvisioningEntryDataMapperWithSumReserved();
        final String sql = "select" + mapper1.getSchema() + " where entry.id = ? group by entry.id, created.username, modified.username";
        return this.jdbcTemplate.queryForObject(sql, mapper1, entryId);
    }

    private static final class ProvisioningEntryDataMapper implements RowMapper<ProvisioningEntryData> {

        private final StringBuilder sqlQuery = new StringBuilder()
                .append(" entry.id, entry.journal_entry_created, entry.createdby_id, entry.created_date, created.username as createduser,")
                .append("entry.lastmodifiedby_id, modified.username as modifieduser, entry.lastmodified_date ")
                .append("from m_provisioning_history entry ").append("left JOIN m_appuser created ON created.id = entry.createdby_id ")
                .append("left JOIN m_appuser modified ON modified.id = entry.lastmodifiedby_id ");

        @Override
        @SuppressWarnings("unused")
        public ProvisioningEntryData mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long id = rs.getLong("id");
            Boolean journalEntry = rs.getBoolean("journal_entry_created");
            Long createdById = rs.getLong("createdby_id");
            String createdUser = rs.getString("createduser");
            Date createdDate = rs.getDate("created_date");
            Long modifiedById = rs.getLong("lastmodifiedby_id");
            String modifieUser = rs.getString("modifieduser");
            BigDecimal totalReservedAmount = null;
            LocalDate createdLocalDate = createdDate != null ? createdDate.toLocalDate() : null;
            return new ProvisioningEntryData(id, journalEntry, createdById, createdUser, createdLocalDate, modifiedById, modifieUser,
                    totalReservedAmount);
        }

        public String getSchema() {
            return sqlQuery.toString();
        }

    }

    private static final class LoanProductProvisioningEntryRowMapper implements RowMapper<LoanProductProvisioningEntryData> {

        private final StringBuilder sqlQuery = new StringBuilder().append(
                " entry.id, entry.history_id as historyId, entry.office_id, entry.criteria_id as criteriaid, office.name as officename, product.name as productname, entry.product_id, ")
                .append("loan.id as loanId, loan.account_no as loanAccountNo, loan.total_outstanding_derived as outstandingBalance, loan.total_outstanding_derived * pcd.provision_percentage/100 as provisioningAmount, ")
                .append("entry.category_id, category.category_name, liability.id as liabilityid, liability.gl_code as liabilitycode, liability.name as liabilityname, ")
                .append("expense.id as expenseid, expense.gl_code as expensecode, expense.name as expensename, entry.currency_code, entry.overdue_in_days, entry.reseve_amount from m_loanproduct_provisioning_entry entry ")
                .append("left join m_office office ON office.id = entry.office_id ")
                .append("left join m_product_loan product ON product.id = entry.product_id ")
                .append("left join m_provision_category category ON category.id = entry.category_id ")
                .append("left join acc_gl_account liability ON liability.id = entry.liability_account ")
                .append("left join acc_gl_account expense ON expense.id = entry.expense_account ")
                .append("left join m_loanproduct_provisioning_entry_loans mlpel on mlpel.loanproduct_provision_entry_id = entry.id " +
                        "left join m_loan loan on loan.id = mlpel.loan_id " +
                        "left join m_provisioning_criteria_definition pcd ON pcd.category_id = entry.category_id ");

        @Override
        @SuppressWarnings("unused")
        public LoanProductProvisioningEntryData mapRow(ResultSet rs, int rowNum) throws SQLException {

            Long id = rs.getLong("id");
            Long historyId = rs.getLong("historyId");
            Long officeId = rs.getLong("office_id");
            String officeName = rs.getString("officename");
            Long productId = rs.getLong("product_id");
            String productName = rs.getString("productname");
            String currentcyCode = rs.getString("currency_code");
            Long overdueDays = rs.getLong("overdue_in_days");
            Long categoryId = rs.getLong("category_id");
            String categoryName = rs.getString("category_name");
            BigDecimal amountreserved = rs.getBigDecimal("reseve_amount");
            Long liabilityAccountCode = rs.getLong("liabilityid");
            String liabilityAccountglCode = rs.getString("liabilitycode");
            String expenseAccountglCode = rs.getString("expensecode");
            Long expenseAccountCode = rs.getLong("expenseid");
            Long criteriaId = rs.getLong("criteriaid");
            String liabilityAccountName = rs.getString("liabilityname");
            String expenseAccountName = rs.getString("expensename");

            List<LoanData> loans = new ArrayList<>();

            LoanProductProvisioningEntryData data = new LoanProductProvisioningEntryData(historyId, officeId, officeName, currentcyCode, productId, productName, categoryId,
                    categoryName, overdueDays, amountreserved, liabilityAccountCode, liabilityAccountglCode, liabilityAccountName,
                    expenseAccountCode, expenseAccountglCode, expenseAccountName, criteriaId, loans);

            do {
                Long loanId = rs.getLong("loanId");
                String accountNo = rs.getString("loanAccountNo");
                BigDecimal outstandingBalance= rs.getBigDecimal("outstandingBalance");
                BigDecimal provisioningAmount =rs.getBigDecimal("provisioningAmount");

                loans.add(new LoanData(loanId,accountNo,outstandingBalance,provisioningAmount));
            } while (rs.next() && rs.getInt("id") == id);

            return data;
        }

        public String getSchema() {
            return sqlQuery.toString();
        }
    }

    private static final class ProvisioningEntryDataMapperWithSumReserved implements RowMapper<ProvisioningEntryData> {

        private final StringBuilder sqlQuery = new StringBuilder()
                .append(" entry.id, journal_entry_created, createdby_id, created_date, created.username as createduser,")
                .append("lastmodifiedby_id, modified.username as modifieduser, lastmodified_date, SUM(reserved.reseve_amount) as totalreserved ")
                .append("from m_provisioning_history entry ")
                .append("JOIN m_loanproduct_provisioning_entry reserved on entry.id = reserved.history_id ")
                .append("left JOIN m_appuser created ON created.id = entry.createdby_id ")
                .append("left JOIN m_appuser modified ON modified.id = entry.lastmodifiedby_id ");

        @Override
        @SuppressWarnings("unused")
        public ProvisioningEntryData mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long id = rs.getLong("id");
            Boolean journalEntry = rs.getBoolean("journal_entry_created");
            Long createdById = rs.getLong("createdby_id");
            String createdUser = rs.getString("createduser");
            Date createdDate = rs.getDate("created_date");
            Long modifiedById = rs.getLong("lastmodifiedby_id");
            String modifieUser = rs.getString("modifieduser");
            BigDecimal totalReservedAmount = rs.getBigDecimal("totalreserved");
            LocalDate createdLocalDate = createdDate != null ? createdDate.toLocalDate() : null;
            return new ProvisioningEntryData(id, journalEntry, createdById, createdUser, createdLocalDate, modifiedById, modifieUser,
                    totalReservedAmount);
        }

        public String getSchema() {
            return sqlQuery.toString();
        }

    }

    @Override
    public Page<ProvisioningEntryData> retrieveAllProvisioningEntries(Integer offset, Integer limit) {
        ProvisioningEntryDataMapper mapper = new ProvisioningEntryDataMapper();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select " + sqlGenerator.calcFoundRows() + " ");
        sqlBuilder.append(mapper.getSchema());
        sqlBuilder.append(" order by entry.created_date");
        if (limit != null) {
            sqlBuilder.append(" limit ").append(limit);
        }
        if (offset != null) {
            sqlBuilder.append(" offset ").append(offset);
        }

        Object[] whereClauseItemsitems = new Object[] {};
        return this.provisioningEntryDataPaginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), whereClauseItemsitems,
                mapper);
    }

    @Override
    public ProvisioningEntryData retrieveProvisioningEntryData(String date) {
        ProvisioningEntryDataMapper mapper1 = new ProvisioningEntryDataMapper();
        date = date + "%";
        final String sql1 = "select " + mapper1.getSchema() + " where entry.created_date like ? ";
        ProvisioningEntryData data = null;
        try {
            data = this.jdbcTemplate.queryForObject(sql1, mapper1, date); // NOSONAR
        } catch (EmptyResultDataAccessException e) {
            log.error("Problem occurred in retrieveProvisioningEntryData function", e);
        }

        return data;
    }

    @Override
    public ProvisioningEntryData retrieveProvisioningEntryDataByCriteriaId(Long criteriaId) {
        ProvisioningEntryData data = null;
        LoanProductProvisioningEntryRowMapper mapper = new LoanProductProvisioningEntryRowMapper();
        final String sql = "select " + mapper.getSchema() + " where entry.criteria_id = ?";
        Collection<LoanProductProvisioningEntryData> entries = this.jdbcTemplate.query(sql, mapper, criteriaId); // NOSONAR
        if (entries != null && entries.size() > 0) {
            Long entryId = ((LoanProductProvisioningEntryData) entries.toArray()[0]).getHistoryId();
            ProvisioningEntryDataMapper mapper1 = new ProvisioningEntryDataMapper();
            final String sql1 = "select " + mapper1.getSchema() + " where entry.id = ?";
            data = this.jdbcTemplate.queryForObject(sql1, mapper1, entryId); // NOSONAR
            data.setEntries(entries);
        }
        return data;
    }

    @Override
    public ProvisioningEntryData retrieveExistingProvisioningIdDateWithJournals() {
        ProvisioningEntryData data = null;
        ProvisioningEntryIdDateRowMapper mapper = new ProvisioningEntryIdDateRowMapper();
        try {
            data = this.jdbcTemplate.queryForObject(mapper.schema(), mapper, new Object[] {});
        } catch (EmptyResultDataAccessException e) {
            data = null;
        }
        return data;
    }

    private static final class ProvisioningEntryIdDateRowMapper implements RowMapper<ProvisioningEntryData> {

        StringBuilder buff = new StringBuilder().append("select history1.id, history1.created_date from m_provisioning_history history1 ")
                .append("where history1.created_date = (select max(history2.created_date) from m_provisioning_history history2 ")
                .append("where history2.journal_entry_created='1')");

        @Override
        public ProvisioningEntryData mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long id = rs.getLong("id");
            Date createdDate = rs.getDate("created_date");
            Long createdBy = null;
            String createdName = null;
            Long modifiedBy = null;
            String modifiedName = null;
            BigDecimal totalReservedAmount = null;
            LocalDate createdLocalDate = createdDate != null ? createdDate.toLocalDate() : null;
            return new ProvisioningEntryData(id, Boolean.TRUE, createdBy, createdName, createdLocalDate, modifiedBy, modifiedName,
                    totalReservedAmount);
        }

        public String schema() {
            return buff.toString();
        }
    }

    @Override
    public Page<LoanProductProvisioningEntryData> retrieveProvisioningEntries(SearchParameters searchParams) {
        LoanProductProvisioningEntryRowMapper mapper = new LoanProductProvisioningEntryRowMapper();
        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select " + sqlGenerator.calcFoundRows() + " ");
        sqlBuilder.append(mapper.getSchema());
        String whereClose = " where ";
        List<Object> items = new ArrayList<>();

        if (searchParams.isProvisioningEntryIdPassed()) {
            sqlBuilder.append(whereClose + " entry.history_id = ?");
            items.add(searchParams.getProvisioningEntryId());
            whereClose = " and ";
        }

        if (searchParams.isOfficeIdPassed()) {
            sqlBuilder.append(whereClose + " entry.office_id = ?");
            items.add(searchParams.getOfficeId());
            whereClose = " and ";
        }

        if (searchParams.isProductIdPassed()) {
            sqlBuilder.append(whereClose + " entry.product_id = ?");
            items.add(searchParams.getProductId());
            whereClose = " and ";
        }

        if (searchParams.isCategoryIdPassed()) {
            sqlBuilder.append(whereClose + " entry.category_id = ?");
            items.add(searchParams.getCategoryId());
        }
        sqlBuilder.append(" order by entry.id");

        if (searchParams.isLimited()) {
            sqlBuilder.append(" limit ").append(searchParams.getLimit());
            if (searchParams.isOffset()) {
                sqlBuilder.append(" offset ").append(searchParams.getOffset());
            }
        }
        Object[] whereClauseItemsitems = items.toArray();
        return this.loanProductProvisioningEntryDataPaginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(),
                whereClauseItemsitems, mapper);
    }

}
