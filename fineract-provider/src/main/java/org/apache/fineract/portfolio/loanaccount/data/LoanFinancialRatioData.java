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

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public final class LoanFinancialRatioData {

    private Long id;
    private Long loanId;
    private BigDecimal cash;
    private BigDecimal inventoryStock;
    private BigDecimal receivables;
    private BigDecimal chamaTontines;
    private BigDecimal otherCurrentAssets;
    private BigDecimal totalCurrentAssets;
    private BigDecimal goodsBoughtOnCredit;
    private BigDecimal anyOtherPendingPayables;
    private BigDecimal totalShortTerm;
    private BigDecimal equipmentTools;
    private BigDecimal furniture;
    private BigDecimal businessPremises;
    private BigDecimal otherFixedAssets;
    private BigDecimal totalFixedAssets;
    private BigDecimal totalAssets;
    private BigDecimal equity;
    private BigDecimal unsecuredLoans;
    private BigDecimal assetFinancing;
    private BigDecimal totalLongTerm;
    private BigDecimal totalLiabilities;
    private BigDecimal bssDeposits;
    private BigDecimal bssWithdrawals;
    private BigDecimal bssMonthlyTurnOver;
    private BigDecimal netMargin;
    private BigDecimal rotation;
    private BigDecimal liquidity;
    private BigDecimal leverage;
    private BigDecimal capitalization;
    private BigDecimal dscr;


    public LoanFinancialRatioData(Long id, Long loanId, BigDecimal cash, BigDecimal inventoryStock, BigDecimal receivables, BigDecimal chamaTontines,
                                  BigDecimal otherCurrentAssets, BigDecimal totalCurrentAssets, BigDecimal goodsBoughtOnCredit, BigDecimal anyOtherPendingPayables,
                                  BigDecimal totalShortTerm, BigDecimal equipmentTools, BigDecimal furniture, BigDecimal businessPremises, BigDecimal otherFixedAssets,
                                  BigDecimal totalFixedAssets, BigDecimal totalAssets, BigDecimal equity, BigDecimal unsecuredLoans, BigDecimal assetFinancing,
                                  BigDecimal totalLongTerm, BigDecimal totalLiabilities, BigDecimal bssDeposits, BigDecimal bssWithdrawals,
                                  BigDecimal bssMonthlyTurnOver) {
        this.id = id;
        this.loanId = loanId;
        this.cash = cash;
        this.inventoryStock = inventoryStock;
        this.receivables = receivables;
        this.chamaTontines = chamaTontines;
        this.otherCurrentAssets = otherCurrentAssets;
        this.totalCurrentAssets = totalCurrentAssets;
        this.goodsBoughtOnCredit = goodsBoughtOnCredit;
        this.anyOtherPendingPayables = anyOtherPendingPayables;
        this.totalShortTerm = totalShortTerm;
        this.equipmentTools = equipmentTools;
        this.furniture = furniture;
        this.businessPremises = businessPremises;
        this.otherFixedAssets = otherFixedAssets;
        this.totalFixedAssets = totalFixedAssets;
        this.totalAssets = totalAssets;
        this.equity = equity;
        this.unsecuredLoans = unsecuredLoans;
        this.assetFinancing = assetFinancing;
        this.totalLongTerm = totalLongTerm;
        this.totalLiabilities = totalLiabilities;
        this.bssDeposits = bssDeposits;
        this.bssWithdrawals = bssWithdrawals;
        this.bssMonthlyTurnOver = bssMonthlyTurnOver;

    }
}
