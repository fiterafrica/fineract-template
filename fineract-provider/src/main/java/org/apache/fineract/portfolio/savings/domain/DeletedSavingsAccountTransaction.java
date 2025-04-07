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

import lombok.Data;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.useradministration.domain.AppUser;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.Column;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


/**
 * All deleted interest posting and accrual transactions against a savings account are modelled through this entity.
 */
@Data
@Entity
@Table(name = "m_deleted_savings_account_transaction")
public final class DeletedSavingsAccountTransaction extends AbstractPersistableCustom {

    @ManyToOne(optional = false)
    @JoinColumn(name = "savings_account_id", referencedColumnName = "id", nullable = false)
    private SavingsAccount savingsAccount;

    @Column(name = "deleted_transaction_id", nullable = false)
    private Long deletedTransactionId;

    @Column(name = "deleted_by_transaction_id", nullable = false)
    private Long deletedByTransactionId;

    @Column(name = "transaction_type_enum", nullable = false)
    private Integer typeOf;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate dateOf;

    @Column(name = "amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal amount;

    @Column(name = "is_reversed", nullable = false)
    private boolean reversed;

    @Column(name = "running_balance_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal runningBalance;

    @Column(name = "cumulative_balance_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal cumulativeBalance;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @ManyToOne
    @JoinColumn(name = "appuser_id", nullable = true)
    private AppUser appUser;

    @Column(name = "ref_no", nullable = true)
    private String refNo;

    DeletedSavingsAccountTransaction() {
        this.dateOf = null;
        this.typeOf = null;
        this.createdDate = null;
    }

    public DeletedSavingsAccountTransaction(SavingsAccount savingsAccount, Long deletedTransactionId, Integer typeOf, LocalDate dateOf, BigDecimal amount, boolean reversed, BigDecimal runningBalance, BigDecimal cumulativeBalance, LocalDateTime createdDate, AppUser appUser, String refNo, Long deletedByTransactionId) {
        this.savingsAccount = savingsAccount;
        this.deletedTransactionId = deletedTransactionId;
        this.typeOf = typeOf;
        this.dateOf = dateOf;
        this.amount = amount;
        this.reversed = reversed;
        this.runningBalance = runningBalance;
        this.cumulativeBalance = cumulativeBalance;
        this.createdDate = createdDate;
        this.appUser = appUser;
        this.refNo = refNo;
        this.deletedByTransactionId = deletedByTransactionId;
    }
}
