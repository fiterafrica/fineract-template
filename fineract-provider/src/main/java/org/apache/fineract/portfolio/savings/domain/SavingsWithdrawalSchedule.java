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
package org.apache.fineract.portfolio.savings.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

/**
 * It Keeps the withdrawal dates that was generated during saving account creation
 */
@Entity
@Table(name = "m_savings_withdrawal_schedule")
public final class SavingsWithdrawalSchedule extends AbstractPersistableCustom {

    @ManyToOne()
    @JoinColumn(name = "savings_account_id", referencedColumnName = "id", nullable = false)
    private SavingsAccount savingsAccount;

    @Column(name = "withdrawal_frequency", nullable = false)
    private Integer withdrawalFrequency;

    @Column(name = "next_withdrawal_date", nullable = false)
    private LocalDate nextWithdrawalDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public SavingsWithdrawalSchedule() {

    }

    public static SavingsWithdrawalSchedule newInstance(SavingsAccount savingsAccount, Integer withdrawalFrequency,
            LocalDate nextWithdrawalDate, LocalDateTime createdAt) {
        return new SavingsWithdrawalSchedule(savingsAccount, withdrawalFrequency, nextWithdrawalDate, createdAt);
    }

    public SavingsWithdrawalSchedule(SavingsAccount savingsAccount, Integer withdrawalFrequency, LocalDate nextWithdrawalDate,
            LocalDateTime createdAt) {
        this.savingsAccount = savingsAccount;
        this.withdrawalFrequency = withdrawalFrequency;
        this.nextWithdrawalDate = nextWithdrawalDate;
        this.createdAt = createdAt;
    }

    public SavingsAccount getSavingsAccount() {
        return savingsAccount;
    }

    public void setSavingsAccount(SavingsAccount savingsAccount) {
        this.savingsAccount = savingsAccount;
    }

    public LocalDate getNextWithdrawalDate() {
        return nextWithdrawalDate;
    }

    public void setNextWithdrawalDate(LocalDate nextWithdrawalDate) {
        this.nextWithdrawalDate = nextWithdrawalDate;
    }

    public Integer getWithdrawalFrequency() {
        return withdrawalFrequency;
    }
}
