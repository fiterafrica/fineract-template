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
package org.apache.fineract.portfolio.loanaccount.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstance;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstanceRepository;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.api.LoanApprovalMatrixConstants;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanApprovalMatrix;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCollateralManagement;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCollateralManagementRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecision;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecisionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecisionState;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleAssembler;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanDecisionStateUtilService {

    private final ConfigurationReadPlatformService configurationReadPlatformService;
    private final LoanDecisionRepository loanDecisionRepository;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final LoanUtilService loanUtilService;
    private final LoanScheduleAssembler loanScheduleAssembler;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanCollateralManagementRepository loanCollateralManagementRepository;

    public void validateLoanAccountWithExtraLoanDecisionStagesConfiguredGlobally(Loan loan, final JsonCommand command) {
        final Boolean isExtendLoanLifeCycleConfig = isExtendLoanLifeCycleConfig();

        LocalDate approveOnDate = command.localDateValueOfParameterNamed(LoanApiConstants.approvedOnDateParameterName);
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());
        if (isExtendLoanLifeCycleConfig) {
            if (loanDecision == null) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.account.should.extend.decision.engine.to.review.Loan.application",
                        "Loan Account is not permitted for Approval since new workflow [Add-More-Stages-To-A-Loan-Life-Cycle] is activated and next status is [Review Application]");
            }
            if (loanDecision != null && !loanDecision.getReviewApplicationSigned()) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.account.should.extend.decision.engine.required.state.is.review.application",
                        "Loan Account is not permitted for Approval . [Review Application ] to proceed ");
            }

            if (loanDecision.getPrepareAndSignContractOn() != null && approveOnDate.isBefore(loanDecision.getPrepareAndSignContractOn())) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.Approval.date.should.be.after.loan.prepare.and.sign.contract.date",
                        "Approval on  date" + approveOnDate + " should be after Prepare And Sign Contract date "
                                + loanDecision.getPrepareAndSignContractOn());
            }

            if (loanDecision.getIcReviewDecisionLevelFiveOn() != null
                    && approveOnDate.isBefore(loanDecision.getIcReviewDecisionLevelFiveOn())) {
                throw new GeneralPlatformDomainRuleException("error.msg.Approval.date.should.be.after.Ic.Review.decision.level.Five.date",
                        "Approval on  date" + approveOnDate + " should be after IC ReviewDecision Level Five date "
                                + loanDecision.getIcReviewDecisionLevelFiveOn());
            }

            if (loanDecision.getIcReviewDecisionLevelFourOn() != null
                    && approveOnDate.isBefore(loanDecision.getIcReviewDecisionLevelFourOn())) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.Approval.date.should.be.after.Ic.Review.decision.level.Four.date",
                        "Approval on  date" + approveOnDate + " should be after IC ReviewDecision Level Four date "
                                + loanDecision.getIcReviewDecisionLevelFourOn());
            }
            if (loanDecision.getIcReviewDecisionLevelThreeOn() != null
                    && approveOnDate.isBefore(loanDecision.getIcReviewDecisionLevelThreeOn())) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.Approval.date.should.be.after.Ic.Review.decision.level.Three.date",
                        "Approval on  date" + approveOnDate + " should be after IC ReviewDecision Level Three date "
                                + loanDecision.getIcReviewDecisionLevelThreeOn());
            }

            if (loanDecision.getIcReviewDecisionLevelTwoOn() != null
                    && approveOnDate.isBefore(loanDecision.getIcReviewDecisionLevelTwoOn())) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.Approval.date.should.be.after.Ic.Review.decision.level.Two.date",
                        "Approval on  date" + approveOnDate + " should be after IC ReviewDecision Level two date "
                                + loanDecision.getIcReviewDecisionLevelTwoOn());
            }
            if (approveOnDate.isBefore(loanDecision.getIcReviewDecisionLevelOneOn())) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.Approval.date.should.be.after.Ic.Review.decision.level.one.date",
                        "Approval on  date" + approveOnDate + " should be after IC ReviewDecision Level One date "
                                + loanDecision.getIcReviewDecisionLevelOneOn());
            }
            if (approveOnDate.isBefore(loanDecision.getCollateralReviewOn())) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.Approval.date.should.be.after.collateral.review.date",
                        "Approval on  date" + approveOnDate + " should be after Collateral Review Approved date "
                                + loanDecision.getCollateralReviewOn());
            }
            if (approveOnDate.isBefore(loanDecision.getDueDiligenceOn())) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.Approval.date.should.be.after.Due.Diligence.date",
                        "Approval on date" + approveOnDate + " should be after Loan Due Diligence Approved date "
                                + loanDecision.getDueDiligenceOn());
            }
            if (approveOnDate.isBefore(loanDecision.getReviewApplicationOn())) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.Approval.date.should.be.after.review.application.date",
                        "Approval on date" + approveOnDate + " should be after Loan Review Application Approved date "
                                + loanDecision.getReviewApplicationOn());
            }
            if (!loan.getLoanDecisionState().equals(loanDecision.getLoanDecisionState())) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.does.not.reconcile",
                        "Loan Account Decision state Does not reconcile . Operation is terminated");
            }
            if (!LoanDecisionState.fromInt(loan.getLoanDecisionState()).isPrepareAndSignContract()) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.approval.is.terminated.expected.state.is.prepare.and.sign.contract.but.found.different",
                        "Loan Account can't be approved because it's Decision state is invalid. Expected "
                                + LoanDecisionState.PREPARE_AND_SIGN_CONTRACT.getValue() + " but found " + loan.getLoanDecisionState());
            }
        }
    }

    public void validateLoanReviewApplicationStateIsFiredBeforeDisbursal(Loan loan, final JsonCommand command) {
        final Boolean isExtendLoanLifeCycleConfig = isExtendLoanLifeCycleConfig();

        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());

        LocalDate actualDisbursementDate = command.localDateValueOfParameterNamed("actualDisbursementDate");

        if (isExtendLoanLifeCycleConfig) {

            if (loanDecision == null) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.account.should.extend.decision.engine.to.review.Loan.application.to.be.disbursed",
                        "Loan Account is not permitted for Disbursement since new workflow [Add-More-Stages-To-A-Loan-Life-Cycle] is activated and next stage is [Review Application]");
            }
            if (loanDecision != null && !loanDecision.getReviewApplicationSigned()) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.account.should.extend.decision.engine.required.state.is.review.application",
                        "Loan Account is not permitted for Disbursement . [Review Application ] to proceed ");
            }

            if (loanDecision.getPrepareAndSignContractOn() != null
                    && actualDisbursementDate.isBefore(loanDecision.getPrepareAndSignContractOn())) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.Disbursement.date.should.be.after.loan.prepare.and.sign.contract.date",
                        "Disbursement on  date" + actualDisbursementDate + " should be after Prepare And Sign Contract date "
                                + loanDecision.getPrepareAndSignContractOn());
            }

            if (loanDecision.getIcReviewDecisionLevelFiveOn() != null
                    && actualDisbursementDate.isBefore(loanDecision.getIcReviewDecisionLevelFiveOn())) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.Disbursement.date.should.be.after.Ic.Review.decision.level.Five.date",
                        "Disbursement on  date" + actualDisbursementDate + " should be after IC ReviewDecision Level Five date "
                                + loanDecision.getIcReviewDecisionLevelFiveOn());
            }

            if (loanDecision.getIcReviewDecisionLevelFourOn() != null
                    && actualDisbursementDate.isBefore(loanDecision.getIcReviewDecisionLevelFourOn())) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.Disbursement.date.should.be.after.Ic.Review.decision.level.Four.date",
                        "Disbursement on  date" + actualDisbursementDate + " should be after IC ReviewDecision Level Four date "
                                + loanDecision.getIcReviewDecisionLevelFourOn());
            }
            if (loanDecision.getIcReviewDecisionLevelThreeOn() != null
                    && actualDisbursementDate.isBefore(loanDecision.getIcReviewDecisionLevelThreeOn())) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.Disbursement.date.should.be.after.Ic.Review.decision.level.Three.date",
                        "Disbursement on  date" + actualDisbursementDate + " should be after IC ReviewDecision Level Three date "
                                + loanDecision.getIcReviewDecisionLevelThreeOn());
            }

            if (loanDecision.getIcReviewDecisionLevelTwoOn() != null
                    && actualDisbursementDate.isBefore(loanDecision.getIcReviewDecisionLevelTwoOn())) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.Disbursement.date.should.be.after.Ic.Review.decision.level.Two.date",
                        "Disbursement on  date" + actualDisbursementDate + " should be after IC ReviewDecision Level two date "
                                + loanDecision.getIcReviewDecisionLevelTwoOn());
            }
            if (actualDisbursementDate.isBefore(loanDecision.getIcReviewDecisionLevelOneOn())) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.Disbursement.date.should.be.after.Ic.Review.decision.level.one.date",
                        "Disbursement on  date" + actualDisbursementDate + " should be after IC ReviewDecision Level One date "
                                + loanDecision.getIcReviewDecisionLevelOneOn());
            }
            if (actualDisbursementDate.isBefore(loanDecision.getCollateralReviewOn())) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.Disbursement.date.should.be.after.collateral.review.date",
                        "Disbursement on  date" + actualDisbursementDate + " should be after Collateral Review Approved date "
                                + loanDecision.getCollateralReviewOn());
            }
            if (actualDisbursementDate.isBefore(loanDecision.getDueDiligenceOn())) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.Disbursement.date.should.be.after.Due.Diligence.date",
                        "Disbursement on date" + actualDisbursementDate + " should be after Loan Due Diligence Approved date "
                                + loanDecision.getDueDiligenceOn());
            }
            if (actualDisbursementDate.isBefore(loanDecision.getReviewApplicationOn())) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.Disbursement.date.should.be.after.review.application.date",
                        "Disbursement on date" + actualDisbursementDate + " should be after Loan Review Application Approved date "
                                + loanDecision.getReviewApplicationOn());
            }
            if (!loan.getLoanDecisionState().equals(loanDecision.getLoanDecisionState())) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.does.not.reconcile",
                        "Loan Account Decision state Does not reconcile . Operation is terminated");
            }
            if (!LoanDecisionState.fromInt(loan.getLoanDecisionState()).isPrepareAndSignContract()) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.Disbursement.is.terminated.expected.state.is.prepare.and.sign.contract.but.found.different",
                        "Loan Account can't be Disbursement because it's Decision state is invalid. Expected "
                                + LoanDecisionState.PREPARE_AND_SIGN_CONTRACT.getValue() + " but found " + loan.getLoanDecisionState());
            }
        }
    }

    @NotNull
    public Boolean isExtendLoanLifeCycleConfig() {
        final GlobalConfigurationPropertyData extendLoanLifeCycleConfig = this.configurationReadPlatformService
                .retrieveGlobalConfiguration("Add-More-Stages-To-A-Loan-Life-Cycle");
        final Boolean isExtendLoanLifeCycleConfig = extendLoanLifeCycleConfig.isEnabled();
        return isExtendLoanLifeCycleConfig;
    }

    @NotNull
    public Boolean isLoanAccountInICReview(LoanDecisionState loanDecisionState) {
        switch (loanDecisionState) {
            case COLLATERAL_REVIEW: // For the loan to have collateral review, it's next stage is IC Review Level One
                                    // Making COLLATERAL_REVIEW necessary to this action
            case IC_REVIEW_LEVEL_ONE:
            case IC_REVIEW_LEVEL_TWO:
            case IC_REVIEW_LEVEL_THREE:
            case IC_REVIEW_LEVEL_FOUR:
                return true;
            default:
                return false;
        }
    }

    private void validateLoanAccountToComplyToApprovalMatrixLevelTwo(Loan loan, LoanApprovalMatrix approvalMatrix, Boolean isLoanFirstCycle,
            Boolean isLoanUnsecure) {
        if (isLoanFirstCycle && isLoanUnsecure) {
            // Loan is FirstCycle and Unsecure
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Two.unsecured.first.cycle";
            String state = "Level Two Unsecured first cycle ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelTwoUnsecuredFirstCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelTwoUnsecuredFirstCycleMinTerm(), approvalMatrix.getLevelTwoUnsecuredFirstCycleMaxTerm(),
                    errormsg, state, approvalMatrix.getLevelOneUnsecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && isLoanUnsecure) {
            // Loan is (Second cycle or plus) and Unsecure
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Two.unsecured.second.cycle plus";
            String state = "Level Two Unsecured second cycle plus ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelTwoUnsecuredSecondCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelTwoUnsecuredSecondCycleMinTerm(), approvalMatrix.getLevelTwoUnsecuredSecondCycleMaxTerm(),
                    errormsg, state, approvalMatrix.getLevelOneUnsecuredSecondCycleMaxAmount());

        } else if (isLoanFirstCycle && !isLoanUnsecure) {
            // First Cycle and secured Loan
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Two.secured.first.cycle";
            String state = "Level Two secured first cycle ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelTwoSecuredFirstCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelTwoSecuredFirstCycleMinTerm(), approvalMatrix.getLevelTwoSecuredFirstCycleMaxTerm(), errormsg,
                    state, approvalMatrix.getLevelOneSecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && !isLoanUnsecure) {
            // Second Cycle or plus and secured
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Two.secured.second.cycle plus";
            String state = "Level Two Secured second cycle plus ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelTwoSecuredSecondCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelTwoSecuredSecondCycleMinTerm(), approvalMatrix.getLevelTwoSecuredSecondCycleMaxTerm(), errormsg,
                    state, approvalMatrix.getLevelOneSecuredSecondCycleMaxAmount());
        } else {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.invalid.loan.principal.not.matching.approval.matrix.in.IC.review.Level.Two",
                    String.format("This Loan Account Principal [ %s ] , does not match  IC Review Level Two Operations .",
                            loan.getProposedPrincipal()));
        }
    }

    private void validateLoanAccountToComplyToApprovalMatrixLevelThree(Loan loan, LoanApprovalMatrix approvalMatrix,
            Boolean isLoanFirstCycle, Boolean isLoanUnsecure) {
        if (isLoanFirstCycle && isLoanUnsecure) {
            // Loan is FirstCycle and Unsecure
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Three.unsecured.first.cycle";
            String state = "Level Three Unsecured first cycle ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelThreeUnsecuredFirstCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelThreeUnsecuredFirstCycleMinTerm(), approvalMatrix.getLevelThreeUnsecuredFirstCycleMaxTerm(),
                    errormsg, state, approvalMatrix.getLevelTwoUnsecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && isLoanUnsecure) {
            // Loan is (Second cycle or plus) and Unsecure
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Three.unsecured.second.cycle plus";
            String state = "Level Three Unsecured second cycle plus ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelThreeUnsecuredSecondCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelThreeUnsecuredSecondCycleMinTerm(), approvalMatrix.getLevelThreeUnsecuredSecondCycleMaxTerm(),
                    errormsg, state, approvalMatrix.getLevelTwoUnsecuredSecondCycleMaxAmount());

        } else if (isLoanFirstCycle && !isLoanUnsecure) {
            // First Cycle and secured Loan
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Three.secured.first.cycle";
            String state = "Level Three secured first cycle ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelThreeSecuredFirstCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelThreeSecuredFirstCycleMinTerm(), approvalMatrix.getLevelThreeSecuredFirstCycleMaxTerm(),
                    errormsg, state, approvalMatrix.getLevelTwoSecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && !isLoanUnsecure) {
            // Second Cycle or plus and secured
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Three.secured.second.cycle plus";
            String state = "Level Three Secured second cycle plus ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelThreeSecuredSecondCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelThreeSecuredSecondCycleMinTerm(), approvalMatrix.getLevelThreeSecuredSecondCycleMaxTerm(),
                    errormsg, state, approvalMatrix.getLevelTwoSecuredSecondCycleMaxAmount());
        } else {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.invalid.loan.principal.not.matching.approval.matrix.in.IC.review.Level.Three",
                    String.format("This Loan Account Principal [ %s ] , does not match  IC Review Level Three Operations .",
                            loan.getProposedPrincipal()));
        }
    }

    private void validateLoanAccountToComplyToApprovalMatrixLevelFour(Loan loan, LoanApprovalMatrix approvalMatrix,
            Boolean isLoanFirstCycle, Boolean isLoanUnsecure) {
        if (isLoanFirstCycle && isLoanUnsecure) {
            // Loan is FirstCycle and Unsecure
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Four.unsecured.first.cycle";
            String state = "Level Four Unsecured first cycle ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelFourUnsecuredFirstCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelFourUnsecuredFirstCycleMinTerm(), approvalMatrix.getLevelFourUnsecuredFirstCycleMaxTerm(),
                    errormsg, state, approvalMatrix.getLevelThreeUnsecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && isLoanUnsecure) {
            // Loan is (Second cycle or plus) and Unsecure
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Four.unsecured.second.cycle plus";
            String state = "Level Four Unsecured second cycle plus ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelFourUnsecuredSecondCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelFourUnsecuredSecondCycleMinTerm(), approvalMatrix.getLevelFourUnsecuredSecondCycleMaxTerm(),
                    errormsg, state, approvalMatrix.getLevelThreeUnsecuredSecondCycleMaxAmount());

        } else if (isLoanFirstCycle && !isLoanUnsecure) {
            // First Cycle and secured Loan
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Four.secured.first.cycle";
            String state = "Level Four secured first cycle ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelFourSecuredFirstCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelFourSecuredFirstCycleMinTerm(), approvalMatrix.getLevelFourSecuredFirstCycleMaxTerm(), errormsg,
                    state, approvalMatrix.getLevelThreeSecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && !isLoanUnsecure) {
            // Second Cycle or plus and secured
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Four.secured.second.cycle plus";
            String state = "Level Four Secured second cycle plus ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelFourSecuredSecondCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelFourSecuredSecondCycleMinTerm(), approvalMatrix.getLevelFourSecuredSecondCycleMaxTerm(),
                    errormsg, state, approvalMatrix.getLevelThreeSecuredSecondCycleMaxAmount());
        } else {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.invalid.loan.principal.not.matching.approval.matrix.in.IC.review.Level.Four",
                    String.format("This Loan Account Principal [ %s ] , does not match  IC Review Level Four Operations .",
                            loan.getProposedPrincipal()));
        }
    }

    private void validateLoanAccountToComplyToApprovalMatrixLevelFive(Loan loan, LoanApprovalMatrix approvalMatrix,
            Boolean isLoanFirstCycle, Boolean isLoanUnsecure) {
        if (isLoanFirstCycle && isLoanUnsecure) {
            // Loan is FirstCycle and Unsecure
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Five.unsecured.first.cycle";
            String state = "Level Five Unsecured first cycle ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelFive(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelFiveUnsecuredFirstCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelFiveUnsecuredFirstCycleMinTerm(), approvalMatrix.getLevelFiveUnsecuredFirstCycleMaxTerm(),
                    errormsg, state, approvalMatrix.getLevelFourUnsecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && isLoanUnsecure) {
            // Loan is (Second cycle or plus) and Unsecure
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Five.unsecured.second.cycle plus";
            String state = "Level Five Unsecured second cycle plus ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelFive(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelFiveUnsecuredSecondCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelFiveUnsecuredSecondCycleMinTerm(), approvalMatrix.getLevelFiveUnsecuredSecondCycleMaxTerm(),
                    errormsg, state, approvalMatrix.getLevelFourUnsecuredSecondCycleMaxAmount());

        } else if (isLoanFirstCycle && !isLoanUnsecure) {
            // First Cycle and secured Loan
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Five.secured.first.cycle";
            String state = "Level Five secured first cycle ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelFive(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelFiveSecuredFirstCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelFiveSecuredFirstCycleMinTerm(), approvalMatrix.getLevelFiveSecuredFirstCycleMaxTerm(), errormsg,
                    state, approvalMatrix.getLevelFourSecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && !isLoanUnsecure) {
            // Second Cycle or plus and secured
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.Five.secured.second.cycle plus";
            String state = "Level Five Secured second cycle plus ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelFive(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelFiveSecuredSecondCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelFiveSecuredSecondCycleMinTerm(), approvalMatrix.getLevelFiveSecuredSecondCycleMaxTerm(),
                    errormsg, state, approvalMatrix.getLevelFourSecuredSecondCycleMaxAmount());
        } else {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.invalid.loan.principal.not.matching.approval.matrix.in.IC.review.Level.Five",
                    String.format("This Loan Account Principal [ %s ] , does not match  IC Review Level Five Operations .",
                            loan.getProposedPrincipal()));
        }
    }

    private void validateLoanAccountToComplyToApprovalMatrixLevelOne(Loan loan, LoanApprovalMatrix approvalMatrix, Boolean isLoanFirstCycle,
            Boolean isLoanUnsecure) {
        if (isLoanFirstCycle && isLoanUnsecure) {
            // Loan is FirstCycle and Unsecure
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.one.unsecured.first.cycle";
            String state = "Level One Unsecured first cycle ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelOne(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelOneUnsecuredFirstCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelOneUnsecuredFirstCycleMinTerm(), approvalMatrix.getLevelOneUnsecuredFirstCycleMaxTerm(),
                    errormsg, state);

        } else if (!isLoanFirstCycle && isLoanUnsecure) {
            // Loan is (Second cycle or plus) and Unsecure
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.one.unsecured.second.cycle plus";
            String state = "Level One Unsecured second cycle plus ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelOne(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelOneUnsecuredSecondCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelOneUnsecuredSecondCycleMinTerm(), approvalMatrix.getLevelOneUnsecuredSecondCycleMaxTerm(),
                    errormsg, state);

        } else if (isLoanFirstCycle && !isLoanUnsecure) {
            // First Cycle and secured Loan
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.one.secured.first.cycle";
            String state = "Level One secured first cycle ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelOne(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelOneSecuredFirstCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelOneSecuredFirstCycleMinTerm(), approvalMatrix.getLevelOneSecuredFirstCycleMaxTerm(), errormsg,
                    state);

        } else if (!isLoanFirstCycle && !isLoanUnsecure) {
            // Second Cycle or plus and secured
            String errormsg = "error.msg.invalid.loan.principal.does.not.qualify.for.IC-review.level.one.secured.second.cycle plus";
            String state = "Level One Secured second cycle plus ";
            validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelOne(loan.getProposedPrincipal(),
                    approvalMatrix.getLevelOneSecuredSecondCycleMaxAmount(), loan.getNumberOfRepayments(),
                    approvalMatrix.getLevelOneSecuredSecondCycleMinTerm(), approvalMatrix.getLevelOneSecuredSecondCycleMaxTerm(), errormsg,
                    state);
        } else {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.invalid.loan.principal.not.matching.approval.matrix.in.IC.review.Level.one",
                    String.format("This Loan Account Principal [ %s ] , does not match  IC Review Level One Operations .",
                            loan.getProposedPrincipal()));
        }
    }

    private void validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelOne(BigDecimal loanPrincipal,
            BigDecimal currentStageMatrixMaxAmount, Integer numberOfRepayment, Integer currentStageMatrixMinTerm,
            Integer currentStageMatrixMaxTerm, String errorMsg, String stateMsg) {
        if ((numberOfRepayment < currentStageMatrixMinTerm || numberOfRepayment > currentStageMatrixMaxTerm)) {
            throw new GeneralPlatformDomainRuleException(errorMsg, String.format(
                    "This Loan Account Principal [ %s ] vs Approval Matrix Max Amount [%s] , does not qualify for IC-Review  [%s] with Terms Min [%s] Max [%s] Vs Loan Term [%s]",
                    loanPrincipal, currentStageMatrixMaxAmount, stateMsg, currentStageMatrixMinTerm, currentStageMatrixMaxTerm,
                    numberOfRepayment));
        }
    }

    private void validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelTwoAndAbove(BigDecimal loanPrincipal,
            BigDecimal currentStageMatrixMaxAmount, Integer numberOfRepayment, Integer currentStageMatrixMinTerm,
            Integer currentStageMatrixMaxTerm, String errorMsg, String stateMsg, BigDecimal previousStageMatrixMaxAmount) {
        BigDecimal minAmount = previousStageMatrixMaxAmount.add(BigDecimal.ONE);
        if ((loanPrincipal.compareTo(minAmount) < 0)
                && (numberOfRepayment < currentStageMatrixMinTerm || numberOfRepayment > currentStageMatrixMaxTerm)) {
            throw new GeneralPlatformDomainRuleException(errorMsg, String.format(
                    "This Loan Account Principal [ %s ] vs Approval Matrix Min [%s] and Max Amount [%s] , does not qualify for IC-Review  [%s] with Terms Min [%s] Max [%s] Vs Loan Term [%s]",
                    loanPrincipal, minAmount, currentStageMatrixMaxAmount, stateMsg, currentStageMatrixMinTerm, currentStageMatrixMaxTerm,
                    numberOfRepayment));
        }
    }

    private void validateLoanAccountCompliancePolicyBasedOnApprovalMatrixLevelFive(BigDecimal loanPrincipal,
            BigDecimal currentStageMatrixMaxAmount, Integer numberOfRepayment, Integer currentStageMatrixMinTerm,
            Integer currentStageMatrixMaxTerm, String errorMsg, String stateMsg, BigDecimal previousStageMatrixMaxAmount) {
        BigDecimal minAmount = previousStageMatrixMaxAmount.add(BigDecimal.ONE);
        if ((loanPrincipal.compareTo(minAmount) < 0 || loanPrincipal.compareTo(currentStageMatrixMaxAmount) <= 0)
                && (numberOfRepayment < currentStageMatrixMinTerm || numberOfRepayment > currentStageMatrixMaxTerm)) {
            throw new GeneralPlatformDomainRuleException(errorMsg, String.format(
                    "This Loan Account Principal [ %s ] vs Approval Matrix Min [%s] and Max Amount [%s] , does not qualify for IC-Review  [%s] with Terms Min [%s] Max [%s] Vs Loan Term [%s]",
                    loanPrincipal, minAmount, currentStageMatrixMaxAmount, stateMsg, currentStageMatrixMinTerm, currentStageMatrixMaxTerm,
                    numberOfRepayment));
        }
    }

    public void validateLoanAccountToComplyToApprovalMatrixStage(Loan loan, LoanApprovalMatrix approvalMatrix, Boolean isLoanFirstCycle,
            Boolean isLoanUnsecure, LoanDecisionState currentStage) {
        switch (currentStage) {
            case IC_REVIEW_LEVEL_ONE:
                validateLoanAccountToComplyToApprovalMatrixLevelOne(loan, approvalMatrix, isLoanFirstCycle, isLoanUnsecure);
            break;
            case IC_REVIEW_LEVEL_TWO:
                validateLoanAccountToComplyToApprovalMatrixLevelTwo(loan, approvalMatrix, isLoanFirstCycle, isLoanUnsecure);
            break;
            case IC_REVIEW_LEVEL_THREE:
                validateLoanAccountToComplyToApprovalMatrixLevelThree(loan, approvalMatrix, isLoanFirstCycle, isLoanUnsecure);
            break;
            case IC_REVIEW_LEVEL_FOUR:
                validateLoanAccountToComplyToApprovalMatrixLevelFour(loan, approvalMatrix, isLoanFirstCycle, isLoanUnsecure);
            break;
            case IC_REVIEW_LEVEL_FIVE:
                validateLoanAccountToComplyToApprovalMatrixLevelFive(loan, approvalMatrix, isLoanFirstCycle, isLoanUnsecure);
            break;
            default:
                throw new GeneralPlatformDomainRuleException("error.msg.invalid.loan.decision.stage",
                        String.format("Invalid Loan Stage detected to be validated . Provided Stage [%s]", currentStage));
        }

    }

    public void validateCollateralReviewBusinessRule(JsonCommand command, Loan loan, LoanDecision loanDecision) {
        Boolean isExtendLoanLifeCycleConfig = getExtendLoanLifeCycleConfig().isEnabled();

        if (!isExtendLoanLifeCycleConfig) {
            throw new GeneralPlatformDomainRuleException("error.msg.Add-More-Stages-To-A-Loan-Life-Cycle.is.not.set",
                    "Add-More-Stages-To-A-Loan-Life-Cycle settings is not set. So this operation is not permitted");
        }

        if (loanDecision == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.account.should.not.found.in.decision.engine",
                    "Loan Account not found in decision engine. Operation [Collateral Review] is not allowed");
        }
        checkClientOrGroupActive(loan);

        validateLoanDisbursementDataWithMeetingDate(loan);
        validateLoanTopUp(loan);
        LocalDate collateralReviewOn = command.localDateValueOfParameterNamed(LoanApiConstants.collateralReviewOnDateParameterName);
        // Collateral Review should not be before other stages below it like Due Diligence and Review Application
        if (collateralReviewOn.isBefore(loanDecision.getDueDiligenceOn())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.collateral.review.date.should.be.after.Due.Diligence.date",
                    "Approve Collateral Review date" + collateralReviewOn + " should be after Loan Due Diligence Approved date "
                            + loanDecision.getDueDiligenceOn());
        }
        if (collateralReviewOn.isBefore(loanDecision.getReviewApplicationOn())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.collateral.review.date.should.be.after.review.application.date",
                    "Approve Collateral Review date" + collateralReviewOn + " should be after Loan Review Application Approved date "
                            + loanDecision.getReviewApplicationOn());
        }
        // Collateral Review date should not be before loan submission date
        if (collateralReviewOn.isBefore(loan.getSubmittedOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.collateral.Review.date.should.be.after.submission.date",
                    "Approve Collateral Review date " + collateralReviewOn + " should be after Loan submission date "
                            + loan.getSubmittedOnDate());
        }

        if (!loan.status().isSubmittedAndPendingApproval()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.current.status.is.invalid",
                    "Loan Account current status is invalid. Expected" + loan.status().getCode() + " but found " + loan.status().getCode());
        }
        if (!LoanDecisionState.fromInt(loan.getLoanDecisionState()).isDueDiligence()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.is.invalid",
                    "Loan Account Decision state is invalid. Expected" + LoanDecisionState.DUE_DILIGENCE.getValue() + " but found "
                            + loan.getLoanDecisionState());
        }
        if (!loan.getLoanDecisionState().equals(loanDecision.getLoanDecisionState())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.does.not.reconcile",
                    "Loan Account Decision state Does not reconcile . Operation is terminated");
        }
    }

    public void validateIcReviewDecisionLevelOneBusinessRule(JsonCommand command, Loan loan, LoanDecision loanDecision,
            LocalDate icReviewOn) {
        Boolean isExtendLoanLifeCycleConfig = getExtendLoanLifeCycleConfig().isEnabled();

        if (!isExtendLoanLifeCycleConfig) {
            throw new GeneralPlatformDomainRuleException("error.msg.Add-More-Stages-To-A-Loan-Life-Cycle.is.not.set",
                    "Add-More-Stages-To-A-Loan-Life-Cycle settings is not set. So this operation is not permitted");
        }

        if (loanDecision == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.account.should.not.found.in.decision.engine",
                    "Loan Account not found in decision engine. Operation [IC Review Decision Level One] is not allowed");
        }
        checkClientOrGroupActive(loan);

        validateLoanDisbursementDataWithMeetingDate(loan);
        validateLoanTopUp(loan);
        // Ic Review Decision Level One should not be before other stages below it like Collateral Review , Due
        // Diligence and Review Application
        if (icReviewOn.isBefore(loanDecision.getCollateralReviewOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.one.date.should.be.after.collateral.review.date",
                    "Approve IC ReviewDecision Level One on  date" + icReviewOn + " should be after Collateral Review Approved date "
                            + loanDecision.getCollateralReviewOn());
        }
        if (icReviewOn.isBefore(loanDecision.getDueDiligenceOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.one.date.should.be.after.Due.Diligence.date",
                    "Approve IC ReviewDecision Level One on date" + icReviewOn + " should be after Loan Due Diligence Approved date "
                            + loanDecision.getDueDiligenceOn());
        }
        if (icReviewOn.isBefore(loanDecision.getReviewApplicationOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.one.date.should.be.after.review.application.date",
                    "Approve IC ReviewDecision Level One on date" + icReviewOn + " should be after Loan Review Application Approved date "
                            + loanDecision.getReviewApplicationOn());
        }
        // Collateral Review date should not be before loan submission date
        if (icReviewOn.isBefore(loan.getSubmittedOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.ic.review.decision.level.one.date.should.be.after.submission.date",
                    "Approve IC Review Decision Level One on date " + icReviewOn + " should be after Loan submission date "
                            + loan.getSubmittedOnDate());
        }

        if (!loan.status().isSubmittedAndPendingApproval()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.current.status.is.invalid",
                    "Loan Account current status is invalid. Expected" + loan.status().getCode() + " but found " + loan.status().getCode());
        }
        if (!LoanDecisionState.fromInt(loan.getLoanDecisionState()).isCollateralReview()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.is.invalid",
                    "Loan Account Decision state is invalid. Expected" + LoanDecisionState.COLLATERAL_REVIEW.getValue() + " but found "
                            + loan.getLoanDecisionState());
        }
        if (!loan.getLoanDecisionState().equals(loanDecision.getLoanDecisionState())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.does.not.reconcile",
                    "Loan Account Decision state Does not reconcile . Operation is terminated");
        }
    }

    public void validateIcReviewDecisionLevelTwoBusinessRule(JsonCommand command, Loan loan, LoanDecision loanDecision,
            LocalDate icReviewOn) {
        Boolean isExtendLoanLifeCycleConfig = getExtendLoanLifeCycleConfig().isEnabled();

        if (!isExtendLoanLifeCycleConfig) {
            throw new GeneralPlatformDomainRuleException("error.msg.Add-More-Stages-To-A-Loan-Life-Cycle.is.not.set",
                    "Add-More-Stages-To-A-Loan-Life-Cycle settings is not set. So this operation is not permitted");
        }

        if (loanDecision == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.account.should.not.found.in.decision.engine",
                    "Loan Account not found in decision engine. Operation [IC Review Decision Level Two] is not allowed");
        }
        checkClientOrGroupActive(loan);

        validateLoanDisbursementDataWithMeetingDate(loan);
        validateLoanTopUp(loan);
        // Ic Review Decision Level One should not be before other stages below it like IC Review Decision Level
        // One,Collateral Review , Due
        // Diligence and Review Application
        if (icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelOneOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Two.date.should.be.after.Ic.Review.decision.level.one.date",
                    "Approve IC ReviewDecision Level Two on  date" + icReviewOn + " should be after IC ReviewDecision Level One date "
                            + loanDecision.getIcReviewDecisionLevelOneOn());
        }
        if (icReviewOn.isBefore(loanDecision.getCollateralReviewOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Two.date.should.be.after.collateral.review.date",
                    "Approve IC ReviewDecision Level Two on  date" + icReviewOn + " should be after Collateral Review Approved date "
                            + loanDecision.getCollateralReviewOn());
        }
        if (icReviewOn.isBefore(loanDecision.getDueDiligenceOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Two.date.should.be.after.Due.Diligence.date",
                    "Approve IC ReviewDecision Level Two on date" + icReviewOn + " should be after Loan Due Diligence Approved date "
                            + loanDecision.getDueDiligenceOn());
        }
        if (icReviewOn.isBefore(loanDecision.getReviewApplicationOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Two.date.should.be.after.review.application.date",
                    "Approve IC ReviewDecision Level Two on date" + icReviewOn + " should be after Loan Review Application Approved date "
                            + loanDecision.getReviewApplicationOn());
        }
        // Collateral Review date should not be before loan submission date
        if (icReviewOn.isBefore(loan.getSubmittedOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.ic.review.decision.level.Two.date.should.be.after.submission.date",
                    "Approve IC ReviewDecision Level Two on date " + icReviewOn + " should be after Loan submission date "
                            + loan.getSubmittedOnDate());
        }

        if (!loan.status().isSubmittedAndPendingApproval()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.current.status.is.invalid",
                    "Loan Account current status is invalid. Expected" + loan.status().getCode() + " but found " + loan.status().getCode());
        }
        if (!LoanDecisionState.fromInt(loan.getLoanDecisionState()).isIcReviewLevelOne()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.is.invalid",
                    "Loan Account Decision state is invalid. Expected" + LoanDecisionState.IC_REVIEW_LEVEL_ONE.getValue() + " but found "
                            + loan.getLoanDecisionState());
        }
        if (!loan.getLoanDecisionState().equals(loanDecision.getLoanDecisionState())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.does.not.reconcile",
                    "Loan Account Decision state Does not reconcile . Operation is terminated");
        }
    }

    public void validateIcReviewDecisionLevelThreeBusinessRule(JsonCommand command, Loan loan, LoanDecision loanDecision,
            LocalDate icReviewOn) {
        Boolean isExtendLoanLifeCycleConfig = getExtendLoanLifeCycleConfig().isEnabled();

        if (!isExtendLoanLifeCycleConfig) {
            throw new GeneralPlatformDomainRuleException("error.msg.Add-More-Stages-To-A-Loan-Life-Cycle.is.not.set",
                    "Add-More-Stages-To-A-Loan-Life-Cycle settings is not set. So this operation is not permitted");
        }

        if (loanDecision == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.account.should.not.found.in.decision.engine",
                    "Loan Account not found in decision engine. Operation [IC Review Decision Level Three] is not allowed");
        }
        checkClientOrGroupActive(loan);

        validateLoanDisbursementDataWithMeetingDate(loan);
        validateLoanTopUp(loan);
        // Ic Review Decision Level One should not be before other stages below it like IC Review Decision Level
        // One,Collateral Review , Due
        // Diligence and Review Application
        if (icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelTwoOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Three.date.should.be.after.Ic.Review.decision.level.Two.date",
                    "Approve IC ReviewDecision Level Three on  date" + icReviewOn + " should be after IC ReviewDecision Level two date "
                            + loanDecision.getIcReviewDecisionLevelTwoOn());
        }
        if (icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelOneOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Three.date.should.be.after.Ic.Review.decision.level.one.date",
                    "Approve IC ReviewDecision Level Three on  date" + icReviewOn + " should be after IC ReviewDecision Level One date "
                            + loanDecision.getIcReviewDecisionLevelOneOn());
        }
        if (icReviewOn.isBefore(loanDecision.getCollateralReviewOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Three.date.should.be.after.collateral.review.date",
                    "Approve IC ReviewDecision Level Three on  date" + icReviewOn + " should be after Collateral Review Approved date "
                            + loanDecision.getCollateralReviewOn());
        }
        if (icReviewOn.isBefore(loanDecision.getDueDiligenceOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Three.date.should.be.after.Due.Diligence.date",
                    "Approve IC ReviewDecision Level Three on date" + icReviewOn + " should be after Loan Due Diligence Approved date "
                            + loanDecision.getDueDiligenceOn());
        }
        if (icReviewOn.isBefore(loanDecision.getReviewApplicationOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Three.date.should.be.after.review.application.date",
                    "Approve IC ReviewDecision Level Three on date" + icReviewOn + " should be after Loan Review Application Approved date "
                            + loanDecision.getReviewApplicationOn());
        }
        // Collateral Review date should not be before loan submission date
        if (icReviewOn.isBefore(loan.getSubmittedOnDate())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Three.date.should.be.after.submission.date",
                    "Approve IC ReviewDecision Level Three on date " + icReviewOn + " should be after Loan submission date "
                            + loan.getSubmittedOnDate());
        }

        if (!loan.status().isSubmittedAndPendingApproval()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.current.status.is.invalid",
                    "Loan Account current status is invalid. Expected" + loan.status().getCode() + " but found " + loan.status().getCode());
        }
        if (!LoanDecisionState.fromInt(loan.getLoanDecisionState()).isIcReviewLevelTwo()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.is.invalid",
                    "Loan Account Decision state is invalid. Expected " + LoanDecisionState.IC_REVIEW_LEVEL_TWO.getValue() + " but found "
                            + loan.getLoanDecisionState());
        }
        if (!loan.getLoanDecisionState().equals(loanDecision.getLoanDecisionState())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.does.not.reconcile",
                    "Loan Account Decision state Does not reconcile . Operation is terminated");
        }
    }

    public void validateIcReviewDecisionLevelFourBusinessRule(JsonCommand command, Loan loan, LoanDecision loanDecision,
            LocalDate icReviewOn) {
        Boolean isExtendLoanLifeCycleConfig = getExtendLoanLifeCycleConfig().isEnabled();

        if (!isExtendLoanLifeCycleConfig) {
            throw new GeneralPlatformDomainRuleException("error.msg.Add-More-Stages-To-A-Loan-Life-Cycle.is.not.set",
                    "Add-More-Stages-To-A-Loan-Life-Cycle settings is not set. So this operation is not permitted");
        }

        if (loanDecision == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.account.not.found.in.decision.engine",
                    "Loan Account not found in decision engine. Operation [IC Review Decision Level Four] is not allowed");
        }
        checkClientOrGroupActive(loan);

        validateLoanDisbursementDataWithMeetingDate(loan);
        validateLoanTopUp(loan);
        // Ic Review Decision Level One should not be before other stages below it like IC Review Decision Level
        // One,Collateral Review , Due
        // Diligence and Review Application
        if (icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelThreeOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Four.date.should.be.after.Ic.Review.decision.level.Three.date",
                    "Approve IC ReviewDecision Level Four on  date" + icReviewOn + " should be after IC ReviewDecision Level Three date "
                            + loanDecision.getIcReviewDecisionLevelThreeOn());
        }

        if (icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelTwoOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Four.date.should.be.after.Ic.Review.decision.level.Two.date",
                    "Approve IC ReviewDecision Level Four on  date" + icReviewOn + " should be after IC ReviewDecision Level two date "
                            + loanDecision.getIcReviewDecisionLevelTwoOn());
        }
        if (icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelOneOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Four.date.should.be.after.Ic.Review.decision.level.one.date",
                    "Approve IC ReviewDecision Level Four on  date" + icReviewOn + " should be after IC ReviewDecision Level One date "
                            + loanDecision.getIcReviewDecisionLevelOneOn());
        }
        if (icReviewOn.isBefore(loanDecision.getCollateralReviewOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Four.date.should.be.after.collateral.review.date",
                    "Approve IC ReviewDecision Level Four on  date" + icReviewOn + " should be after Collateral Review Approved date "
                            + loanDecision.getCollateralReviewOn());
        }
        if (icReviewOn.isBefore(loanDecision.getDueDiligenceOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Four.date.should.be.after.Due.Diligence.date",
                    "Approve IC ReviewDecision Level Four on date" + icReviewOn + " should be after Loan Due Diligence Approved date "
                            + loanDecision.getDueDiligenceOn());
        }
        if (icReviewOn.isBefore(loanDecision.getReviewApplicationOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Four.date.should.be.after.review.application.date",
                    "Approve IC ReviewDecision Level Four on date" + icReviewOn + " should be after Loan Review Application Approved date "
                            + loanDecision.getReviewApplicationOn());
        }
        // Collateral Review date should not be before loan submission date
        if (icReviewOn.isBefore(loan.getSubmittedOnDate())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Four.date.should.be.after.submission.date",
                    "Approve IC ReviewDecision Level Four on date " + icReviewOn + " should be after Loan submission date "
                            + loan.getSubmittedOnDate());
        }

        if (!loan.status().isSubmittedAndPendingApproval()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.current.status.is.invalid",
                    "Loan Account current status is invalid. Expected" + loan.status().getCode() + " but found " + loan.status().getCode());
        }
        if (!LoanDecisionState.fromInt(loan.getLoanDecisionState()).isIcReviewLevelThree()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.is.invalid",
                    "Loan Account Decision state is invalid. Expected " + LoanDecisionState.IC_REVIEW_LEVEL_THREE.getValue() + " but found "
                            + loan.getLoanDecisionState());
        }
        if (!loan.getLoanDecisionState().equals(loanDecision.getLoanDecisionState())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.does.not.reconcile",
                    "Loan Account Decision state Does not reconcile . Operation is terminated");
        }
    }

    public void validateIcReviewDecisionLevelFiveBusinessRule(JsonCommand command, Loan loan, LoanDecision loanDecision,
            LocalDate icReviewOn) {
        Boolean isExtendLoanLifeCycleConfig = getExtendLoanLifeCycleConfig().isEnabled();

        if (!isExtendLoanLifeCycleConfig) {
            throw new GeneralPlatformDomainRuleException("error.msg.Add-More-Stages-To-A-Loan-Life-Cycle.is.not.set",
                    "Add-More-Stages-To-A-Loan-Life-Cycle settings is not set. So this operation is not permitted");
        }

        if (loanDecision == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.account.not.found.in.decision.engine",
                    "Loan Account not found in decision engine. Operation [IC Review Decision Level Five] is not allowed");
        }
        checkClientOrGroupActive(loan);

        validateLoanDisbursementDataWithMeetingDate(loan);
        validateLoanTopUp(loan);
        // Ic Review Decision Level One should not be before other stages below it like IC Review Decision Level
        // One,Collateral Review , Due
        // Diligence and Review Application
        if (icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelFourOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Five.date.should.be.after.Ic.Review.decision.level.Four.date",
                    "Approve IC ReviewDecision Level Five on  date" + icReviewOn + " should be after IC ReviewDecision Level Four date "
                            + loanDecision.getIcReviewDecisionLevelFourOn());
        }
        if (icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelThreeOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Five.date.should.be.after.Ic.Review.decision.level.Three.date",
                    "Approve IC ReviewDecision Level Five on  date" + icReviewOn + " should be after IC ReviewDecision Level Three date "
                            + loanDecision.getIcReviewDecisionLevelThreeOn());
        }

        if (icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelTwoOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Five.date.should.be.after.Ic.Review.decision.level.Two.date",
                    "Approve IC ReviewDecision Level Five on  date" + icReviewOn + " should be after IC ReviewDecision Level two date "
                            + loanDecision.getIcReviewDecisionLevelTwoOn());
        }
        if (icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelOneOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Five.date.should.be.after.Ic.Review.decision.level.one.date",
                    "Approve IC ReviewDecision Level Five on  date" + icReviewOn + " should be after IC ReviewDecision Level One date "
                            + loanDecision.getIcReviewDecisionLevelOneOn());
        }
        if (icReviewOn.isBefore(loanDecision.getCollateralReviewOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Five.date.should.be.after.collateral.review.date",
                    "Approve IC ReviewDecision Level Five on  date" + icReviewOn + " should be after Collateral Review Approved date "
                            + loanDecision.getCollateralReviewOn());
        }
        if (icReviewOn.isBefore(loanDecision.getDueDiligenceOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Five.date.should.be.after.Due.Diligence.date",
                    "Approve IC ReviewDecision Level Five on date" + icReviewOn + " should be after Loan Due Diligence Approved date "
                            + loanDecision.getDueDiligenceOn());
        }
        if (icReviewOn.isBefore(loanDecision.getReviewApplicationOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Five.date.should.be.after.review.application.date",
                    "Approve IC ReviewDecision Level Five on date" + icReviewOn + " should be after Loan Review Application Approved date "
                            + loanDecision.getReviewApplicationOn());
        }
        // Collateral Review date should not be before loan submission date
        if (icReviewOn.isBefore(loan.getSubmittedOnDate())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.ic.review.decision.level.Five.date.should.be.after.submission.date",
                    "Approve IC ReviewDecision Level Five on date " + icReviewOn + " should be after Loan submission date "
                            + loan.getSubmittedOnDate());
        }

        if (!loan.status().isSubmittedAndPendingApproval()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.current.status.is.invalid",
                    "Loan Account current status is invalid. Expected" + loan.status().getCode() + " but found " + loan.status().getCode());
        }
        if (!LoanDecisionState.fromInt(loan.getLoanDecisionState()).isIcReviewLevelFour()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.is.invalid",
                    "Loan Account Decision state is invalid. Expected " + LoanDecisionState.IC_REVIEW_LEVEL_FOUR.getValue() + " but found "
                            + loan.getLoanDecisionState());
        }
        if (!loan.getLoanDecisionState().equals(loanDecision.getLoanDecisionState())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.does.not.reconcile",
                    "Loan Account Decision state Does not reconcile . Operation is terminated");
        }
    }

    public void validatePrepareAndSignContractBusinessRule(JsonCommand command, Loan loan, LoanDecision loanDecision) {
        Boolean isExtendLoanLifeCycleConfig = getExtendLoanLifeCycleConfig().isEnabled();

        if (!isExtendLoanLifeCycleConfig) {
            throw new GeneralPlatformDomainRuleException("error.msg.Add-More-Stages-To-A-Loan-Life-Cycle.is.not.set",
                    "Add-More-Stages-To-A-Loan-Life-Cycle settings is not set. So this operation is not permitted");
        }

        if (loanDecision == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.account.not.found.in.decision.engine",
                    "Loan Account not found in decision engine. Operation [Prepare And Sign Contract] is not allowed");
        }
        checkClientOrGroupActive(loan);

        validateLoanDisbursementDataWithMeetingDate(loan);
        validateLoanTopUp(loan);
        LocalDate icReviewOn = command.localDateValueOfParameterNamed(LoanApiConstants.icReviewOnDateParameterName);

        if (loanDecision.getIcReviewDecisionLevelFiveOn() != null && icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelFiveOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.prepare.and.sign.contract.date.should.be.after.Ic.Review.decision.level.Five.date",
                    "Approve Prepare And Sign Contract on  date" + icReviewOn + " should be after IC ReviewDecision Level Five date "
                            + loanDecision.getIcReviewDecisionLevelFiveOn());
        }

        if (loanDecision.getIcReviewDecisionLevelFourOn() != null && icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelFourOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.prepare.and.sign.contract.date.should.be.after.Ic.Review.decision.level.Four.date",
                    "Approve Prepare And Sign Contract on  date" + icReviewOn + " should be after IC ReviewDecision Level Four date "
                            + loanDecision.getIcReviewDecisionLevelFourOn());
        }
        if (loanDecision.getIcReviewDecisionLevelThreeOn() != null && icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelThreeOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.prepare.and.sign.contract.date.should.be.after.Ic.Review.decision.level.Three.date",
                    "Approve Prepare And Sign Contract on  date" + icReviewOn + " should be after IC ReviewDecision Level Three date "
                            + loanDecision.getIcReviewDecisionLevelThreeOn());
        }

        if (loanDecision.getIcReviewDecisionLevelTwoOn() != null && icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelTwoOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.prepare.and.sign.contract.date.should.be.after.Ic.Review.decision.level.Two.date",
                    "Approve Prepare And Sign Contract on  date" + icReviewOn + " should be after IC ReviewDecision Level two date "
                            + loanDecision.getIcReviewDecisionLevelTwoOn());
        }
        if (icReviewOn.isBefore(loanDecision.getIcReviewDecisionLevelOneOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.prepare.and.sign.contract.date.should.be.after.Ic.Review.decision.level.one.date",
                    "Approve Prepare And Sign Contract on  date" + icReviewOn + " should be after IC ReviewDecision Level One date "
                            + loanDecision.getIcReviewDecisionLevelOneOn());
        }
        if (icReviewOn.isBefore(loanDecision.getCollateralReviewOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.prepare.and.sign.contract.date.should.be.after.collateral.review.date",
                    "Approve Prepare And Sign Contract on  date" + icReviewOn + " should be after Collateral Review Approved date "
                            + loanDecision.getCollateralReviewOn());
        }
        if (icReviewOn.isBefore(loanDecision.getDueDiligenceOn())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.prepare.and.sign.contract.date.should.be.after.Due.Diligence.date",
                    "Approve Prepare And Sign Contract on date" + icReviewOn + " should be after Loan Due Diligence Approved date "
                            + loanDecision.getDueDiligenceOn());
        }
        if (icReviewOn.isBefore(loanDecision.getReviewApplicationOn())) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.prepare.and.sign.contract.date.should.be.after.review.application.date",
                    "Approve Prepare And Sign Contract on date" + icReviewOn + " should be after Loan Review Application Approved date "
                            + loanDecision.getReviewApplicationOn());
        }
        // Collateral Review date should not be before loan submission date
        if (icReviewOn.isBefore(loan.getSubmittedOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.prepare.and.sign.contract.date.should.be.after.submission.date",
                    "Approve Prepare And Sign Contract on date " + icReviewOn + " should be after Loan submission date "
                            + loan.getSubmittedOnDate());
        }

        if (!loan.status().isSubmittedAndPendingApproval()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.current.status.is.invalid",
                    "Loan Account current status is invalid. Expected" + loan.status().getCode() + " but found " + loan.status().getCode());
        }

        if (!loan.getLoanDecisionState().equals(loanDecision.getLoanDecisionState())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.does.not.reconcile",
                    "Loan Account Decision state Does not reconcile . Operation is terminated");
        }
    }

    private void generateTheNextIcReviewStageFive(BigDecimal loanPrincipal, BigDecimal nextStageMatrixMaxAmount, Integer numberOfRepayment,
            Integer nextStageMatrixMinTerm, Integer nextStageMatrixMaxTerm, LoanDecision loanDecision,
            LoanDecisionState nextStageIcReview) {

        if ((loanPrincipal.compareTo(nextStageMatrixMaxAmount) > 0)
                && (numberOfRepayment > nextStageMatrixMinTerm && numberOfRepayment <= nextStageMatrixMaxTerm)) {
            loanDecision.setNextLoanIcReviewDecisionState(nextStageIcReview.getValue());
        } else {
            loanDecision.setNextLoanIcReviewDecisionState(LoanDecisionState.PREPARE_AND_SIGN_CONTRACT.getValue());
        }
    }

    private void generateTheNextIcReviewStage(BigDecimal loanPrincipal, BigDecimal nextStageMatrixMaxAmount, Integer numberOfRepayment,
            Integer nextStageMatrixMinTerm, Integer nextStageMatrixMaxTerm, LoanDecision loanDecision, LoanDecisionState nextStageIcReview,
            BigDecimal currentStageMaximumLoanAmount) {

        if ((loanPrincipal.compareTo(currentStageMaximumLoanAmount.add(BigDecimal.ONE)) >= 0
                && (loanPrincipal.compareTo(nextStageMatrixMaxAmount) <= 0 || loanPrincipal.compareTo(nextStageMatrixMaxAmount) > 0))
                && (numberOfRepayment > nextStageMatrixMinTerm && numberOfRepayment <= nextStageMatrixMaxTerm)) {
            loanDecision.setNextLoanIcReviewDecisionState(nextStageIcReview.getValue());
        } else {
            loanDecision.setNextLoanIcReviewDecisionState(LoanDecisionState.PREPARE_AND_SIGN_CONTRACT.getValue());
        }
    }

    public void determineTheNextDecisionStage(Loan loan, LoanDecision loanDecision, LoanApprovalMatrix approvalMatrix,
            Boolean isLoanFirstCycle, Boolean isLoanUnsecure, LoanDecisionState currentStage) {
        switch (currentStage) {
            case IC_REVIEW_LEVEL_ONE:
                determineTheNextDecisionStateAfterLevelOne(loan, loanDecision, approvalMatrix, isLoanFirstCycle, isLoanUnsecure);
            break;
            case IC_REVIEW_LEVEL_TWO:
                determineTheNextDecisionStateAfterLevelTwo(loan, loanDecision, approvalMatrix, isLoanFirstCycle, isLoanUnsecure);
            break;
            case IC_REVIEW_LEVEL_THREE:
                determineTheNextDecisionStateAfterLevelThree(loan, loanDecision, approvalMatrix, isLoanFirstCycle, isLoanUnsecure);
            break;
            case IC_REVIEW_LEVEL_FOUR:
                determineTheNextDecisionStateAfterLevelFour(loan, loanDecision, approvalMatrix, isLoanFirstCycle, isLoanUnsecure);
            break;
            default:
                throw new GeneralPlatformDomainRuleException("error.msg.invalid.loan.decision.stage",
                        String.format("Invalid Loan Stage detected [%s]", currentStage));
        }

    }

    private void determineTheNextDecisionStateAfterLevelOne(Loan loan, LoanDecision loanDecision, LoanApprovalMatrix approvalMatrix,
            Boolean isLoanFirstCycle, Boolean isLoanUnsecure) {
        LoanDecisionState expectedNextIcReviewStage = LoanDecisionState.IC_REVIEW_LEVEL_TWO;
        if (isLoanFirstCycle && isLoanUnsecure) {
            // Loan is FirstCycle and Unsecure
            generateTheNextIcReviewStage(loan.getProposedPrincipal(), approvalMatrix.getLevelTwoUnsecuredFirstCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelTwoUnsecuredFirstCycleMinTerm(),
                    approvalMatrix.getLevelTwoUnsecuredFirstCycleMaxTerm(), loanDecision, expectedNextIcReviewStage,
                    approvalMatrix.getLevelOneUnsecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && isLoanUnsecure) {
            // Loan is (Second cycle or plus) and Unsecure
            generateTheNextIcReviewStage(loan.getProposedPrincipal(), approvalMatrix.getLevelTwoUnsecuredSecondCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelTwoUnsecuredSecondCycleMinTerm(),
                    approvalMatrix.getLevelTwoUnsecuredSecondCycleMaxTerm(), loanDecision, expectedNextIcReviewStage,
                    approvalMatrix.getLevelOneUnsecuredSecondCycleMaxAmount());

        } else if (isLoanFirstCycle && !isLoanUnsecure) {
            // First Cycle and secured Loan
            generateTheNextIcReviewStage(loan.getProposedPrincipal(), approvalMatrix.getLevelTwoSecuredFirstCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelTwoSecuredFirstCycleMinTerm(),
                    approvalMatrix.getLevelTwoSecuredFirstCycleMaxTerm(), loanDecision, expectedNextIcReviewStage,
                    approvalMatrix.getLevelOneSecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && !isLoanUnsecure) {
            // Second Cycle or plus and secured
            generateTheNextIcReviewStage(loan.getProposedPrincipal(), approvalMatrix.getLevelTwoSecuredSecondCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelTwoSecuredSecondCycleMinTerm(),
                    approvalMatrix.getLevelTwoSecuredSecondCycleMaxTerm(), loanDecision, expectedNextIcReviewStage,
                    approvalMatrix.getLevelOneSecuredSecondCycleMaxAmount());

        } else {
            throw new GeneralPlatformDomainRuleException("error.msg.invalid.loan.decision.engine.can.not.determine.the.next.decision.state",
                    "The Loan Decision Engine can not determine the next Decision State .");
        }
    }

    private void determineTheNextDecisionStateAfterLevelTwo(Loan loan, LoanDecision loanDecision, LoanApprovalMatrix approvalMatrix,
            Boolean isLoanFirstCycle, Boolean isLoanUnsecure) {
        LoanDecisionState expectedNextIcReviewStage = LoanDecisionState.IC_REVIEW_LEVEL_THREE;
        if (isLoanFirstCycle && isLoanUnsecure) {
            // Loan is FirstCycle and Unsecure
            generateTheNextIcReviewStage(loan.getProposedPrincipal(), approvalMatrix.getLevelThreeUnsecuredFirstCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelThreeUnsecuredFirstCycleMinTerm(),
                    approvalMatrix.getLevelThreeUnsecuredFirstCycleMaxTerm(), loanDecision, expectedNextIcReviewStage,
                    approvalMatrix.getLevelTwoUnsecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && isLoanUnsecure) {
            // Loan is (Second cycle or plus) and Unsecure
            generateTheNextIcReviewStage(loan.getProposedPrincipal(), approvalMatrix.getLevelThreeUnsecuredSecondCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelThreeUnsecuredSecondCycleMinTerm(),
                    approvalMatrix.getLevelThreeUnsecuredSecondCycleMaxTerm(), loanDecision, expectedNextIcReviewStage,
                    approvalMatrix.getLevelTwoUnsecuredSecondCycleMaxAmount());

        } else if (isLoanFirstCycle && !isLoanUnsecure) {
            // First Cycle and secured Loan
            generateTheNextIcReviewStage(loan.getProposedPrincipal(), approvalMatrix.getLevelThreeSecuredFirstCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelThreeSecuredFirstCycleMinTerm(),
                    approvalMatrix.getLevelThreeSecuredFirstCycleMaxTerm(), loanDecision, expectedNextIcReviewStage,
                    approvalMatrix.getLevelTwoSecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && !isLoanUnsecure) {
            // Second Cycle or plus and secured
            generateTheNextIcReviewStage(loan.getProposedPrincipal(), approvalMatrix.getLevelThreeSecuredSecondCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelThreeSecuredSecondCycleMinTerm(),
                    approvalMatrix.getLevelThreeSecuredSecondCycleMaxTerm(), loanDecision, expectedNextIcReviewStage,
                    approvalMatrix.getLevelTwoSecuredSecondCycleMaxAmount());

        } else {
            throw new GeneralPlatformDomainRuleException("error.msg.invalid.loan.decision.engine.can.not.determine.the.next.decision.state",
                    "The Loan Decision Engine can not determine the next Decision State .");
        }
    }

    private void determineTheNextDecisionStateAfterLevelThree(Loan loan, LoanDecision loanDecision, LoanApprovalMatrix approvalMatrix,
            Boolean isLoanFirstCycle, Boolean isLoanUnsecure) {
        LoanDecisionState expectedNextIcReviewStage = LoanDecisionState.IC_REVIEW_LEVEL_FOUR;
        if (isLoanFirstCycle && isLoanUnsecure) {
            // Loan is FirstCycle and Unsecure
            generateTheNextIcReviewStage(loan.getProposedPrincipal(), approvalMatrix.getLevelFourUnsecuredFirstCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelFourUnsecuredFirstCycleMinTerm(),
                    approvalMatrix.getLevelFourUnsecuredFirstCycleMaxTerm(), loanDecision, expectedNextIcReviewStage,
                    approvalMatrix.getLevelThreeUnsecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && isLoanUnsecure) {
            // Loan is (Second cycle or plus) and Unsecure
            generateTheNextIcReviewStage(loan.getProposedPrincipal(), approvalMatrix.getLevelFourUnsecuredSecondCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelFourUnsecuredSecondCycleMinTerm(),
                    approvalMatrix.getLevelFourUnsecuredSecondCycleMaxTerm(), loanDecision, expectedNextIcReviewStage,
                    approvalMatrix.getLevelThreeUnsecuredSecondCycleMaxAmount());

        } else if (isLoanFirstCycle && !isLoanUnsecure) {
            // First Cycle and secured Loan
            generateTheNextIcReviewStage(loan.getProposedPrincipal(), approvalMatrix.getLevelFourSecuredFirstCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelFourSecuredFirstCycleMinTerm(),
                    approvalMatrix.getLevelFourSecuredFirstCycleMaxTerm(), loanDecision, expectedNextIcReviewStage,
                    approvalMatrix.getLevelThreeSecuredFirstCycleMaxAmount());

        } else if (!isLoanFirstCycle && !isLoanUnsecure) {
            // Second Cycle or plus and secured
            generateTheNextIcReviewStage(loan.getProposedPrincipal(), approvalMatrix.getLevelFourSecuredSecondCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelFourSecuredSecondCycleMinTerm(),
                    approvalMatrix.getLevelFourSecuredSecondCycleMaxTerm(), loanDecision, expectedNextIcReviewStage,
                    approvalMatrix.getLevelThreeSecuredSecondCycleMaxAmount());

        } else {
            throw new GeneralPlatformDomainRuleException("error.msg.invalid.loan.decision.engine.can.not.determine.the.next.decision.state",
                    "The Loan Decision Engine can not determine the next Decision State .");
        }
    }

    private void determineTheNextDecisionStateAfterLevelFour(Loan loan, LoanDecision loanDecision, LoanApprovalMatrix approvalMatrix,
            Boolean isLoanFirstCycle, Boolean isLoanUnsecure) {
        LoanDecisionState expectedNextIcReviewStage = LoanDecisionState.IC_REVIEW_LEVEL_FIVE;
        if (isLoanFirstCycle && isLoanUnsecure) {
            // Loan is FirstCycle and Unsecure
            generateTheNextIcReviewStageFive(loan.getProposedPrincipal(), approvalMatrix.getLevelFiveUnsecuredFirstCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelFiveUnsecuredFirstCycleMinTerm(),
                    approvalMatrix.getLevelFiveUnsecuredFirstCycleMaxTerm(), loanDecision, expectedNextIcReviewStage);

        } else if (!isLoanFirstCycle && isLoanUnsecure) {
            // Loan is (Second cycle or plus) and Unsecure
            generateTheNextIcReviewStageFive(loan.getProposedPrincipal(), approvalMatrix.getLevelFiveUnsecuredSecondCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelFiveUnsecuredSecondCycleMinTerm(),
                    approvalMatrix.getLevelFiveUnsecuredSecondCycleMaxTerm(), loanDecision, expectedNextIcReviewStage);

        } else if (isLoanFirstCycle && !isLoanUnsecure) {
            // First Cycle and secured Loan
            generateTheNextIcReviewStageFive(loan.getProposedPrincipal(), approvalMatrix.getLevelFiveSecuredFirstCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelFiveSecuredFirstCycleMinTerm(),
                    approvalMatrix.getLevelFiveSecuredFirstCycleMaxTerm(), loanDecision, expectedNextIcReviewStage);

        } else if (!isLoanFirstCycle && !isLoanUnsecure) {
            // Second Cycle or plus and secured
            generateTheNextIcReviewStageFive(loan.getProposedPrincipal(), approvalMatrix.getLevelFiveSecuredSecondCycleMaxAmount(),
                    loan.getNumberOfRepayments(), approvalMatrix.getLevelFiveSecuredSecondCycleMinTerm(),
                    approvalMatrix.getLevelFiveSecuredSecondCycleMaxTerm(), loanDecision, expectedNextIcReviewStage);

        } else {
            throw new GeneralPlatformDomainRuleException("error.msg.invalid.loan.decision.engine.can.not.determine.the.next.decision.state",
                    "The Loan Decision Engine can not determine the next Decision State .");
        }
    }

    public GlobalConfigurationPropertyData getExtendLoanLifeCycleConfig() {
        final GlobalConfigurationPropertyData extendLoanLifeCycleConfig = this.configurationReadPlatformService
                .retrieveGlobalConfiguration(LoanApprovalMatrixConstants.ADD_MORE_STAGES_TO_A_LOAN_LIFE_CYCLE);
        return extendLoanLifeCycleConfig;
    }

    public void checkClientOrGroupActive(final Loan loan) {
        final Client client = loan.client();
        if (client != null) {
            if (client.isNotActive()) {
                throw new ClientNotActiveException(client.getId());
            }
        }
        final Group group = loan.group();
        if (group != null) {
            if (group.isNotActive()) {
                throw new GroupNotActiveException(group.getId());
            }
        }
    }

    public void validateLoanDisbursementDataWithMeetingDate(Loan loan) {
        Boolean isSkipRepaymentOnFirstMonth = false;
        Integer numberOfDays = 0;
        // validate expected disbursement date against meeting date
        if (loan.isSyncDisbursementWithMeeting() && (loan.isGroupLoan() || loan.isJLGLoan())) {
            final CalendarInstance calendarInstance = this.calendarInstanceRepository.findCalendarInstaneByEntityId(loan.getId(),
                    CalendarEntityType.LOANS.getValue());
            Calendar calendar = null;
            if (calendarInstance != null) {
                calendar = calendarInstance.getCalendar();
            }

            boolean isSkipRepaymentOnFirstMonthEnabled = this.configurationDomainService.isSkippingMeetingOnFirstDayOfMonthEnabled();
            if (isSkipRepaymentOnFirstMonthEnabled) {
                isSkipRepaymentOnFirstMonth = this.loanUtilService.isLoanRepaymentsSyncWithMeeting(loan.group(), calendar);
                if (isSkipRepaymentOnFirstMonth) {
                    numberOfDays = configurationDomainService.retreivePeroidInNumberOfDaysForSkipMeetingDate().intValue();
                }
            }
            this.loanScheduleAssembler.validateDisbursementDateWithMeetingDates(loan.getDisbursementDate(), calendar,
                    isSkipRepaymentOnFirstMonth, numberOfDays);
        }
    }

    public void validateLoanTopUp(Loan loan) {
        if (loan.isTopup() && loan.getClientId() != null) {
            final Long loanIdToClose = loan.getTopupLoanDetails().getLoanIdToClose();
            final Loan loanToClose = this.loanRepositoryWrapper.findNonClosedLoanThatBelongsToClient(loanIdToClose, loan.getClientId());
            if (loanToClose == null) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.to.be.closed.with.top-up.is.not.active",
                        "Loan to be closed with this top-up is not active.");
            }

            final LocalDate lastUserTransactionOnLoanToClose = loanToClose.getLastUserTransactionDate();
            if (loan.getDisbursementDate().isBefore(lastUserTransactionOnLoanToClose)) {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.loan.disbursal.date.should.be.after.last.transaction.date.of.loan.to.be.closed",
                        "Disbursal date of this loan application " + loan.getDisbursementDate()
                                + " should be after last transaction date of loan to be closed " + lastUserTransactionOnLoanToClose);
            }
            BigDecimal loanOutstanding = this.loanReadPlatformService
                    .retrieveLoanForeclosureTemplate(loanIdToClose, loan.getDisbursementDate()).getAmount();
            final BigDecimal firstDisbursalAmount = loan.getFirstDisbursalAmount();
            if (loanOutstanding.compareTo(firstDisbursalAmount) > 0) {
                throw new GeneralPlatformDomainRuleException("error.msg.loan.amount.less.than.outstanding.of.loan.to.be.closed",
                        "Topup loan amount should be greater than outstanding amount of loan to be closed.");
            }
            BigDecimal netDisbursalAmount = loan.getApprovedPrincipal().subtract(loanOutstanding);
            loan.adjustNetDisbursalAmount(netDisbursalAmount);
        }
    }

    public Boolean isLoanFirstCycle(List<Loan> loanIndividualCounter) {
        return CollectionUtils.isEmpty(loanIndividualCounter);
    }

    public Boolean isLoanUnSecure(Loan loan) {
        List<LoanCollateralManagement> collateralManagementList = loanCollateralManagementRepository.findByLoan(loan);
        return CollectionUtils.isEmpty(collateralManagementList);
    }

    public List<Loan> getLoanCounter(Loan loan) {
        List<Loan> loanIndividualCounter;
        if (loan.isIndividualLoan() || loan.isJLGLoan() || loan.isGLIMLoan()) {
            // Validate Individual Loan Cycle . . .
            loanIndividualCounter = this.loanRepositoryWrapper.findLoanCounterByClientId(loan.getClientId());
        } else if (loan.isGroupLoan()) {
            loanIndividualCounter = this.loanRepositoryWrapper.findLoanCounterByGroupId(loan.getGroupId());
        } else {
            // Throw Not Support Loan Type
            throw new GeneralPlatformDomainRuleException("error.msg.invalid.loan.type.not.supported.for.Ic.Review",
                    String.format("This Loan Type [ %s ] , is not supported for IC Review Operations .", loan.getLoanType()));
        }
        return loanIndividualCounter;
    }
}
