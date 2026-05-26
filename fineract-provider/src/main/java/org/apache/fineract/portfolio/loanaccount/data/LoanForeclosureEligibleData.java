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
package org.apache.fineract.portfolio.loanaccount.data;

import java.io.Serializable;
import java.math.BigDecimal;

public final class LoanForeclosureEligibleData implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String loanId;
    private final String clientId;
    private final BigDecimal principalOutstanding;
    private final BigDecimal interestOutstanding;
    private final BigDecimal feeChargesOutstanding;
    private final BigDecimal penalyOutstanding;
    private final BigDecimal totalPayoff;

    public LoanForeclosureEligibleData(final String loanId, final String clientId, final BigDecimal principalOutstanding,
            final BigDecimal interestOutstanding, final BigDecimal feeChargesOutstanding, final BigDecimal penalyOutstanding,
            final BigDecimal totalPayoff) {
        this.loanId = loanId;
        this.clientId = clientId;
        this.principalOutstanding = principalOutstanding;
        this.interestOutstanding = interestOutstanding;
        this.feeChargesOutstanding = feeChargesOutstanding;
        this.penalyOutstanding = penalyOutstanding;
        this.totalPayoff = totalPayoff;
    }

    public static LoanForeclosureEligibleData instance(final Long loanId, final Long clientId, final BigDecimal principalOutstanding,
            final BigDecimal interestOutstanding, final BigDecimal feeChargesOutstanding, final BigDecimal penalyOutstanding,
            final BigDecimal totalPayoff) {
        return new LoanForeclosureEligibleData(String.valueOf(loanId), String.valueOf(clientId), principalOutstanding, interestOutstanding,
                feeChargesOutstanding, penalyOutstanding, totalPayoff);
    }

    public String getLoanId() {
        return this.loanId;
    }

    public String getClientId() {
        return this.clientId;
    }

    public BigDecimal getPrincipalOutstanding() {
        return this.principalOutstanding;
    }

    public BigDecimal getInterestOutstanding() {
        return this.interestOutstanding;
    }

    public BigDecimal getPenalyOutstanding() {
        return this.penalyOutstanding;
    }

    public BigDecimal getTotalPayoff() {
        return this.totalPayoff;
    }

}
