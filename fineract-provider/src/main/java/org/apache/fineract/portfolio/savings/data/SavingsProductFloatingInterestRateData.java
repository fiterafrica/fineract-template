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

package org.apache.fineract.portfolio.savings.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SavingsProductFloatingInterestRateData implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final Long id;

    private final Long savingsProductId;

    private LocalDate fromDate;

    private LocalDate endDate;

    private BigDecimal floatingInterestRate;

    public SavingsProductFloatingInterestRateData(Long id, Long savingsProductId, LocalDate fromDate, LocalDate endDate, BigDecimal floatingInterestRate) {
        this.id = id;
        this.savingsProductId = savingsProductId;
        this.fromDate = fromDate;
        this.endDate = endDate;
        this.floatingInterestRate = floatingInterestRate;
    }

    public static SavingsProductFloatingInterestRateData instance(Long id, Long savingsProductId, LocalDate fromDate, LocalDate endDate, BigDecimal floatingInterestRate) {
        return new SavingsProductFloatingInterestRateData(id, savingsProductId, fromDate, endDate, floatingInterestRate);
    }

    public Long getId() {
        return id;
    }

    public Long getSavingsProductId() {
        return savingsProductId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getFloatingInterestRate() {
        return floatingInterestRate;
    }

    public void setFloatingInterestRate(BigDecimal floatingInterestRate) {
        this.floatingInterestRate = floatingInterestRate;
    }
}
