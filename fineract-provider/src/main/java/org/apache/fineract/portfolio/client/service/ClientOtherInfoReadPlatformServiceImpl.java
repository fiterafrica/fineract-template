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
package org.apache.fineract.portfolio.client.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.client.data.ClientOtherInfoData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ClientOtherInfoReadPlatformServiceImpl implements ClientOtherInfoReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;
    private final CodeValueReadPlatformService codeValueReadPlatformService;

    @Autowired
    public ClientOtherInfoReadPlatformServiceImpl(final PlatformSecurityContext context, final JdbcTemplate jdbcTemplate,
            final CodeValueReadPlatformService codeValueReadPlatformService) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
        this.codeValueReadPlatformService = codeValueReadPlatformService;

    }

    private static final class ClientOtherInfoMapper implements RowMapper<ClientOtherInfoData> {

        public String schema() {
            return "co.id AS id, co.client_id AS clientId, co.strata_cv_id AS strataId, co.nationality_cv_id AS nationalityId, cv.code_value as strataName, co.year_arrived_in_country_cv_id AS yearArrivedInHostCountryId,"
                    + "cvn.code_value AS nationalityName, cy.code_value AS yearArrivedInHostCountryName, co.number_of_children AS numberOfChildren, co.number_of_dependents AS numberOfDependents, co.co_signors as coSignors, co.guarantor as guarantor,"
                    + "co.national_identification_number AS nationalIdentificationNumber,co.passport_number AS passportNumber,co.bank_account_number AS bankAccountNumber,co.bank_name AS bankName,co.telephone_no as telephoneNumber "
                    + " FROM m_client_other_info co" + " left join m_code_value cvn on co.nationality_cv_id=cvn.id"
                    + " left join m_code_value cv on co.strata_cv_id=cv.id left join m_code_value cy on co.year_arrived_in_country_cv_id=cy.id";
        }

        @Override
        public ClientOtherInfoData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final long id = rs.getLong("id");
            final long clientId = rs.getLong("clientId");

            final long nationalityId = rs.getLong("nationalityId");
            final String nationalityName = rs.getString("nationalityName");
            final CodeValueData nationality = CodeValueData.instance(nationalityId, nationalityName);
            final long strataId = rs.getLong("strataId");
            final String strataName = rs.getString("strataName");
            final CodeValueData strata = CodeValueData.instance(strataId, strataName);
            final Integer numberOfChildren = JdbcSupport.getInteger(rs, "numberOfChildren");
            final Integer numberOfDependents = JdbcSupport.getInteger(rs, "numberOfDependents");
            final Long yearArrivedInHostCountryId = JdbcSupport.getLong(rs, "yearArrivedInHostCountryId");
            final String yearArrivedInHostCountryName = rs.getString("yearArrivedInHostCountryName");
            final CodeValueData yearArrivedInHostCountry = CodeValueData.instance(yearArrivedInHostCountryId, yearArrivedInHostCountryName);
            final String nationalIdentificationNumber = rs.getString("nationalIdentificationNumber");
            final String passportNumber = rs.getString("passportNumber");
            final String bankAccountNumber = rs.getString("bankAccountNumber");
            final String bankName = rs.getString("bankName");
            final String telephoneNumber = rs.getString("telephoneNumber");

            return ClientOtherInfoData.instance(id, clientId, strata, yearArrivedInHostCountry, nationality, numberOfChildren,
                    numberOfDependents, nationalIdentificationNumber, passportNumber, bankAccountNumber, bankName, telephoneNumber);

        }
    }

    @Override
    public Collection<ClientOtherInfoData> retrieveAll(long clientId) {

        this.context.authenticatedUser();

        final ClientOtherInfoMapper rm = new ClientOtherInfoMapper();
        final String sql = "select " + rm.schema() + " where co.client_id=?";

        return this.jdbcTemplate.query(sql, rm, new Object[] { clientId }); // NOSONAR
    }

    @Override
    public ClientOtherInfoData retrieveByClientId(long clientId) {
        this.context.authenticatedUser();
        final ClientOtherInfoMapper rm = new ClientOtherInfoMapper();
        final String sql = "select " + rm.schema() + " where co.client_id=?";
        try {
            return this.jdbcTemplate.queryForObject(sql, rm, new Object[] { clientId }); // NOSONAR
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ClientOtherInfoData retrieveOne(Long id) {

        this.context.authenticatedUser();

        final ClientOtherInfoMapper rm = new ClientOtherInfoMapper();
        final String sql = "select " + rm.schema() + " where co.id=? ";

        return this.jdbcTemplate.queryForObject(sql, rm, new Object[] { id }); // NOSONAR
    }

    @Override
    public ClientOtherInfoData retrieveTemplate() {

        final List<CodeValueData> nationalityOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode("COUNTRY"));
        final List<CodeValueData> strataOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.STRATA));
        final List<CodeValueData> yearArrivedInHostCountryOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.YEAR_ARRIVED_IN_HOST_COUNTRY));
        return ClientOtherInfoData.template(nationalityOptions, strataOptions, yearArrivedInHostCountryOptions);
    }

    private static final class ClientOtherInfoEntityMapper implements RowMapper<ClientOtherInfoData> {

        public String schema() {
            return "co.id AS id, co.client_id AS clientId, co.strata_cv_id AS strataId, co.co_signors AS coSignorsName, cv.code_value as strataName, "
                    + " co.guarantor AS guarantor, co.tax_identification_number as taxIdentificationNumber, co.business_location as businessLocation,"
                    + " co.income_generating_activity AS incomeGeneratingActivity, co.income_generating_activity_monthly_amount as incomeGeneratingActivityMonthlyAmount,"
                    + " co.telephone_no as telephoneNo" + " FROM m_client_other_info co"
                    + " left join m_code_value cv on co.strata_cv_id=cv.id";
        }

        @Override
        public ClientOtherInfoData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final long id = rs.getLong("id");
            final long clientId = rs.getLong("clientId");

            final long strataId = rs.getLong("strataId");
            final String strataName = rs.getString("strataName");
            final CodeValueData strata = CodeValueData.instance(strataId, strataName);

            final String coSignors = rs.getString("coSignorsName");
            final String guarantor = rs.getString("guarantor");
            final Long taxIdentificationNumber = rs.getLong("taxIdentificationNumber");
            final String businessLocation = rs.getString("businessLocation");
            final Long incomeGeneratingActivity = rs.getLong("incomeGeneratingActivity");
            final BigDecimal incomeGeneratingActivityMonthlyAmount = rs.getBigDecimal("incomeGeneratingActivityMonthlyAmount");
            final String telephoneNo = rs.getString("telephoneNo");

            return ClientOtherInfoData.instanceEntity(id, clientId, coSignors, guarantor, strata, businessLocation, taxIdentificationNumber,
                    incomeGeneratingActivity, incomeGeneratingActivityMonthlyAmount, telephoneNo);

        }
    }

    @Override
    public Collection<ClientOtherInfoData> retrieveEntityAll(long clientId) {

        this.context.authenticatedUser();

        final ClientOtherInfoEntityMapper rm = new ClientOtherInfoEntityMapper();
        final String sql = "select " + rm.schema() + " where co.client_id=?";

        return this.jdbcTemplate.query(sql, rm, new Object[] { clientId }); // NOSONAR
    }

    @Override
    public ClientOtherInfoData retrieveEntityOne(Long id) {

        this.context.authenticatedUser();

        final ClientOtherInfoEntityMapper rm = new ClientOtherInfoEntityMapper();
        final String sql = "select " + rm.schema() + " where co.id=? ";

        return this.jdbcTemplate.queryForObject(sql, rm, new Object[] { id }); // NOSONAR
    }

}
