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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.loanproduct.domain.CLIChargeSlab;
import org.apache.fineract.portfolio.loanproduct.domain.CLIChargeSlabRepository;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.savings.SavingsPeriodFrequencyType;
import org.apache.fineract.useradministration.domain.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Unit tests for CLIChargeSlabReadPlatformServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class CLIChargeSlabReadPlatformServiceImplTest {

    @Mock
    private PlatformSecurityContext context;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CLIChargeSlabRepository cliChargeSlabRepository;

    @Mock
    private AppUser appUser;

    @Mock
    private LoanProduct loanProduct;

    @Mock
    private Charge charge;

    private CLIChargeSlabReadPlatformServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CLIChargeSlabReadPlatformServiceImpl(context, jdbcTemplate, cliChargeSlabRepository);
        when(context.authenticatedUser()).thenReturn(appUser);
    }

    @Test
    void testResolveCLIRate_MatchingSlabFound() {
        // Create test slabs
        List<CLIChargeSlab> slabs = createTestSlabs();
        when(cliChargeSlabRepository.findByLoanProductAndChargeOrderByPeriod(anyLong(), anyLong())).thenReturn(slabs);

        // Test with 3 months tenor and 50 million loan amount
        BigDecimal rate = service.resolveCLIRate(1L, 1L, 3, new BigDecimal("50000000"));

        assertNotNull(rate);
        assertEquals(new BigDecimal("0.9"), rate);
    }

    @Test
    void testResolveCLIRate_SecondSlabMatched() {
        List<CLIChargeSlab> slabs = createTestSlabs();
        when(cliChargeSlabRepository.findByLoanProductAndChargeOrderByPeriod(anyLong(), anyLong())).thenReturn(slabs);

        // Test with 10 months tenor
        BigDecimal rate = service.resolveCLIRate(1L, 1L, 10, new BigDecimal("50000000"));

        assertNotNull(rate);
        assertEquals(new BigDecimal("1.0"), rate);
    }

    @Test
    void testResolveCLIRate_ThirdSlabMatched() {
        List<CLIChargeSlab> slabs = createTestSlabs();
        when(cliChargeSlabRepository.findByLoanProductAndChargeOrderByPeriod(anyLong(), anyLong())).thenReturn(slabs);

        // Test with 20 months tenor
        BigDecimal rate = service.resolveCLIRate(1L, 1L, 20, new BigDecimal("50000000"));

        assertNotNull(rate);
        assertEquals(new BigDecimal("1.2"), rate);
    }

    @Test
    void testResolveCLIRate_NoMatchingSlabFound() {
        List<CLIChargeSlab> slabs = createTestSlabs();
        when(cliChargeSlabRepository.findByLoanProductAndChargeOrderByPeriod(anyLong(), anyLong())).thenReturn(slabs);

        // Test with 30 months tenor (beyond configured slabs)
        BigDecimal rate = service.resolveCLIRate(1L, 1L, 30, new BigDecimal("50000000"));

        assertNull(rate);
    }

    @Test
    void testResolveCLIRate_EmptySlabs() {
        when(cliChargeSlabRepository.findByLoanProductAndChargeOrderByPeriod(anyLong(), anyLong())).thenReturn(new ArrayList<>());

        BigDecimal rate = service.resolveCLIRate(1L, 1L, 6, new BigDecimal("50000000"));

        assertNull(rate);
    }

    @Test
    void testResolveCLIRate_AmountExceedsSlabRange() {
        List<CLIChargeSlab> slabs = createTestSlabs();
        when(cliChargeSlabRepository.findByLoanProductAndChargeOrderByPeriod(anyLong(), anyLong())).thenReturn(slabs);

        // Test with amount exceeding the configured range
        BigDecimal rate = service.resolveCLIRate(1L, 1L, 3, new BigDecimal("200000000"));

        assertNull(rate);
    }

    /**
     * Creates test CLI slabs matching the document specification:
     * - ≤6 months: 0.9%
     * - ≤12 months: 1.0%
     * - ≤24 months: 1.2%
     */
    private List<CLIChargeSlab> createTestSlabs() {
        List<CLIChargeSlab> slabs = new ArrayList<>();

        // First slab: 1-6 months, 0.9%
        CLIChargeSlab slab1 = new CLIChargeSlab();
        slab1.setLoanProduct(loanProduct);
        slab1.setCharge(charge);
        slab1.setFromPeriod(1);
        slab1.setToPeriod(6);
        slab1.setPeriodType(SavingsPeriodFrequencyType.MONTHS.getValue());
        slab1.setAmountRangeTo(new BigDecimal("100000000"));
        slab1.setRate(new BigDecimal("0.9"));
        slabs.add(slab1);

        // Second slab: 7-12 months, 1.0%
        CLIChargeSlab slab2 = new CLIChargeSlab();
        slab2.setLoanProduct(loanProduct);
        slab2.setCharge(charge);
        slab2.setFromPeriod(7);
        slab2.setToPeriod(12);
        slab2.setPeriodType(SavingsPeriodFrequencyType.MONTHS.getValue());
        slab2.setAmountRangeTo(new BigDecimal("100000000"));
        slab2.setRate(new BigDecimal("1.0"));
        slabs.add(slab2);

        // Third slab: 13-24 months, 1.2%
        CLIChargeSlab slab3 = new CLIChargeSlab();
        slab3.setLoanProduct(loanProduct);
        slab3.setCharge(charge);
        slab3.setFromPeriod(13);
        slab3.setToPeriod(24);
        slab3.setPeriodType(SavingsPeriodFrequencyType.MONTHS.getValue());
        slab3.setAmountRangeTo(new BigDecimal("100000000"));
        slab3.setRate(new BigDecimal("1.2"));
        slabs.add(slab3);

        return slabs;
    }
}

