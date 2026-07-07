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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.savings.SavingsPeriodFrequencyType;

@Entity
@Table(name = "m_product_loan_cli_slab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CLIChargeSlab extends AbstractPersistableCustom {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id", nullable = false)
    private Charge charge;

    @Column(name = "from_period")
    private Integer fromPeriod;

    @Column(name = "to_period")
    private Integer toPeriod;

    @Column(name = "period_type_enum", nullable = false)
    private Integer periodType;

    @Column(name = "amount_range_from", scale = 6, precision = 19)
    private BigDecimal amountRangeFrom;

    @Column(name = "amount_range_to", scale = 6, precision = 19)
    private BigDecimal amountRangeTo;

    @Column(name = "rate", scale = 6, precision = 19, nullable = false)
    private BigDecimal rate;

    public static List<CLIChargeSlab> assembleFrom(JsonArray cliSlabsArray, LoanProduct loanProduct, Charge charge) {
        List<CLIChargeSlab> slabs = new ArrayList<>();
        if (cliSlabsArray == null) return slabs;

        for (JsonElement element : cliSlabsArray) {
            JsonObject slabObj = element.getAsJsonObject();
            CLIChargeSlab slab = new CLIChargeSlab();
            slab.setLoanProduct(loanProduct);
            slab.setCharge(charge);
            slab.setFromPeriod(getIntOrNull(slabObj, "fromPeriod"));
            slab.setToPeriod(getIntOrNull(slabObj, "toPeriod"));
            slab.setPeriodType(getIntOrDefault(slabObj, "periodType", SavingsPeriodFrequencyType.MONTHS.getValue()));
            slab.setAmountRangeFrom(getBigDecimalOrNull(slabObj, "amountRangeFrom"));
            slab.setAmountRangeTo(getBigDecimalOrNull(slabObj, "amountRangeTo"));
            slab.setRate(slabObj.get("rate").getAsBigDecimal());
            slabs.add(slab);
        }
        return slabs;
    }

    private static Integer getIntOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : null;
    }

    private static Integer getIntOrDefault(JsonObject obj, String key, Integer defaultVal) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : defaultVal;
    }

    private static BigDecimal getBigDecimalOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsBigDecimal() : null;
    }

    public boolean isApplicable(Integer tenorInMonths, BigDecimal loanAmount) {
        return isApplicableForTenor(tenorInMonths) && isApplicableForAmount(loanAmount);
    }

    public boolean isApplicableForTenor(Integer tenorInMonths) {
        if (tenorInMonths == null) return false;
        boolean fromOk = (this.fromPeriod == null || tenorInMonths >= this.fromPeriod);
        boolean toOk = (this.toPeriod == null || tenorInMonths <= this.toPeriod);
        return fromOk && toOk;
    }

    public boolean isApplicableForAmount(BigDecimal loanAmount) {
        if (loanAmount == null) return true;
        boolean fromOk = (this.amountRangeFrom == null || loanAmount.compareTo(this.amountRangeFrom) >= 0);
        boolean toOk = (this.amountRangeTo == null || loanAmount.compareTo(this.amountRangeTo) <= 0);
        return fromOk && toOk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CLIChargeSlab)) return false;
        CLIChargeSlab that = (CLIChargeSlab) o;
        return Objects.equals(fromPeriod, that.fromPeriod) && Objects.equals(toPeriod, that.toPeriod) &&
               Objects.equals(amountRangeFrom, that.amountRangeFrom) && Objects.equals(amountRangeTo, that.amountRangeTo) &&
               Objects.equals(rate, that.rate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromPeriod, toPeriod, amountRangeFrom, amountRangeTo, rate);
    }
}

