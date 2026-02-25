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
package org.apache.fineract.portfolio.tax.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.accounting.common.AccountingDropdownReadPlatformService;
import org.apache.fineract.accounting.common.AccountingEnumerations;
import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.portfolio.tax.data.TaxComponentData;
import org.apache.fineract.portfolio.tax.data.TaxComponentHistoryData;
import org.apache.fineract.portfolio.tax.data.TaxGroupData;
import org.apache.fineract.portfolio.tax.data.TaxGroupMappingsData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class TaxReadPlatformServiceImpl implements TaxReadPlatformService {

    final TaxComponentMapper taxComponentMapper = new TaxComponentMapper();
    final TaxGroupMapper taxGroupMapper = new TaxGroupMapper();
    final TaxComponentLookUpMapper taxComponentLookUpMapper = new TaxComponentLookUpMapper();
    final TaxGroupLookUpMapper taxGroupLookUpMapper = new TaxGroupLookUpMapper();

    private final JdbcTemplate jdbcTemplate;
    private final AccountingDropdownReadPlatformService accountingDropdownReadPlatformService;

    @Autowired
    public TaxReadPlatformServiceImpl(final JdbcTemplate jdbcTemplate,
            final AccountingDropdownReadPlatformService accountingDropdownReadPlatformService) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountingDropdownReadPlatformService = accountingDropdownReadPlatformService;
    }

    @Override
    public Collection<TaxComponentData> retrieveAllTaxComponents() {
        String sql = "select " + this.taxComponentMapper.getSchema();
        // return this.jdbcTemplate.query(sql, this.taxComponentMapper); // NOSONAR
        return retrieveTaxComponentData(sql, null);
    }

    @Override
    public TaxComponentData retrieveTaxComponentData(final Long id) {
        String sql = "select " + this.taxComponentMapper.getSchema() + " where tc.id=?";
        // return this.jdbcTemplate.queryForObject(sql, this.taxComponentMapper, new Object[] { id }); // NOSONAR
        List<TaxComponentData> taxComponentData = (List<TaxComponentData>) retrieveTaxComponentData(sql, new Object[] { id });
        if (taxComponentData != null && !taxComponentData.isEmpty()) {
            return taxComponentData.get(0);
        } else {
            return null;
        }
    }

    @Override
    public TaxComponentData retrieveTaxComponentTemplate() {
        return TaxComponentData.template(this.accountingDropdownReadPlatformService.retrieveAccountMappingOptions(),
                this.accountingDropdownReadPlatformService.retrieveGLAccountTypeOptions());
    }

    @Override
    public Collection<TaxGroupData> retrieveAllTaxGroups() {
        String sql = "select " + this.taxGroupMapper.getSchema() + " order by tg.id ";
        // return this.jdbcTemplate.query(sql, this.taxGroupMapper); // NOSONAR
        return retrieveTaxGroupData(sql, null);
    }

    @Override
    public TaxGroupData retrieveTaxGroupData(final Long id) {
        String sql = "select " + this.taxGroupMapper.getSchema() + " where tg.id=?";
        // return this.jdbcTemplate.queryForObject(sql, this.taxGroupMapper, new Object[] { id }); // NOSONAR
        List<TaxGroupData> taxGroupData = (List<TaxGroupData>) retrieveTaxGroupData(sql, new Object[] { id });
        if (taxGroupData != null && !taxGroupData.isEmpty()) {
            return taxGroupData.get(0);
        } else {
            return null;
        }
    }

    @Override
    public TaxGroupData retrieveTaxGroupWithTemplate(final Long id) {
        TaxGroupData taxGroupData = retrieveTaxGroupData(id);
        taxGroupData = TaxGroupData.template(taxGroupData, retrieveTaxComponentsForLookUp());
        return taxGroupData;
    }

    @Override
    public TaxGroupData retrieveTaxGroupTemplate() {
        return TaxGroupData.template(retrieveTaxComponentsForLookUp());
    }

    private Collection<TaxComponentData> retrieveTaxComponentsForLookUp() {
        String sql = "select " + this.taxComponentLookUpMapper.getSchema();
        return this.jdbcTemplate.query(sql, this.taxComponentLookUpMapper); // NOSONAR
    }

    @Override
    public Collection<TaxGroupData> retrieveTaxGroupsForLookUp() {
        String sql = "select " + this.taxGroupLookUpMapper.getSchema();
        return this.jdbcTemplate.query(sql, this.taxGroupLookUpMapper); // NOSONAR
    }

    private Collection<TaxComponentData> retrieveTaxComponentData(String sql, Object[] args) {
        return this.jdbcTemplate.query(sql, args, new ResultSetExtractor<Collection<TaxComponentData>>() {

            @Override
            public Collection<TaxComponentData> extractData(ResultSet rs) throws SQLException, DataAccessException {

                List<TaxComponentData> list = new ArrayList<TaxComponentData>();
                while (rs.next()) {
                    final Long id = rs.getLong("id");
                    if (!list.isEmpty() && id.equals(list.get(list.size() - 1).getId())) {
                        TaxComponentData taxData = list.get(list.size() - 1);
                        TaxComponentHistoryData historyData = null;
                        final BigDecimal historyPercentage = rs.getBigDecimal("historyPercentage");
                        final LocalDate historyStartDate = JdbcSupport.getLocalDate(rs, "historyStartDate");
                        final LocalDate historyEndDate = JdbcSupport.getLocalDate(rs, "historyEndDate");
                        historyData = new TaxComponentHistoryData(historyPercentage, historyStartDate, historyEndDate);
                        taxData.getTaxComponentHistories().add(historyData);
                    } else {
                        final String name = rs.getString("name");
                        final BigDecimal percentage = rs.getBigDecimal("percentage");
                        final Integer debitAccountTypeEnum = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "debitAccountTypeEnum");
                        EnumOptionData debitAccountType = null;
                        if (debitAccountTypeEnum != null) {
                            debitAccountType = AccountingEnumerations.gLAccountType(debitAccountTypeEnum);
                        }
                        GLAccountData debitAccountData = null;
                        if (debitAccountTypeEnum != null && debitAccountTypeEnum > 0) {
                            final Long debitAccountId = rs.getLong("debitAccountId");
                            final String debitAccountName = rs.getString("debitAccountName");
                            final String debitAccountGlCode = rs.getString("debitAccountGlCode");
                            debitAccountData = new GLAccountData(debitAccountId, debitAccountName, debitAccountGlCode);
                        }

                        final Integer creditAccountTypeEnum = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "creditAccountTypeEnum");
                        EnumOptionData creditAccountType = null;
                        if (creditAccountTypeEnum != null) {
                            creditAccountType = AccountingEnumerations.gLAccountType(creditAccountTypeEnum);
                        }
                        GLAccountData creditAccountData = null;
                        if (creditAccountTypeEnum != null && creditAccountTypeEnum > 0) {
                            final Long creditAccountId = rs.getLong("creditAccountId");
                            final String creditAccountName = rs.getString("creditAccountName");
                            final String creditAccountGlCode = rs.getString("creditAccountGlCode");
                            creditAccountData = new GLAccountData(creditAccountId, creditAccountName, creditAccountGlCode);
                        }
                        final LocalDate startDate = JdbcSupport.getLocalDate(rs, "startDate");

                        TaxComponentHistoryData historyData = null;
                        Collection<TaxComponentHistoryData> historyDatas = new ArrayList<>();
                        if (rs.getBigDecimal("historyPercentage") != null) {
                            final BigDecimal historyPercentage = rs.getBigDecimal("historyPercentage");
                            final LocalDate historyStartDate = JdbcSupport.getLocalDate(rs, "historyStartDate");
                            final LocalDate historyEndDate = JdbcSupport.getLocalDate(rs, "historyEndDate");
                            historyData = new TaxComponentHistoryData(historyPercentage, historyStartDate, historyEndDate);

                            historyDatas.add(historyData);
                        }
                        TaxComponentData taxData = TaxComponentData.instance(id, name, percentage, debitAccountType, debitAccountData,
                                creditAccountType, creditAccountData, startDate, historyDatas);
                        list.add(taxData);
                    }
                }
                return list;
            }
        });
    }

    private Collection<TaxGroupData> retrieveTaxGroupData(String sql, Object[] args) {
        return this.jdbcTemplate.query(sql, args, new ResultSetExtractor<Collection<TaxGroupData>>() {

            @Override
            public Collection<TaxGroupData> extractData(ResultSet rs) throws SQLException, DataAccessException {

                List<TaxGroupData> list = new ArrayList<TaxGroupData>();
                while (rs.next()) {
                    final Long id = rs.getLong("id");
                    if (!list.isEmpty() && id.equals(list.get(list.size() - 1).getId())) {
                        TaxGroupData taxGroupData = list.get(list.size() - 1);
                        TaxGroupMappingsData taxGroupMappingsData = null;

                        final Long mappingId = rs.getLong("mappingId");
                        final Long taxComponentId = rs.getLong("taxComponentId");
                        final String taxComponentName = rs.getString("taxComponentName");
                        final BigDecimal taxComponentPercentage = rs.getBigDecimal("taxComponentPercentage");
                        TaxComponentData componentData = TaxComponentData.lookupWithPercentage(taxComponentId, taxComponentName,
                                taxComponentPercentage);

                        final LocalDate startDate = JdbcSupport.getLocalDate(rs, "startDate");
                        final LocalDate endDate = JdbcSupport.getLocalDate(rs, "endDate");
                        taxGroupMappingsData = new TaxGroupMappingsData(mappingId, componentData, startDate, endDate);
                        taxGroupData.getTaxAssociations().add(taxGroupMappingsData);

                    } else {
                        final String name = rs.getString("name");
                        final Collection<TaxGroupMappingsData> taxAssociations = new ArrayList<>();
                        final Long mappingId = rs.getLong("mappingId");
                        final Long taxComponentId = rs.getLong("taxComponentId");
                        final String taxComponentName = rs.getString("taxComponentName");
                        final BigDecimal taxComponentPercentage = rs.getBigDecimal("taxComponentPercentage");
                        TaxComponentData componentData = TaxComponentData.lookupWithPercentage(taxComponentId, taxComponentName,
                                taxComponentPercentage);

                        final LocalDate startDate = JdbcSupport.getLocalDate(rs, "startDate");
                        final LocalDate endDate = JdbcSupport.getLocalDate(rs, "endDate");
                        TaxGroupMappingsData taxGroupMappingsData = new TaxGroupMappingsData(mappingId, componentData, startDate, endDate);
                        taxAssociations.add(taxGroupMappingsData);

                        TaxGroupData taxGroupData = TaxGroupData.instance(id, name, taxAssociations);
                        list.add(taxGroupData);
                    }
                }
                return list;
            }
        });
    }

    private static final class TaxComponentMapper implements RowMapper<TaxComponentData> {

        private final String schema;
        private TaxComponentHistoryDataMapper componentHistoryDataMapper = new TaxComponentHistoryDataMapper();

        TaxComponentMapper() {
            StringBuilder sb = new StringBuilder();
            sb.append("tc.id as id, tc.name as name,");
            sb.append("tc.percentage as percentage, tc.start_date as startDate,");
            sb.append("tc.debit_account_type_enum as debitAccountTypeEnum,");
            sb.append("dgl.id as debitAccountId, dgl.name as debitAccountName,  dgl.gl_code as debitAccountGlCode,");
            sb.append("tc.credit_account_type_enum as creditAccountTypeEnum,");
            sb.append("cgl.id as creditAccountId, cgl.name as creditAccountName,  cgl.gl_code as creditAccountGlCode,");
            sb.append("history.percentage as historyPercentage, history.start_date as historyStartDate,");
            sb.append("history.end_date as historyEndDate");
            sb.append(" from m_tax_component tc ");
            sb.append(" left join acc_gl_account dgl on dgl.id = tc.debit_account_id");
            sb.append(" left join acc_gl_account cgl on cgl.id = tc.credit_account_id");
            sb.append(" left join m_tax_component_history history on history.tax_component_id = tc.id");

            this.schema = sb.toString();
        }

        @Override
        public TaxComponentData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final BigDecimal percentage = rs.getBigDecimal("percentage");
            final Integer debitAccountTypeEnum = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "debitAccountTypeEnum");
            EnumOptionData debitAccountType = null;
            if (debitAccountTypeEnum != null) {
                debitAccountType = AccountingEnumerations.gLAccountType(debitAccountTypeEnum);
            }
            GLAccountData debitAccountData = null;
            if (debitAccountTypeEnum != null && debitAccountTypeEnum > 0) {
                final Long debitAccountId = rs.getLong("debitAccountId");
                final String debitAccountName = rs.getString("debitAccountName");
                final String debitAccountGlCode = rs.getString("debitAccountGlCode");
                debitAccountData = new GLAccountData(debitAccountId, debitAccountName, debitAccountGlCode);
            }

            final Integer creditAccountTypeEnum = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "creditAccountTypeEnum");
            EnumOptionData creditAccountType = null;
            if (creditAccountTypeEnum != null) {
                creditAccountType = AccountingEnumerations.gLAccountType(creditAccountTypeEnum);
            }
            GLAccountData creditAccountData = null;
            if (creditAccountTypeEnum != null && creditAccountTypeEnum > 0) {
                final Long creditAccountId = rs.getLong("creditAccountId");
                final String creditAccountName = rs.getString("creditAccountName");
                final String creditAccountGlCode = rs.getString("creditAccountGlCode");
                creditAccountData = new GLAccountData(creditAccountId, creditAccountName, creditAccountGlCode);
            }
            final LocalDate startDate = JdbcSupport.getLocalDate(rs, "startDate");

            Collection<TaxComponentHistoryData> historyDatas = new ArrayList<>();
            historyDatas.add(componentHistoryDataMapper.mapRow(rs, rowNum));
            while (rs.next()) {
                if (id.equals(rs.getLong("id"))) {
                    historyDatas.add(componentHistoryDataMapper.mapRow(rs, rowNum));
                } else {
                    rs.previous();
                    break;
                }
            }
            return TaxComponentData.instance(id, name, percentage, debitAccountType, debitAccountData, creditAccountType, creditAccountData,
                    startDate, historyDatas);
        }

        public String getSchema() {
            return this.schema;
        }

    }

    private static final class TaxComponentHistoryDataMapper implements RowMapper<TaxComponentHistoryData> {

        @Override
        public TaxComponentHistoryData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {
            final BigDecimal percentage = rs.getBigDecimal("historyPercentage");
            final LocalDate startDate = JdbcSupport.getLocalDate(rs, "historyStartDate");
            final LocalDate endDate = JdbcSupport.getLocalDate(rs, "historyEndDate");
            return new TaxComponentHistoryData(percentage, startDate, endDate);
        }

    }

    private static final class TaxGroupMapper implements RowMapper<TaxGroupData> {

        private final String schema;
        private final TaxGroupMappingsDataMapper taxGroupMappingsDataMapper = new TaxGroupMappingsDataMapper();

        TaxGroupMapper() {
            StringBuilder sb = new StringBuilder();
            sb.append("tg.id as id, tg.name as name,");
            sb.append("tgm.id as mappingId,");
            sb.append("tc.id as taxComponentId, tc.name as taxComponentName, tc.percentage as taxComponentPercentage,");
            sb.append("tgm.start_date as startDate, tgm.end_date as endDate ");
            sb.append(" from m_tax_group tg ");
            sb.append(" inner join m_tax_group_mappings tgm on tgm.tax_group_id = tg.id ");
            sb.append(" inner join m_tax_component tc on tc.id = tgm.tax_component_id ");
            this.schema = sb.toString();
        }

        @Override
        public TaxGroupData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final Collection<TaxGroupMappingsData> taxAssociations = new ArrayList<>();
            taxAssociations.add(this.taxGroupMappingsDataMapper.mapRow(rs, rowNum));
            while (rs.next()) {
                if (id.equals(rs.getLong("id"))) {
                    taxAssociations.add(this.taxGroupMappingsDataMapper.mapRow(rs, rowNum));
                } else {
                    rs.previous();
                    break;
                }
            }
            return TaxGroupData.instance(id, name, taxAssociations);
        }

        public String getSchema() {
            return this.schema;
        }

    }

    private static final class TaxGroupMappingsDataMapper implements RowMapper<TaxGroupMappingsData> {

        @Override
        public TaxGroupMappingsData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {
            final Long mappingId = rs.getLong("mappingId");
            final Long id = rs.getLong("taxComponentId");
            final String name = rs.getString("taxComponentName");
            final BigDecimal percentage = rs.getBigDecimal("taxComponentPercentage");
            TaxComponentData componentData = TaxComponentData.lookupWithPercentage(id, name, percentage);

            final LocalDate startDate = JdbcSupport.getLocalDate(rs, "startDate");
            final LocalDate endDate = JdbcSupport.getLocalDate(rs, "endDate");
            return new TaxGroupMappingsData(mappingId, componentData, startDate, endDate);
        }

    }

    private static final class TaxComponentLookUpMapper implements RowMapper<TaxComponentData> {

        private final String schema;

        TaxComponentLookUpMapper() {
            StringBuilder sb = new StringBuilder();
            sb.append("tc.id as id, tc.name as name ");
            sb.append(" from m_tax_component tc ");
            this.schema = sb.toString();
        }

        public String getSchema() {
            return this.schema;
        }

        @Override
        public TaxComponentData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            return TaxComponentData.lookup(id, name);
        }

    }

    private static final class TaxGroupLookUpMapper implements RowMapper<TaxGroupData> {

        private final String schema;

        TaxGroupLookUpMapper() {
            StringBuilder sb = new StringBuilder();
            sb.append("tg.id as id, tg.name as name ");
            sb.append(" from m_tax_group tg ");
            this.schema = sb.toString();
        }

        public String getSchema() {
            return this.schema;
        }

        @Override
        public TaxGroupData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            return TaxGroupData.lookup(id, name);
        }

    }

}
