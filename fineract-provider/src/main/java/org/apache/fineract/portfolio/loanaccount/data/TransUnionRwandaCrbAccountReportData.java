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

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TransUnionRwandaCrbAccountReportData {

    private Integer id;
    private String accountNo;
    private String accountOpeningDate;
    private String accountOwner;
    private String accountStatus;
    private String accountType;
    private BigDecimal arrearAmount;
    private Integer arrearDays;
    private BigDecimal balanceAmount;
    private String currency;
    private Boolean disputed;
    private Boolean isMyAccount;
    private String lastPaymentDate;
    private String listingDate;
    private BigDecimal principalAmount;
    private Integer repaymentDuration;
    private String repaymentTerm;
    private BigDecimal scheduledPaymentAmount;
    private String tradeSector;
    private Integer worstArrear;

    public TransUnionRwandaCrbAccountReportData() {}

    public TransUnionRwandaCrbAccountReportData(Integer id, String accountNo, String accountOpeningDate, String accountOwner,
            String accountStatus, String accountType, BigDecimal arrearAmount, Integer arrearDays, BigDecimal balanceAmount,
            String currency, Boolean disputed, Boolean isMyAccount, String lastPaymentDate, String listingDate, BigDecimal principalAmount,
            Integer repaymentDuration, String repaymentTerm, BigDecimal scheduledPaymentAmount, String tradeSector, Integer worstArrear) {
        this.id = id;
        this.accountNo = accountNo;
        this.accountOpeningDate = accountOpeningDate;
        this.accountOwner = accountOwner;
        this.accountStatus = accountStatus;
        this.accountType = accountType;
        this.arrearAmount = arrearAmount;
        this.arrearDays = arrearDays;
        this.balanceAmount = balanceAmount;
        this.currency = currency;
        this.disputed = disputed;
        this.isMyAccount = isMyAccount;
        this.lastPaymentDate = lastPaymentDate;
        this.listingDate = listingDate;
        this.principalAmount = principalAmount;
        this.repaymentDuration = repaymentDuration;
        this.repaymentTerm = repaymentTerm;
        this.scheduledPaymentAmount = scheduledPaymentAmount;
        this.tradeSector = tradeSector;
        this.worstArrear = worstArrear;
    }
}
