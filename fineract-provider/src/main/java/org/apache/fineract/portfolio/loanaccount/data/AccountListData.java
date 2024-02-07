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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "accountList")
public class AccountListData {

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

    @XmlElement(name = "accountNo")
    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    @XmlElement(name = "accountOpeningDate")
    public String getAccountOpeningDate() {
        return accountOpeningDate;
    }

    public void setAccountOpeningDate(String accountOpeningDate) {
        this.accountOpeningDate = accountOpeningDate;
    }

    @XmlElement(name = "accountOwner")
    public String getAccountOwner() {
        return accountOwner;
    }

    public void setAccountOwner(String accountOwner) {
        this.accountOwner = accountOwner;
    }

    @XmlElement(name = "accountStatus")
    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    @XmlElement(name = "arrearAmount")
    public BigDecimal getArrearAmount() {
        return arrearAmount;
    }

    public void setArrearAmount(BigDecimal arrearAmount) {
        this.arrearAmount = arrearAmount;
    }

    @XmlElement(name = "arrearDays")
    public Integer getArrearDays() {
        return arrearDays;
    }

    public void setArrearDays(Integer arrearDays) {
        this.arrearDays = arrearDays;
    }

    @XmlElement(name = "balanceAmount")
    public BigDecimal getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(BigDecimal balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    @XmlElement(name = "currency")
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @XmlElement(name = "disputed")
    public Boolean getDisputed() {
        if (disputed == null) {
            disputed = false;
        }
        return disputed;
    }

    public void setDisputed(Boolean disputed) {
        this.disputed = disputed;
    }

    @XmlElement(name = "isMyAccount")
    public Boolean getMyAccount() {
        if (isMyAccount == null) {
            isMyAccount = false;
        }
        return isMyAccount;
    }

    public void setMyAccount(Boolean myAccount) {
        isMyAccount = myAccount;
    }

    @XmlElement(name = "lastPaymentDate")
    public String getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(String lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    @XmlElement(name = "listingDate")
    public String getListingDate() {
        return listingDate;
    }

    public void setListingDate(String listingDate) {
        this.listingDate = listingDate;
    }

    @XmlElement(name = "principalAmount")
    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    @XmlElement(name = "repaymentDuration")
    public Integer getRepaymentDuration() {
        return repaymentDuration;
    }

    public void setRepaymentDuration(Integer repaymentDuration) {
        this.repaymentDuration = repaymentDuration;
    }

    @XmlElement(name = "repaymentTerm")
    public String getRepaymentTerm() {
        return repaymentTerm;
    }

    public void setRepaymentTerm(String repaymentTerm) {
        this.repaymentTerm = repaymentTerm;
    }

    @XmlElement(name = "scheduledPaymentAmount")
    public BigDecimal getScheduledPaymentAmount() {
        return scheduledPaymentAmount;
    }

    public void setScheduledPaymentAmount(BigDecimal scheduledPaymentAmount) {
        this.scheduledPaymentAmount = scheduledPaymentAmount;
    }

    @XmlElement(name = "tradeSector")
    public String getTradeSector() {
        return tradeSector;
    }

    public void setTradeSector(String tradeSector) {
        this.tradeSector = tradeSector;
    }

    @XmlElement(name = "worstArrear")
    public Integer getWorstArrear() {
        return worstArrear;
    }

    public void setWorstArrear(Integer worstArrear) {
        this.worstArrear = worstArrear;
    }

    @XmlElement(name = "accountType")
    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
}
