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
package org.apache.fineract.portfolio.loanproduct.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.interestratechart.service.InterestRateChartEnumerations;
import org.apache.fineract.portfolio.loanproduct.data.CLIChargeSlabData;
import org.apache.fineract.portfolio.loanproduct.domain.CLIChargeSlab;
import org.apache.fineract.portfolio.loanproduct.domain.CLIChargeSlabRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class CLIChargeSlabReadPlatformServiceImpl implements CLIChargeSlabReadPlatformService {

    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;
    private final CLIChargeSlabRepository cliChargeSlabRepository;
    private final CLIChargeSlabMapper slabMapper = new CLIChargeSlabMapper();

    @Autowired
    public CLIChargeSlabReadPlatformServiceImpl(PlatformSecurityContext context, JdbcTemplate jdbcTemplate,
            CLIChargeSlabRepository cliChargeSlabRepository) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
        this.cliChargeSlabRepository = cliChargeSlabRepository;
    }

    @Override
    public Collection<CLIChargeSlabData> retrieveAllByLoanProductId(Long loanProductId) {
        this.context.authenticatedUser();
        final String sql = "SELECT " + slabMapper.schema() + " WHERE cs.loan_product_id = ? ORDER BY cs.charge_id, cs.from_period";
        return this.jdbcTemplate.query(sql, slabMapper, loanProductId);
    }

    @Override
    public Collection<CLIChargeSlabData> retrieveByLoanProductIdAndChargeId(Long loanProductId, Long chargeId) {
        this.context.authenticatedUser();
        final String sql = "SELECT " + slabMapper.schema() + " WHERE cs.loan_product_id = ? AND cs.charge_id = ? ORDER BY cs.from_period";
        return this.jdbcTemplate.query(sql, slabMapper, loanProductId, chargeId);
    }

    @Override
    public BigDecimal resolveCLIRate(Long loanProductId, Long chargeId, Integer tenorInMonths, BigDecimal loanAmount) {
        List<CLIChargeSlab> slabs = cliChargeSlabRepository.findByLoanProductAndChargeOrderByPeriod(loanProductId, chargeId);

        for (CLIChargeSlab slab : slabs) {
            if (slab.isApplicable(tenorInMonths, loanAmount)) {
                return slab.getRate();
            }
        }
        return null; // No matching slab found
    }

    private static final class CLIChargeSlabMapper implements RowMapper<CLIChargeSlabData> {

        public String schema() {
            return "cs.id as id, cs.charge_id as chargeId, c.name as chargeName, " +
                   "cs.from_period as fromPeriod, cs.to_period as toPeriod, cs.period_type_enum as periodType, " +
                   "cs.amount_range_from as amountRangeFrom, cs.amount_range_to as amountRangeTo, cs.rate as rate " +
                   "FROM m_product_loan_cli_slab cs " +
                   "JOIN m_charge c ON c.id = cs.charge_id ";
        }

        @Override
        public CLIChargeSlabData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final Long chargeId = rs.getLong("chargeId");
            final String chargeName = rs.getString("chargeName");
            final Integer fromPeriod = rs.getObject("fromPeriod") != null ? rs.getInt("fromPeriod") : null;
            final Integer toPeriod = rs.getObject("toPeriod") != null ? rs.getInt("toPeriod") : null;
            final Integer periodTypeId = rs.getObject("periodType") != null ? rs.getInt("periodType") : null;
            final BigDecimal amountRangeFrom = rs.getBigDecimal("amountRangeFrom");
            final BigDecimal amountRangeTo = rs.getBigDecimal("amountRangeTo");
            final BigDecimal rate = rs.getBigDecimal("rate");

            EnumOptionData periodType = null;
            if (periodTypeId != null) {
                periodType = InterestRateChartEnumerations.periodType(periodTypeId);
            }

            return CLIChargeSlabData.instance(id, chargeId, chargeName, fromPeriod, toPeriod, periodType,
                    amountRangeFrom, amountRangeTo, rate);
        }
    }
}

