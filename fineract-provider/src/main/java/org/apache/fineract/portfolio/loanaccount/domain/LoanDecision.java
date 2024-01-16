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
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.useradministration.domain.AppUser;

@Data
@Entity
@Table(name = "m_loan_decision")
public class LoanDecision extends AbstractAuditableCustom {

    @OneToOne
    @JoinColumn(name = "loan_id")
    private Loan loan;
    @Column(name = "loan_decision_state")
    private Integer loanDecisionState;
    @Column(name = "review_application_note")
    private String reviewApplicationNote;
    @Column(name = "is_review_application_signed")
    private Boolean reviewApplicationSigned;
    @Column(name = "is_reject_review_application")
    private Boolean rejectReviewApplicationSigned;
    @Column(name = "review_application_on")
    private LocalDate reviewApplicationOn;
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "review_application_by")
    private AppUser reviewApplicationBy;
    // Due diligence
    @Column(name = "due_diligence_note")
    private String dueDiligenceNote;
    @Column(name = "is_due_diligence_signed")
    private Boolean dueDiligenceSigned;
    @Column(name = "is_reject_due_diligence")
    private Boolean rejectDueDiligence;
    @Column(name = "due_diligence_on")
    private LocalDate dueDiligenceOn;
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "due_diligence_by")
    private AppUser dueDiligenceBy;
    @Column(name = "due_diligence_recommended_amount")
    private BigDecimal dueDiligenceRecommendedAmount;
    @Column(name = "due_diligence_term_frequency")
    private Integer dueDiligenceTermFrequency;
    @Column(name = "due_diligence_term_period_frequency_type")
    private Integer dueDiligenceTermFrequencyType;


    // collateral review
    @Column(name = "collateral_review_note")
    private String collateralReviewNote;
    @Column(name = "is_collateral_review_signed")
    private Boolean collateralReviewSigned;
    @Column(name = "is_reject_collateral_review")
    private Boolean rejectCollateralReviewSigned;
    @Column(name = "collateral_review_on")
    private LocalDate collateralReviewOn;
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "collateral_review_by")
    private AppUser collateralReviewBy;

    // IC review Decision Level One
    @Column(name = "ic_review_decision_level_one_note")
    private String icReviewDecisionLevelOneNote;
    @Column(name = "is_ic_review_decision_level_one_signed")
    private Boolean icReviewDecisionLevelOneSigned;
    @Column(name = "is_reject_ic_review_decision_level_one")
    private Boolean rejectIcReviewDecisionLevelOneSigned;
    @Column(name = "ic_review_decision_level_one_on")
    private LocalDate icReviewDecisionLevelOneOn;
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "ic_review_decision_level_one_by")
    private AppUser icReviewDecisionLevelOneBy;
    @Column(name = "ic_review_decision_level_one_recommended_amount")
    private BigDecimal icReviewDecisionLevelOneRecommendedAmount;
    @Column(name = "ic_review_decision_level_one_term_frequency")
    private Integer icReviewDecisionLevelOneTermFrequency;
    @Column(name = "ic_review_decision_level_one_term_period_frequency_enum")
    private Integer icReviewDecisionLevelOneTermPeriodFrequencyEnum;

    @Column(name = "next_loan_ic_review_decision_state")
    private Integer nextLoanIcReviewDecisionState;

    // IC review Decision Level Two
    @Column(name = "ic_review_decision_level_two_note")
    private String icReviewDecisionLevelTwoNote;
    @Column(name = "is_ic_review_decision_level_two_signed")
    private Boolean icReviewDecisionLevelTwoSigned;
    @Column(name = "is_reject_ic_review_decision_level_two")
    private Boolean rejectIcReviewDecisionLevelTwoSigned;
    @Column(name = "ic_review_decision_level_two_on")
    private LocalDate icReviewDecisionLevelTwoOn;
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "ic_review_decision_level_two_by")
    private AppUser icReviewDecisionLevelTwoBy;
    @Column(name = "ic_review_decision_level_two_recommended_amount")
    private BigDecimal icReviewDecisionLevelTwoRecommendedAmount;
    @Column(name = "ic_review_decision_level_two_term_frequency")
    private Integer icReviewDecisionLevelTwoTermFrequency;
    @Column(name = "ic_review_decision_level_two_term_period_frequency_enum")
    private Integer icReviewDecisionLevelTwoTermPeriodFrequencyEnum;

    // IC review Decision Level Three
    @Column(name = "ic_review_decision_level_three_note")
    private String icReviewDecisionLevelThreeNote;
    @Column(name = "is_ic_review_decision_level_three_signed")
    private Boolean icReviewDecisionLevelThreeSigned;
    @Column(name = "is_reject_ic_review_decision_level_three")
    private Boolean rejectIcReviewDecisionLevelThreeSigned;
    @Column(name = "ic_review_decision_level_three_on")
    private LocalDate icReviewDecisionLevelThreeOn;
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "ic_review_decision_level_three_by")
    private AppUser icReviewDecisionLevelThreeBy;
    @Column(name = "ic_review_decision_level_three_recommended_amount")
    private BigDecimal icReviewDecisionLevelThreeRecommendedAmount;
    @Column(name = "ic_review_decision_level_three_term_frequency")
    private Integer icReviewDecisionLevelThreeTermFrequency;
    @Column(name = "ic_review_decision_level_three_term_period_frequency_enum")
    private Integer icReviewDecisionLevelThreeTermPeriodFrequencyEnum;

    // IC review Decision Level Four
    @Column(name = "ic_review_decision_level_four_note")
    private String icReviewDecisionLevelFourNote;
    @Column(name = "is_ic_review_decision_level_four_signed")
    private Boolean icReviewDecisionLevelFourSigned;
    @Column(name = "is_reject_ic_review_decision_level_four")
    private Boolean rejectIcReviewDecisionLevelFourSigned;
    @Column(name = "ic_review_decision_level_four_on")
    private LocalDate icReviewDecisionLevelFourOn;
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "ic_review_decision_level_four_by")
    private AppUser icReviewDecisionLevelFourBy;
    @Column(name = "ic_review_decision_level_four_recommended_amount")
    private BigDecimal icReviewDecisionLevelFourRecommendedAmount;
    @Column(name = "ic_review_decision_level_four_term_frequency")
    private Integer icReviewDecisionLevelFourTermFrequency;
    @Column(name = "ic_review_decision_level_four_term_period_frequency_enum")
    private Integer icReviewDecisionLevelFourTermPeriodFrequencyEnum;

    // IC review Decision Level Five
    @Column(name = "ic_review_decision_level_five_note")
    private String icReviewDecisionLevelFiveNote;
    @Column(name = "is_ic_review_decision_level_five_signed")
    private Boolean icReviewDecisionLevelFiveSigned;
    @Column(name = "is_reject_ic_review_decision_level_five")
    private Boolean rejectIcReviewDecisionLevelFiveSigned;
    @Column(name = "ic_review_decision_level_five_on")
    private LocalDate icReviewDecisionLevelFiveOn;
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "ic_review_decision_level_five_by")
    private AppUser icReviewDecisionLevelFiveBy;
    @Column(name = "ic_review_decision_level_five_recommended_amount")
    private BigDecimal icReviewDecisionLevelFiveRecommendedAmount;
    @Column(name = "ic_review_decision_level_five_term_frequency")
    private Integer icReviewDecisionLevelFiveTermFrequency;
    @Column(name = "ic_review_decision_level_five_term_period_frequency_enum")
    private Integer icReviewDecisionLevelFiveTermPeriodFrequencyEnum;

    // Prepare and sign contract
    @Column(name = "prepare_and_sign_contract_note")
    private String prepareAndSignContractNote;
    @Column(name = "is_prepare_and_sign_contract_signed")
    private Boolean prepareAndSignContractSigned;
    @Column(name = "is_reject_prepare_and_sign_contract")
    private Boolean rejectPrepareAndSignContractSigned;
    @Column(name = "prepare_and_sign_contract_on")
    private LocalDate prepareAndSignContractOn;
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "prepare_and_sign_contract_by")
    private AppUser prepareAndSignContractBy;

    public LoanDecision() {}

    public static LoanDecision reviewApplication(Loan loan, Integer loanDecisionState, String reviewApplicationNote,
            Boolean reviewApplicationSigned, Boolean rejectReviewApplicationSigned, LocalDate reviewApplicationOn,
            AppUser reviewApplicationBy) {
        return new LoanDecision(loan, loanDecisionState, reviewApplicationNote, reviewApplicationSigned, rejectReviewApplicationSigned,
                reviewApplicationOn, reviewApplicationBy);
    }

    public LoanDecision(Loan loan, Integer loanDecisionState, String reviewApplicationNote, Boolean reviewApplicationSigned,
            Boolean rejectReviewApplicationSigned, LocalDate reviewApplicationOn, AppUser reviewApplicationBy) {
        this.loan = loan;
        this.loanDecisionState = loanDecisionState;
        this.reviewApplicationNote = reviewApplicationNote;
        this.reviewApplicationSigned = reviewApplicationSigned;
        this.rejectReviewApplicationSigned = rejectReviewApplicationSigned;
        this.reviewApplicationOn = reviewApplicationOn;
        this.reviewApplicationBy = reviewApplicationBy;
        this.dueDiligenceSigned = Boolean.FALSE;
        this.rejectDueDiligence = Boolean.FALSE;
        this.collateralReviewSigned = Boolean.FALSE;
        this.rejectCollateralReviewSigned = Boolean.FALSE;
        this.rejectIcReviewDecisionLevelOneSigned = Boolean.FALSE;
        this.icReviewDecisionLevelOneSigned = Boolean.FALSE;
        this.rejectIcReviewDecisionLevelTwoSigned = Boolean.FALSE;
        this.icReviewDecisionLevelTwoSigned = Boolean.FALSE;
        this.rejectIcReviewDecisionLevelThreeSigned = Boolean.FALSE;
        this.icReviewDecisionLevelThreeSigned = Boolean.FALSE;
        this.rejectIcReviewDecisionLevelFourSigned = Boolean.FALSE;
        this.icReviewDecisionLevelFourSigned = Boolean.FALSE;
        this.rejectIcReviewDecisionLevelFiveSigned = Boolean.FALSE;
        this.icReviewDecisionLevelFiveSigned = Boolean.FALSE;
        this.rejectPrepareAndSignContractSigned = Boolean.FALSE;
        this.prepareAndSignContractSigned = Boolean.FALSE;
    }
}
