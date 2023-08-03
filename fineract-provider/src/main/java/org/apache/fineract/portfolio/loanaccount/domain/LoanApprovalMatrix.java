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
import javax.persistence.Table;
import lombok.Data;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;

@Data
@Entity
@Table(name = "m_loan_approval_matrix")
public class LoanApprovalMatrix extends AbstractAuditableCustom {

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;
    @Column(name = "level_one_unsecured_first_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelOneUnsecuredFirstCycleMaxAmount;
    @Column(name = "level_one_unsecured_first_cycle_min_term")
    private Integer levelOneUnsecuredFirstCycleMinTerm;
    @Column(name = "level_one_unsecured_first_cycle_max_term")
    private Integer levelOneUnsecuredFirstCycleMaxTerm;
    @Column(name = "level_one_unsecured_second_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelOneUnsecuredSecondCycleMaxAmount;
    @Column(name = "level_one_unsecured_second_cycle_min_term")
    private Integer levelOneUnsecuredSecondCycleMinTerm;
    @Column(name = "level_one_unsecured_second_cycle_max_term")
    private Integer levelOneUnsecuredSecondCycleMaxTerm;
    @Column(name = "level_one_secured_first_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelOneSecuredFirstCycleMaxAmount;
    @Column(name = "level_one_secured_first_cycle_min_term")
    private Integer levelOneSecuredFirstCycleMinTerm;
    @Column(name = "level_one_secured_first_cycle_max_term")
    private Integer levelOneSecuredFirstCycleMaxTerm;
    @Column(name = "level_one_secured_second_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelOneSecuredSecondCycleMaxAmount;
    @Column(name = "level_one_secured_second_cycle_min_term")
    private Integer levelOneSecuredSecondCycleMinTerm;
    @Column(name = "level_one_secured_second_cycle_max_term")
    private Integer levelOneSecuredSecondCycleMaxTerm;

    @Column(name = "level_two_unsecured_first_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelTwoUnsecuredFirstCycleMaxAmount;
    @Column(name = "level_two_unsecured_first_cycle_min_term")
    private Integer levelTwoUnsecuredFirstCycleMinTerm;
    @Column(name = "level_two_unsecured_first_cycle_max_term")
    private Integer levelTwoUnsecuredFirstCycleMaxTerm;
    @Column(name = "level_two_unsecured_second_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelTwoUnsecuredSecondCycleMaxAmount;
    @Column(name = "level_two_unsecured_second_cycle_min_term")
    private Integer levelTwoUnsecuredSecondCycleMinTerm;
    @Column(name = "level_two_unsecured_second_cycle_max_term")
    private Integer levelTwoUnsecuredSecondCycleMaxTerm;
    @Column(name = "level_two_secured_first_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelTwoSecuredFirstCycleMaxAmount;
    @Column(name = "level_two_secured_first_cycle_min_term")
    private Integer levelTwoSecuredFirstCycleMinTerm;
    @Column(name = "level_two_secured_first_cycle_max_term")
    private Integer levelTwoSecuredFirstCycleMaxTerm;
    @Column(name = "level_two_secured_second_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelTwoSecuredSecondCycleMaxAmount;
    @Column(name = "level_two_secured_second_cycle_min_term")
    private Integer levelTwoSecuredSecondCycleMinTerm;
    @Column(name = "level_two_secured_second_cycle_max_term")
    private Integer levelTwoSecuredSecondCycleMaxTerm;

    @Column(name = "level_three_unsecured_first_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelThreeUnsecuredFirstCycleMaxAmount;
    @Column(name = "level_three_unsecured_first_cycle_min_term")
    private Integer levelThreeUnsecuredFirstCycleMinTerm;
    @Column(name = "level_three_unsecured_first_cycle_max_term")
    private Integer levelThreeUnsecuredFirstCycleMaxTerm;
    @Column(name = "level_three_unsecured_second_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelThreeUnsecuredSecondCycleMaxAmount;
    @Column(name = "level_three_unsecured_second_cycle_min_term")
    private Integer levelThreeUnsecuredSecondCycleMinTerm;
    @Column(name = "level_three_unsecured_second_cycle_max_term")
    private Integer levelThreeUnsecuredSecondCycleMaxTerm;
    @Column(name = "level_three_secured_first_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelThreeSecuredFirstCycleMaxAmount;
    @Column(name = "level_three_secured_first_cycle_min_term")
    private Integer levelThreeSecuredFirstCycleMinTerm;
    @Column(name = "level_three_secured_first_cycle_max_term")
    private Integer levelThreeSecuredFirstCycleMaxTerm;
    @Column(name = "level_three_secured_second_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelThreeSecuredSecondCycleMaxAmount;
    @Column(name = "level_three_secured_second_cycle_min_term")
    private Integer levelThreeSecuredSecondCycleMinTerm;
    @Column(name = "level_three_secured_second_cycle_max_term")
    private Integer levelThreeSecuredSecondCycleMaxTerm;

    @Column(name = "level_four_unsecured_first_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelFourUnsecuredFirstCycleMaxAmount;
    @Column(name = "level_four_unsecured_first_cycle_min_term")
    private Integer levelFourUnsecuredFirstCycleMinTerm;
    @Column(name = "level_four_unsecured_first_cycle_max_term")
    private Integer levelFourUnsecuredFirstCycleMaxTerm;
    @Column(name = "level_four_unsecured_second_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelFourUnsecuredSecondCycleMaxAmount;
    @Column(name = "level_four_unsecured_second_cycle_min_term")
    private Integer levelFourUnsecuredSecondCycleMinTerm;
    @Column(name = "level_four_unsecured_second_cycle_max_term")
    private Integer levelFourUnsecuredSecondCycleMaxTerm;
    @Column(name = "level_four_secured_first_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelFourSecuredFirstCycleMaxAmount;
    @Column(name = "level_four_secured_first_cycle_min_term")
    private Integer levelFourSecuredFirstCycleMinTerm;
    @Column(name = "level_four_secured_first_cycle_max_term")
    private Integer levelFourSecuredFirstCycleMaxTerm;
    @Column(name = "level_four_secured_second_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelFourSecuredSecondCycleMaxAmount;
    @Column(name = "level_four_secured_second_cycle_min_term")
    private Integer levelFourSecuredSecondCycleMinTerm;
    @Column(name = "level_four_secured_second_cycle_max_term")
    private Integer levelFourSecuredSecondCycleMaxTerm;

    @Column(name = "level_five_unsecured_first_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelFiveUnsecuredFirstCycleMaxAmount;
    @Column(name = "level_five_unsecured_first_cycle_min_term")
    private Integer levelFiveUnsecuredFirstCycleMinTerm;
    @Column(name = "level_five_unsecured_first_cycle_max_term")
    private Integer levelFiveUnsecuredFirstCycleMaxTerm;
    @Column(name = "level_five_unsecured_second_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelFiveUnsecuredSecondCycleMaxAmount;
    @Column(name = "level_five_unsecured_second_cycle_min_term")
    private Integer levelFiveUnsecuredSecondCycleMinTerm;
    @Column(name = "level_five_unsecured_second_cycle_max_term")
    private Integer levelFiveUnsecuredSecondCycleMaxTerm;
    @Column(name = "level_five_secured_first_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelFiveSecuredFirstCycleMaxAmount;
    @Column(name = "level_five_secured_first_cycle_min_term")
    private Integer levelFiveSecuredFirstCycleMinTerm;
    @Column(name = "level_five_secured_first_cycle_max_term")
    private Integer levelFiveSecuredFirstCycleMaxTerm;
    @Column(name = "level_five_secured_second_cycle_max_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal levelFiveSecuredSecondCycleMaxAmount;
    @Column(name = "level_five_secured_second_cycle_min_term")
    private Integer levelFiveSecuredSecondCycleMinTerm;
    @Column(name = "level_five_secured_second_cycle_max_term")
    private Integer levelFiveSecuredSecondCycleMaxTerm;

    public LoanApprovalMatrix() {}

    public LoanApprovalMatrix(String currency, BigDecimal levelOneUnsecuredFirstCycleMaxAmount, Integer levelOneUnsecuredFirstCycleMinTerm,
            Integer levelOneUnsecuredFirstCycleMaxTerm, BigDecimal levelOneUnsecuredSecondCycleMaxAmount,
            Integer levelOneUnsecuredSecondCycleMinTerm, Integer levelOneUnsecuredSecondCycleMaxTerm,
            BigDecimal levelOneSecuredFirstCycleMaxAmount, Integer levelOneSecuredFirstCycleMinTerm,
            Integer levelOneSecuredFirstCycleMaxTerm, BigDecimal levelOneSecuredSecondCycleMaxAmount,
            Integer levelOneSecuredSecondCycleMinTerm, Integer levelOneSecuredSecondCycleMaxTerm,
            BigDecimal levelTwoUnsecuredFirstCycleMaxAmount, Integer levelTwoUnsecuredFirstCycleMinTerm,
            Integer levelTwoUnsecuredFirstCycleMaxTerm, BigDecimal levelTwoUnsecuredSecondCycleMaxAmount,
            Integer levelTwoUnsecuredSecondCycleMinTerm, Integer levelTwoUnsecuredSecondCycleMaxTerm,
            BigDecimal levelTwoSecuredFirstCycleMaxAmount, Integer levelTwoSecuredFirstCycleMinTerm,
            Integer levelTwoSecuredFirstCycleMaxTerm, BigDecimal levelTwoSecuredSecondCycleMaxAmount,
            Integer levelTwoSecuredSecondCycleMinTerm, Integer levelTwoSecuredSecondCycleMaxTerm,
            BigDecimal levelThreeUnsecuredFirstCycleMaxAmount, Integer levelThreeUnsecuredFirstCycleMinTerm,
            Integer levelThreeUnsecuredFirstCycleMaxTerm, BigDecimal levelThreeUnsecuredSecondCycleMaxAmount,
            Integer levelThreeUnsecuredSecondCycleMinTerm, Integer levelThreeUnsecuredSecondCycleMaxTerm,
            BigDecimal levelThreeSecuredFirstCycleMaxAmount, Integer levelThreeSecuredFirstCycleMinTerm,
            Integer levelThreeSecuredFirstCycleMaxTerm, BigDecimal levelThreeSecuredSecondCycleMaxAmount,
            Integer levelThreeSecuredSecondCycleMinTerm, Integer levelThreeSecuredSecondCycleMaxTerm,
            BigDecimal levelFourUnsecuredFirstCycleMaxAmount, Integer levelFourUnsecuredFirstCycleMinTerm,
            Integer levelFourUnsecuredFirstCycleMaxTerm, BigDecimal levelFourUnsecuredSecondCycleMaxAmount,
            Integer levelFourUnsecuredSecondCycleMinTerm, Integer levelFourUnsecuredSecondCycleMaxTerm,
            BigDecimal levelFourSecuredFirstCycleMaxAmount, Integer levelFourSecuredFirstCycleMinTerm,
            Integer levelFourSecuredFirstCycleMaxTerm, BigDecimal levelFourSecuredSecondCycleMaxAmount,
            Integer levelFourSecuredSecondCycleMinTerm, Integer levelFourSecuredSecondCycleMaxTerm,
            BigDecimal levelFiveUnsecuredFirstCycleMaxAmount, Integer levelFiveUnsecuredFirstCycleMinTerm,
            Integer levelFiveUnsecuredFirstCycleMaxTerm, BigDecimal levelFiveUnsecuredSecondCycleMaxAmount,
            Integer levelFiveUnsecuredSecondCycleMinTerm, Integer levelFiveUnsecuredSecondCycleMaxTerm,
            BigDecimal levelFiveSecuredFirstCycleMaxAmount, Integer levelFiveSecuredFirstCycleMinTerm,
            Integer levelFiveSecuredFirstCycleMaxTerm, BigDecimal levelFiveSecuredSecondCycleMaxAmount,
            Integer levelFiveSecuredSecondCycleMinTerm, Integer levelFiveSecuredSecondCycleMaxTerm) {
        this.currency = currency;
        this.levelOneUnsecuredFirstCycleMaxAmount = levelOneUnsecuredFirstCycleMaxAmount;
        this.levelOneUnsecuredFirstCycleMinTerm = levelOneUnsecuredFirstCycleMinTerm;
        this.levelOneUnsecuredFirstCycleMaxTerm = levelOneUnsecuredFirstCycleMaxTerm;
        this.levelOneUnsecuredSecondCycleMaxAmount = levelOneUnsecuredSecondCycleMaxAmount;
        this.levelOneUnsecuredSecondCycleMinTerm = levelOneUnsecuredSecondCycleMinTerm;
        this.levelOneUnsecuredSecondCycleMaxTerm = levelOneUnsecuredSecondCycleMaxTerm;
        this.levelOneSecuredFirstCycleMaxAmount = levelOneSecuredFirstCycleMaxAmount;
        this.levelOneSecuredFirstCycleMinTerm = levelOneSecuredFirstCycleMinTerm;
        this.levelOneSecuredFirstCycleMaxTerm = levelOneSecuredFirstCycleMaxTerm;
        this.levelOneSecuredSecondCycleMaxAmount = levelOneSecuredSecondCycleMaxAmount;
        this.levelOneSecuredSecondCycleMinTerm = levelOneSecuredSecondCycleMinTerm;
        this.levelOneSecuredSecondCycleMaxTerm = levelOneSecuredSecondCycleMaxTerm;

        this.levelTwoUnsecuredFirstCycleMaxAmount = levelTwoUnsecuredFirstCycleMaxAmount;
        this.levelTwoUnsecuredFirstCycleMinTerm = levelTwoUnsecuredFirstCycleMinTerm;
        this.levelTwoUnsecuredFirstCycleMaxTerm = levelTwoUnsecuredFirstCycleMaxTerm;
        this.levelTwoUnsecuredSecondCycleMaxAmount = levelTwoUnsecuredSecondCycleMaxAmount;
        this.levelTwoUnsecuredSecondCycleMinTerm = levelTwoUnsecuredSecondCycleMinTerm;
        this.levelTwoUnsecuredSecondCycleMaxTerm = levelTwoUnsecuredSecondCycleMaxTerm;
        this.levelTwoSecuredFirstCycleMaxAmount = levelTwoSecuredFirstCycleMaxAmount;
        this.levelTwoSecuredFirstCycleMinTerm = levelTwoSecuredFirstCycleMinTerm;
        this.levelTwoSecuredFirstCycleMaxTerm = levelTwoSecuredFirstCycleMaxTerm;
        this.levelTwoSecuredSecondCycleMaxAmount = levelTwoSecuredSecondCycleMaxAmount;
        this.levelTwoSecuredSecondCycleMinTerm = levelTwoSecuredSecondCycleMinTerm;
        this.levelTwoSecuredSecondCycleMaxTerm = levelTwoSecuredSecondCycleMaxTerm;

        this.levelThreeUnsecuredFirstCycleMaxAmount = levelThreeUnsecuredFirstCycleMaxAmount;
        this.levelThreeUnsecuredFirstCycleMinTerm = levelThreeUnsecuredFirstCycleMinTerm;
        this.levelThreeUnsecuredFirstCycleMaxTerm = levelThreeUnsecuredFirstCycleMaxTerm;
        this.levelThreeUnsecuredSecondCycleMaxAmount = levelThreeUnsecuredSecondCycleMaxAmount;
        this.levelThreeUnsecuredSecondCycleMinTerm = levelThreeUnsecuredSecondCycleMinTerm;
        this.levelThreeUnsecuredSecondCycleMaxTerm = levelThreeUnsecuredSecondCycleMaxTerm;
        this.levelThreeSecuredFirstCycleMaxAmount = levelThreeSecuredFirstCycleMaxAmount;
        this.levelThreeSecuredFirstCycleMinTerm = levelThreeSecuredFirstCycleMinTerm;
        this.levelThreeSecuredFirstCycleMaxTerm = levelThreeSecuredFirstCycleMaxTerm;
        this.levelThreeSecuredSecondCycleMaxAmount = levelThreeSecuredSecondCycleMaxAmount;
        this.levelThreeSecuredSecondCycleMinTerm = levelThreeSecuredSecondCycleMinTerm;
        this.levelThreeSecuredSecondCycleMaxTerm = levelThreeSecuredSecondCycleMaxTerm;

        this.levelFourUnsecuredFirstCycleMaxAmount = levelFourUnsecuredFirstCycleMaxAmount;
        this.levelFourUnsecuredFirstCycleMinTerm = levelFourUnsecuredFirstCycleMinTerm;
        this.levelFourUnsecuredFirstCycleMaxTerm = levelFourUnsecuredFirstCycleMaxTerm;
        this.levelFourUnsecuredSecondCycleMaxAmount = levelFourUnsecuredSecondCycleMaxAmount;
        this.levelFourUnsecuredSecondCycleMinTerm = levelFourUnsecuredSecondCycleMinTerm;
        this.levelFourUnsecuredSecondCycleMaxTerm = levelFourUnsecuredSecondCycleMaxTerm;
        this.levelFourSecuredFirstCycleMaxAmount = levelFourSecuredFirstCycleMaxAmount;
        this.levelFourSecuredFirstCycleMinTerm = levelFourSecuredFirstCycleMinTerm;
        this.levelFourSecuredFirstCycleMaxTerm = levelFourSecuredFirstCycleMaxTerm;
        this.levelFourSecuredSecondCycleMaxAmount = levelFourSecuredSecondCycleMaxAmount;
        this.levelFourSecuredSecondCycleMinTerm = levelFourSecuredSecondCycleMinTerm;
        this.levelFourSecuredSecondCycleMaxTerm = levelFourSecuredSecondCycleMaxTerm;

        this.levelFiveUnsecuredFirstCycleMaxAmount = levelFiveUnsecuredFirstCycleMaxAmount;
        this.levelFiveUnsecuredFirstCycleMinTerm = levelFiveUnsecuredFirstCycleMinTerm;
        this.levelFiveUnsecuredFirstCycleMaxTerm = levelFiveUnsecuredFirstCycleMaxTerm;
        this.levelFiveUnsecuredSecondCycleMaxAmount = levelFiveUnsecuredSecondCycleMaxAmount;
        this.levelFiveUnsecuredSecondCycleMinTerm = levelFiveUnsecuredSecondCycleMinTerm;
        this.levelFiveUnsecuredSecondCycleMaxTerm = levelFiveUnsecuredSecondCycleMaxTerm;
        this.levelFiveSecuredFirstCycleMaxAmount = levelFiveSecuredFirstCycleMaxAmount;
        this.levelFiveSecuredFirstCycleMinTerm = levelFiveSecuredFirstCycleMinTerm;
        this.levelFiveSecuredFirstCycleMaxTerm = levelFiveSecuredFirstCycleMaxTerm;
        this.levelFiveSecuredSecondCycleMaxAmount = levelFiveSecuredSecondCycleMaxAmount;
        this.levelFiveSecuredSecondCycleMinTerm = levelFiveSecuredSecondCycleMinTerm;
        this.levelFiveSecuredSecondCycleMaxTerm = levelFiveSecuredSecondCycleMaxTerm;
    }
}
