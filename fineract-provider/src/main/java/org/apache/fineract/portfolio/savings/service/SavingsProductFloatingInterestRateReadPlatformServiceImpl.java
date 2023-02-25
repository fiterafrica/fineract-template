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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.savings.data.SavingsProductFloatingInterestRateData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SavingsProductFloatingInterestRateReadPlatformServiceImpl implements SavingsProductFloatingInterestRateReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;

    private static final class SavingsProductFloatingInterestRateMapper implements RowMapper<SavingsProductFloatingInterestRateData> {

        public String schema() {
            return "spfir.id AS id, spfir.savings_product_id AS savingsProductId, spfir.from_date AS fromDate, spfir.end_date AS endDate,spfir.floating_interest_rate AS floatingInterestRate"
                    + " FROM m_savings_product_floating_interest_rate spfir";
        }

        @Override
        public SavingsProductFloatingInterestRateData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
                throws SQLException {
            final long id = rs.getLong("id");
            final long savingsProductId = rs.getLong("savingsProductId");
            final BigDecimal floatingInterestRate = rs.getBigDecimal("floatingInterestRate");
            final LocalDate fromDate = JdbcSupport.getLocalDate(rs, "fromDate");
            final LocalDate endDate = JdbcSupport.getLocalDate(rs, "endDate");
            return SavingsProductFloatingInterestRateData.instance(id, savingsProductId, fromDate, endDate, floatingInterestRate);
        }
    }

    @Override
    public Collection<SavingsProductFloatingInterestRateData> getSavingsProductFloatingInterestRateForSavingsProduct(
            long savingsProductId) {
        this.context.authenticatedUser();
        final SavingsProductFloatingInterestRateMapper rm = new SavingsProductFloatingInterestRateMapper();
        final String sql = "select " + rm.schema() + " where spfir.savings_product_id=?";
        return this.jdbcTemplate.query(sql, rm, new Object[] { savingsProductId }); // NOSONAR
    }

    @Override
    public SavingsProductFloatingInterestRateData getSavingsProductFloatingInterestRateById(long id) {
        this.context.authenticatedUser();
        final SavingsProductFloatingInterestRateMapper rm = new SavingsProductFloatingInterestRateMapper();
        final String sql = "select " + rm.schema() + " where spfir.id=?";
        return this.jdbcTemplate.queryForObject(sql, rm, new Object[] { id }); // NOSONAR
    }
}
