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
package org.apache.fineract.portfolio.loanaccount.domain;

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.loanaccount.data.AccountListData;

@Data
@Entity
@Table(name = "m_transunion_crb_account")
public class TransunionCrbAccount extends AbstractPersistableCustom {

    private static final long serialVersionUID = 9181640245194392646L;

    @ManyToOne
    @JoinColumn(name = "header_id", nullable = true)
    private TransunionCrbHeader headerId;
    @Column(name = "account_no")
    private String accountNo;
    @Column(name = "account_opening_date")
    private String accountOpeningDate;
    @Column(name = "account_owner")
    private String accountOwner;
    @Column(name = "account_status")
    private String accountStatus;
    @Column(name = "account_type")
    private String accountType;
    @Column(name = "arrear_amount")
    private BigDecimal arrearAmount;
    @Column(name = "arrear_days")
    private Integer arrearDays;
    @Column(name = "balance_amount")
    private BigDecimal balanceAmount;
    @Column(name = "currency")
    private String currency;
    @Column(name = "disputed")
    private Boolean disputed;
    @Column(name = "is_my_account")
    private Boolean isMyAccount;
    @Column(name = "last_payment_date")
    private String lastPaymentDate;
    @Column(name = "listing_date")
    private String listingDate;
    @Column(name = "principal_amount")
    private BigDecimal principalAmount;
    @Column(name = "repayment_duration")
    private Integer repaymentDuration;
    @Column(name = "repayment_term")
    private String repaymentTerm;
    @Column(name = "scheduled_payment_amount")
    private BigDecimal scheduledPaymentAmount;
    @Column(name = "trade_sector")
    private String tradeSector;
    @Column(name = "worst_arrear")
    private Integer worstArrear;

    public TransunionCrbAccount() {}

    public TransunionCrbAccount(TransunionCrbHeader headerId, AccountListData ac) {
        this.headerId = headerId;
        this.accountNo = ac.getAccountNo();
        this.accountOpeningDate = ac.getAccountOpeningDate();
        this.accountOwner = ac.getAccountOwner();
        this.accountStatus = ac.getAccountStatus();
        this.accountType = ac.getAccountType();
        this.arrearAmount = ac.getArrearAmount();
        this.arrearDays = ac.getArrearDays();
        this.balanceAmount = ac.getBalanceAmount();
        this.currency = ac.getCurrency();
        this.disputed = ac.getDisputed();
        this.isMyAccount = ac.getMyAccount();
        this.lastPaymentDate = ac.getLastPaymentDate();
        this.listingDate = ac.getListingDate();
        this.principalAmount = ac.getPrincipalAmount();
        this.repaymentDuration = ac.getRepaymentDuration();
        this.repaymentTerm = ac.getRepaymentTerm();
        this.scheduledPaymentAmount = ac.getScheduledPaymentAmount();
        this.tradeSector = ac.getTradeSector();
        this.worstArrear = ac.getWorstArrear();
    }
}
