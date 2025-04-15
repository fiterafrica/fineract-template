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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DeletedSavingsAccountTransactionData {

    private Long deletedTransactionId;
    private Long deletedByTransactionId;
    private Integer transactionTypeOf;
    private LocalDate transactionDateOf;
    private BigDecimal transactionAmount;
    private BigDecimal transactionRunningBalance;
    private BigDecimal transactionCumulativeBalance;
    private LocalDateTime transactionCreatedDate;
    private String transactionAppUser;
    private String transactionRefNo;

    // Constructors
    public DeletedSavingsAccountTransactionData() {}

    public DeletedSavingsAccountTransactionData(Long deletedTransactionId, Long deletedByTransactionId, Integer deleteTransactionTypeOf,
            LocalDate deleteTransactionDateOf, BigDecimal deleteTransactionAmount, BigDecimal deletedTransactionRunningBalance,
            BigDecimal deletedTransactionCumulativeBalance, LocalDateTime deleteTransactionCreatedDate, String deleteTransactionRefNo) {
        this.deletedTransactionId = deletedTransactionId;
        this.deletedByTransactionId = deletedByTransactionId;
        this.transactionTypeOf = deleteTransactionTypeOf;
        this.transactionDateOf = deleteTransactionDateOf;
        this.transactionAmount = deleteTransactionAmount;
        this.transactionRunningBalance = deletedTransactionRunningBalance;
        this.transactionCumulativeBalance = deletedTransactionCumulativeBalance;
        this.transactionCreatedDate = deleteTransactionCreatedDate;
        this.transactionRefNo = deleteTransactionRefNo;

    }

}
