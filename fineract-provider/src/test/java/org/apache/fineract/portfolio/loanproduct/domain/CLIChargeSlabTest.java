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
package org.apache.fineract.portfolio.loanproduct.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.List;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.savings.SavingsPeriodFrequencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for CLIChargeSlab entity.
 */
@ExtendWith(MockitoExtension.class)
class CLIChargeSlabTest {

    @Mock
    private LoanProduct loanProduct;

    @Mock
    private Charge charge;

    private CLIChargeSlab slab;

    @BeforeEach
    void setUp() {
        slab = new CLIChargeSlab();
        slab.setLoanProduct(loanProduct);
        slab.setCharge(charge);
        slab.setFromPeriod(1);
        slab.setToPeriod(6);
        slab.setPeriodType(SavingsPeriodFrequencyType.MONTHS.getValue());
        slab.setAmountRangeFrom(BigDecimal.ZERO);
        slab.setAmountRangeTo(new BigDecimal("100000000"));
        slab.setRate(new BigDecimal("0.9"));
    }

    @Test
    void testIsApplicableForTenor_WithinRange() {
        assertTrue(slab.isApplicableForTenor(3));
        assertTrue(slab.isApplicableForTenor(1));
        assertTrue(slab.isApplicableForTenor(6));
    }

    @Test
    void testIsApplicableForTenor_OutsideRange() {
        assertFalse(slab.isApplicableForTenor(0));
        assertFalse(slab.isApplicableForTenor(7));
        assertFalse(slab.isApplicableForTenor(12));
    }

    @Test
    void testIsApplicableForTenor_NullTenor() {
        assertFalse(slab.isApplicableForTenor(null));
    }

    @Test
    void testIsApplicableForTenor_NullFromPeriod() {
        slab.setFromPeriod(null);
        assertTrue(slab.isApplicableForTenor(3));
        assertTrue(slab.isApplicableForTenor(1));
    }

    @Test
    void testIsApplicableForTenor_NullToPeriod() {
        slab.setToPeriod(null);
        assertTrue(slab.isApplicableForTenor(100));
        assertTrue(slab.isApplicableForTenor(1));
    }

    @Test
    void testIsApplicableForAmount_WithinRange() {
        assertTrue(slab.isApplicableForAmount(new BigDecimal("50000000")));
        assertTrue(slab.isApplicableForAmount(BigDecimal.ZERO));
        assertTrue(slab.isApplicableForAmount(new BigDecimal("100000000")));
    }

    @Test
    void testIsApplicableForAmount_OutsideRange() {
        assertFalse(slab.isApplicableForAmount(new BigDecimal("100000001")));
        assertFalse(slab.isApplicableForAmount(new BigDecimal("200000000")));
    }

    @Test
    void testIsApplicableForAmount_NullAmount() {
        assertTrue(slab.isApplicableForAmount(null));
    }

    @Test
    void testIsApplicable_BothConditionsMet() {
        assertTrue(slab.isApplicable(3, new BigDecimal("50000000")));
        assertTrue(slab.isApplicable(6, new BigDecimal("100000000")));
    }

    @Test
    void testIsApplicable_TenorOutOfRange() {
        assertFalse(slab.isApplicable(12, new BigDecimal("50000000")));
    }

    @Test
    void testIsApplicable_AmountOutOfRange() {
        assertFalse(slab.isApplicable(3, new BigDecimal("200000000")));
    }

    @Test
    void testAssembleFrom_ValidJsonArray() {
        JsonArray slabsArray = new JsonArray();

        JsonObject slabObj1 = new JsonObject();
        slabObj1.addProperty("fromPeriod", 1);
        slabObj1.addProperty("toPeriod", 6);
        slabObj1.addProperty("amountRangeTo", 100000000);
        slabObj1.addProperty("rate", 0.9);
        slabsArray.add(slabObj1);

        JsonObject slabObj2 = new JsonObject();
        slabObj2.addProperty("fromPeriod", 7);
        slabObj2.addProperty("toPeriod", 12);
        slabObj2.addProperty("amountRangeTo", 100000000);
        slabObj2.addProperty("rate", 1.0);
        slabsArray.add(slabObj2);

        List<CLIChargeSlab> slabs = CLIChargeSlab.assembleFrom(slabsArray, loanProduct, charge);

        assertNotNull(slabs);
        assertEquals(2, slabs.size());

        CLIChargeSlab first = slabs.get(0);
        assertEquals(Integer.valueOf(1), first.getFromPeriod());
        assertEquals(Integer.valueOf(6), first.getToPeriod());
        assertEquals(new BigDecimal("0.9"), first.getRate());

        CLIChargeSlab second = slabs.get(1);
        assertEquals(Integer.valueOf(7), second.getFromPeriod());
        assertEquals(Integer.valueOf(12), second.getToPeriod());
        assertEquals(new BigDecimal("1.0"), second.getRate());
    }

    @Test
    void testAssembleFrom_NullJsonArray() {
        List<CLIChargeSlab> slabs = CLIChargeSlab.assembleFrom(null, loanProduct, charge);
        assertNotNull(slabs);
        assertTrue(slabs.isEmpty());
    }

    @Test
    void testAssembleFrom_EmptyJsonArray() {
        JsonArray slabsArray = new JsonArray();
        List<CLIChargeSlab> slabs = CLIChargeSlab.assembleFrom(slabsArray, loanProduct, charge);
        assertNotNull(slabs);
        assertTrue(slabs.isEmpty());
    }

    @Test
    void testEquals_SameValues() {
        CLIChargeSlab slab2 = new CLIChargeSlab();
        slab2.setFromPeriod(1);
        slab2.setToPeriod(6);
        slab2.setAmountRangeFrom(BigDecimal.ZERO);
        slab2.setAmountRangeTo(new BigDecimal("100000000"));
        slab2.setRate(new BigDecimal("0.9"));

        assertEquals(slab, slab2);
        assertEquals(slab.hashCode(), slab2.hashCode());
    }

    @Test
    void testEquals_DifferentRate() {
        CLIChargeSlab slab2 = new CLIChargeSlab();
        slab2.setFromPeriod(1);
        slab2.setToPeriod(6);
        slab2.setAmountRangeFrom(BigDecimal.ZERO);
        slab2.setAmountRangeTo(new BigDecimal("100000000"));
        slab2.setRate(new BigDecimal("1.0"));

        assertFalse(slab.equals(slab2));
    }
}

