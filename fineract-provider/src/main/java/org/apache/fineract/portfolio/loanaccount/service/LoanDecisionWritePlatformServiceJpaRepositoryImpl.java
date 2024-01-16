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
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.documentmanagement.data.DocumentData;
import org.apache.fineract.infrastructure.documentmanagement.service.DocumentReadPlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.api.LoanApprovalMatrixConstants;
import org.apache.fineract.portfolio.loanaccount.data.LoanCashFlowData;
import org.apache.fineract.portfolio.loanaccount.data.LoanFinancialRatioData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanApprovalMatrix;
import org.apache.fineract.portfolio.loanaccount.domain.LoanApprovalMatrixRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCollateralManagementRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecision;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecisionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecisionState;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDueDiligenceInfo;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDueDiligenceInfoRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolCrbIdentityReport;
import org.apache.fineract.portfolio.loanaccount.domain.MetropolCrbIdentityVerificationRepository;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbHeader;
import org.apache.fineract.portfolio.loanaccount.domain.TransunionCrbHeaderRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanDueDiligenceException;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanDecisionTransitionApiJsonValidator;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanDecisionWritePlatformServiceJpaRepositoryImpl implements LoanApplicationDecisionWritePlatformService {

    private final PlatformSecurityContext context;
    private final LoanDecisionTransitionApiJsonValidator loanDecisionTransitionApiJsonValidator;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanDecisionRepository loanDecisionRepository;
    private final ConfigurationReadPlatformService configurationReadPlatformService;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanDecisionAssembler loanDecisionAssembler;
    private final LoanDueDiligenceInfoRepository loanDueDiligenceInfoRepository;
    private final NoteRepository noteRepository;
    private final LoanApprovalMatrixRepository loanApprovalMatrixRepository;
    private final LoanCollateralManagementRepository loanCollateralManagementRepository;
    private final LoanDecisionStateUtilService loanDecisionStateUtilService;
    private final DocumentReadPlatformService documentReadPlatformService;
    private final MetropolCrbIdentityVerificationRepository metropolCrbIdentityVerificationRepository;
    private final TransunionCrbHeaderRepository transunionCrbHeaderRepository;

    @Override
    public CommandProcessingResult acceptLoanApplicationReview(final Long loanId, final JsonCommand command) {

        final AppUser currentUser = getAppUserIfPresent();

        this.loanDecisionTransitionApiJsonValidator.validateApplicationReview(command.json());

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());

        validateReviewApplicationBusinessRule(command, loan, loanDecision);
        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleFrom(command, loan, currentUser);
        LoanDecision savedObj = loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.REVIEW_APPLICATION.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getReviewApplicationNote())) {
            final Note note = Note.loanNote(loanObj, "Review Application: " + loanDecisionObj.getReviewApplicationNote());
            this.noteRepository.save(note);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(savedObj.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .withResourceIdAsString(savedObj.getId().toString()).build();
    }

    private void validateReviewApplicationBusinessRule(JsonCommand command, Loan loan, LoanDecision loanDecision) {
        Boolean isExtendLoanLifeCycleConfig = loanDecisionStateUtilService.getExtendLoanLifeCycleConfig().isEnabled();

        if (!isExtendLoanLifeCycleConfig) {
            throw new GeneralPlatformDomainRuleException("error.msg.Add-More-Stages-To-A-Loan-Life-Cycle.is.not.set",
                    "Add-More-Stages-To-A-Loan-Life-Cycle settings is not set. So this operation is not permitted");
        }

        if (loanDecision != null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.account.should.not.exist.in.decision.engine",
                    "Loan Account found in decision engine. Operation [Review Application] is not allowed");
        }
        loanDecisionStateUtilService.checkClientOrGroupActive(loan);

        loanDecisionStateUtilService.validateLoanDisbursementDataWithMeetingDate(loan);
        loanDecisionStateUtilService.validateLoanTopUp(loan);

        LocalDate loanReviewOnDate = command.localDateValueOfParameterNamed(LoanApiConstants.loanReviewOnDateParameterName);
        if (loanReviewOnDate.isBefore(loan.getSubmittedOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.review.application.date.should.be.after.submission.date",
                    "Loan Review Application date " + loanReviewOnDate + " should be after submission date " + loan.getSubmittedOnDate());
        }

        if (!loan.status().isSubmittedAndPendingApproval()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.current.status.is.invalid",
                    "Loan Account current status is invalid. Expected" + loan.status().getCode() + " but found " + loan.status().getCode());
        }
    }

    @Override
    public CommandProcessingResult applyDueDiligence(Long loanId, JsonCommand command) {

        final AppUser currentUser = getAppUserIfPresent();

        this.loanDecisionTransitionApiJsonValidator.validateDueDiligence(command.json());
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        Boolean isIdeaClient = command.booleanObjectValueOfParameterNamed(LoanApiConstants.isIdeaClientParamName);
        if (isIdeaClient == null) {
            isIdeaClient = Boolean.FALSE;
        }

        //Check CRB Verification has been executed
        if (loan.getCurrencyCode().equalsIgnoreCase("KES")) {
            List<MetropolCrbIdentityReport> metropolCrbIdentityReportList = metropolCrbIdentityVerificationRepository.findByLoanId(loan.getId());
            if (metropolCrbIdentityReportList.isEmpty()) {
                throw new LoanDueDiligenceException("error.msg.required.crb.verification", "CRB Verification required.");
            }
        } else if (loan.getCurrencyCode().equalsIgnoreCase("RWF")) {
            //transunion
            List<TransunionCrbHeader> transunionCrbHeaderList = transunionCrbHeaderRepository.findByLoanId(loan.getId());
            if (transunionCrbHeaderList.isEmpty()) {
                throw new LoanDueDiligenceException("error.msg.required.crb.verification", "CRB Verification required.");
            }
        }

        if (!isIdeaClient) {
            //check for cashflow and financial ratio. Idea Client does not have a cashflow/ balancesheet
            final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
            final MathContext mc = new MathContext(8, roundingMode);

            List<LoanCashFlowData> cashFlowData = this.loanReadPlatformService.retrieveCashFlow(loanId);
            if (CollectionUtils.isEmpty(cashFlowData)) {
                throw new LoanDueDiligenceException("error.msg.loan.required.cashflow.data",
                        "CashFlow data not available.");
            }
            LoanFinancialRatioData financialRatioData = this.loanReadPlatformService.findLoanFinancialRatioDataByLoanId(loanId);
            if (financialRatioData == null) {
                throw new LoanDueDiligenceException("error.msg.loan.required.financialRatio.data", "Financial Ratio data not available.");
            }
            this.loanReadPlatformService.generateFinancialRatioData(loan, cashFlowData, financialRatioData);

            BigDecimal maxEMI = financialRatioData.getNetCashFlow().divide(loan.getLoanProduct().getAllowableDSCR(), mc);
            BigDecimal calculatedAmount = maxEMI.multiply(BigDecimal.valueOf(loan.getNumberOfRepayments()), mc);
            final BigDecimal recommendedAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.dueDiligenceRecommendedAmountParameterName);
            if (recommendedAmount.compareTo(calculatedAmount) > 0) {
                throw new PlatformDataIntegrityException("error.msg.loan.recommended.amount.cannot.be.greater.than.calculated.amount",
                        "Recommended amount cannot be greater than the calculated amount", calculatedAmount);
            }

        }
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());

        validateDueDiligenceBusinessRule(command, loan, loanDecision);

        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleDueDiligenceFrom(command, currentUser, loanDecision);
        LoanDecision savedObj = loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.DUE_DILIGENCE.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        LoanDueDiligenceInfo loanDueDiligenceInfo = loanDecisionAssembler.assembleDueDiligenceDetailsFrom(command, savedObj, loanObj);
        loanDueDiligenceInfoRepository.saveAndFlush(loanDueDiligenceInfo);

        if (StringUtils.isNotBlank(loanDecisionObj.getDueDiligenceNote())) {
            final Note note = Note.loanNote(loanObj, "Due Diligence : " + loanDecisionObj.getDueDiligenceNote());
            this.noteRepository.save(note);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(savedObj.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .withResourceIdAsString(savedObj.getId().toString()).build();
    }

    @Override
    public CommandProcessingResult acceptLoanCollateralReview(Long loanId, JsonCommand command) {

        final AppUser currentUser = getAppUserIfPresent();

        this.loanDecisionTransitionApiJsonValidator.validateCollateralReview(command.json());

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());

        loanDecisionStateUtilService.validateCollateralReviewBusinessRule(command, loan, loanDecision);

        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleCollateralReviewFrom(command, currentUser, loanDecision);
        LoanDecision savedObj = loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.COLLATERAL_REVIEW.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getCollateralReviewNote())) {
            final Note note = Note.loanNote(loanObj, "Collateral Review : " + loanDecisionObj.getCollateralReviewNote());
            this.noteRepository.save(note);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(savedObj.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .withResourceIdAsString(savedObj.getId().toString()).build();
    }

    @Override
    public CommandProcessingResult createLoanApprovalMatrix(JsonCommand command) {

        Boolean isExtendLoanLifeCycleConfig = loanDecisionStateUtilService.getExtendLoanLifeCycleConfig().isEnabled();
        if (!isExtendLoanLifeCycleConfig) {
            throw new GeneralPlatformDomainRuleException("error.msg.Add-More-Stages-To-A-Loan-Life-Cycle.is.not.set",
                    "Add-More-Stages-To-A-Loan-Life-Cycle settings is not set. So this operation is not permitted");
        }

        this.loanDecisionTransitionApiJsonValidator.validateCreateApprovalMatrix(command.json());

        final String currency = command.stringValueOfParameterNamed(LoanApprovalMatrixConstants.currencyParameterName);
        LoanApprovalMatrix loanApprovalMatrix = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(currency);

        if (loanApprovalMatrix != null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.already.exist.",
                    String.format("Loan Approval Matrix with Currency [ %s ] exist. Only One currency per Matrix is accepted", currency));
        }

        LoanApprovalMatrix loanApprovalMatrixFrom = loanDecisionAssembler.assembleLoanApprovalMatrixFrom(command);
        this.loanApprovalMatrixRepository.saveAndFlush(loanApprovalMatrixFrom);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loanApprovalMatrixFrom.getId()) //
                .withResourceIdAsString(loanApprovalMatrixFrom.getId().toString()).build();

    }

    @Override
    public CommandProcessingResult deleteLoanApprovalMatrix(Long matrixId) {
        Boolean isExtendLoanLifeCycleConfig = loanDecisionStateUtilService.getExtendLoanLifeCycleConfig().isEnabled();

        if (!isExtendLoanLifeCycleConfig) {
            throw new GeneralPlatformDomainRuleException("error.msg.Add-More-Stages-To-A-Loan-Life-Cycle.is.not.set",
                    "Add-More-Stages-To-A-Loan-Life-Cycle settings is not set. So this operation is not permitted");
        }

        LoanApprovalMatrix loanApprovalMatrix = this.loanApprovalMatrixRepository.findById(matrixId).orElseThrow();

        this.loanApprovalMatrixRepository.delete(loanApprovalMatrix);

        return new CommandProcessingResultBuilder() //
                .withEntityId(matrixId) //
                .withResourceIdAsString(matrixId.toString()).build();
    }

    @Override
    public CommandProcessingResult updateLoanApprovalMatrix(JsonCommand command, Long matrixId) {
        try {
            this.context.authenticatedUser();

            Boolean isExtendLoanLifeCycleConfig = loanDecisionStateUtilService.getExtendLoanLifeCycleConfig().isEnabled();

            if (!isExtendLoanLifeCycleConfig) {
                throw new GeneralPlatformDomainRuleException("error.msg.Add-More-Stages-To-A-Loan-Life-Cycle.is.not.set",
                        "Add-More-Stages-To-A-Loan-Life-Cycle settings is not set. So this operation is not permitted");
            }
            this.loanDecisionTransitionApiJsonValidator.validateUpdateApprovalMatrix(command.json());

            LoanApprovalMatrix loanApprovalMatrix = this.loanApprovalMatrixRepository.findById(matrixId).orElseThrow();

            final String currency = command.stringValueOfParameterNamed(LoanApprovalMatrixConstants.currencyParameterName);

            if (!currency.equals(loanApprovalMatrix.getCurrency())) {
                LoanApprovalMatrix matrixCurrency = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(currency);

                if (matrixCurrency != null && !matrixCurrency.getId().equals(loanApprovalMatrix.getId())) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.already.exist.", String
                            .format("Loan Approval Matrix with Currency [ %s ] exist. Only One currency per Matrix is accepted", currency));
                }
            }

            final Map<String, Object> changes = loanApprovalMatrix.update(command);

            if (!changes.isEmpty()) {
                this.loanApprovalMatrixRepository.saveAndFlush(loanApprovalMatrix);
            }

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withResourceIdAsString(loanApprovalMatrix.getId().toString()) //
                    .withEntityId(loanApprovalMatrix.getId()) //
                    .with(changes) //
                    .build();
        } catch (JpaSystemException | PersistenceException ex) {
            return CommandProcessingResult.empty();
        }
    }

    @Override
    public CommandProcessingResult acceptIcReviewDecisionLevelOne(Long loanId, JsonCommand command) {
        final AppUser currentUser = getAppUserIfPresent();

        this.loanDecisionTransitionApiJsonValidator.validateIcReviewStage(command.json());

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());

        LocalDate icReviewOn = command.localDateValueOfParameterNamed(LoanApiConstants.icReviewOnDateParameterName);
        final BigDecimal recommendedAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.icReviewRecommendedAmount);
        final Integer termFrequency = command.integerValueOfParameterNamed(LoanApiConstants.icReviewTermFrequency);
        final Integer termPeriodFrequencyEnum = command.integerValueOfParameterNamed(LoanApiConstants.icReviewTermPeriodFrequencyEnum);

        loanDecisionStateUtilService.validateIcReviewDecisionLevelOneBusinessRule(command, loan, loanDecision, icReviewOn);
        LoanApprovalMatrix approvalMatrix = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(loan.getCurrencyCode());

        if (approvalMatrix == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.does.not.exist.",
                    String.format("Loan Approval Matrix with Currency [ %s ] doesn't exist. Approval matrix is expected to continue ",
                            loan.getCurrencyCode()));
        }

        /* TODO Get max loan amount from cash flow calculation */
        final BigDecimal maxLoanAmount = BigDecimal.valueOf(100000);
        final int comparisonResult = maxLoanAmount.compareTo(recommendedAmount);
        if(comparisonResult < 0) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.ic.review.recommended.amount.must.be.lower.than.max.loan.amount",
                    String.format("IC Review recommended value must be lower than Loan maximum amount from cash flow calculation"));
        }
        // Get Loan Matrix
        // Determine which cycle of this Loan Account
        // Determine the Next Level or stage to review
        // Add custom Params in Decision Table
        List<Loan> loanIndividualCounter = loanDecisionStateUtilService.getLoanCounter(loan);

        Boolean isLoanFirstCycle = loanDecisionStateUtilService.isLoanFirstCycle(loanIndividualCounter);
        Boolean isLoanUnsecure = loanDecisionStateUtilService.isLoanUnSecure(loan);

        loanDecisionStateUtilService.validateLoanAccountToComplyToApprovalMatrixStage(loan, approvalMatrix, isLoanFirstCycle,
                isLoanUnsecure, LoanDecisionState.IC_REVIEW_LEVEL_ONE);
        // generate the next stage based on loan approval matrix via amounts to be disbursed
        loanDecisionStateUtilService.determineTheNextDecisionStage(loan, loanDecision, approvalMatrix, isLoanFirstCycle, isLoanUnsecure,
                LoanDecisionState.IC_REVIEW_LEVEL_ONE);

        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleIcReviewDecisionLevelOneFrom(command, currentUser, loanDecision, false,
                icReviewOn, recommendedAmount, termFrequency, termPeriodFrequencyEnum);
        LoanDecision savedObj = loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_ONE.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getIcReviewDecisionLevelOneNote())) {
            final Note note = Note.loanNote(loanObj,
                    "Approve IC Review-Decision Level One : " + loanDecisionObj.getIcReviewDecisionLevelOneNote() +
                    " Recommended Amount : " + recommendedAmount + " " + loan.getCurrencyCode() +
                    " Loan Term : " + termFrequency + " " + PeriodFrequencyType.fromInt(termPeriodFrequencyEnum));
            this.noteRepository.save(note);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(savedObj.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .withResourceIdAsString(savedObj.getId().toString()).build();
    }

    @Override
    public CommandProcessingResult acceptIcReviewDecisionLevelTwo(Long loanId, JsonCommand command) {
        final AppUser currentUser = getAppUserIfPresent();

        this.loanDecisionTransitionApiJsonValidator.validateIcReviewStage(command.json());

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());

        LocalDate icReviewOn = command.localDateValueOfParameterNamed(LoanApiConstants.icReviewOnDateParameterName);
        final BigDecimal recommendedAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.icReviewRecommendedAmount);
        final Integer termFrequency = command.integerValueOfParameterNamed(LoanApiConstants.icReviewTermFrequency);
        final Integer termPeriodFrequencyEnum = command.integerValueOfParameterNamed(LoanApiConstants.icReviewTermPeriodFrequencyEnum);

        loanDecisionStateUtilService.validateIcReviewDecisionLevelTwoBusinessRule(command, loan, loanDecision, icReviewOn);
        LoanApprovalMatrix approvalMatrix = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(loan.getCurrencyCode());

        if (approvalMatrix == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.does.not.exist.",
                    String.format("Loan Approval Matrix with Currency [ %s ] doesn't exist. Approval matrix is expected to continue ",
                            loan.getCurrencyCode()));
        }

        /* TODO Get max loan amount from cash flow calculation */
        final BigDecimal maxLoanAmount = BigDecimal.valueOf(100000);
        final int comparisonResult = maxLoanAmount.compareTo(recommendedAmount);
        if(comparisonResult < 0) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.ic.review.recommended.amount.must.be.lower.than.max.loan.amount",
                    String.format("IC Review recommended value must be lower than Loan maximum amount from cash flow calculation"));
        }
        // Get Loan Matrix
        // Determine which cycle of this Loan Account
        // Determine the Next Level or stage to review
        // Add custom Params in Decision Table
        List<Loan> loanIndividualCounter = loanDecisionStateUtilService.getLoanCounter(loan);

        Boolean isLoanFirstCycle = loanDecisionStateUtilService.isLoanFirstCycle(loanIndividualCounter);
        Boolean isLoanUnsecure = loanDecisionStateUtilService.isLoanUnSecure(loan);

        loanDecisionStateUtilService.validateLoanAccountToComplyToApprovalMatrixStage(loan, approvalMatrix, isLoanFirstCycle,
                isLoanUnsecure, LoanDecisionState.IC_REVIEW_LEVEL_TWO);
        // generate the next stage based on loan approval matrix via amounts to be disbursed
        loanDecisionStateUtilService.determineTheNextDecisionStage(loan, loanDecision, approvalMatrix, isLoanFirstCycle, isLoanUnsecure,
                LoanDecisionState.IC_REVIEW_LEVEL_TWO);

        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleIcReviewDecisionLevelTwoFrom(command, currentUser, loanDecision,
                Boolean.FALSE, icReviewOn, recommendedAmount, termFrequency, termPeriodFrequencyEnum);
        LoanDecision savedObj = loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_TWO.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getIcReviewDecisionLevelTwoNote())) {
            final Note note = Note.loanNote(loanObj,
                    "Approve IC Review-Decision Level Two : " + loanDecisionObj.getIcReviewDecisionLevelTwoNote() +
                            " Recommended Amount : " + recommendedAmount + " " + loan.getCurrencyCode() +
                            " Loan Term : " + termFrequency + " " + PeriodFrequencyType.fromInt(termPeriodFrequencyEnum));
            this.noteRepository.save(note);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(savedObj.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .withResourceIdAsString(savedObj.getId().toString()).build();
    }

    @Override
    public CommandProcessingResult acceptIcReviewDecisionLevelThree(Long loanId, JsonCommand command) {
        final AppUser currentUser = getAppUserIfPresent();

        this.loanDecisionTransitionApiJsonValidator.validateIcReviewStage(command.json());

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());

        LocalDate icReviewOn = command.localDateValueOfParameterNamed(LoanApiConstants.icReviewOnDateParameterName);
        final BigDecimal recommendedAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.icReviewRecommendedAmount);
        final Integer termFrequency = command.integerValueOfParameterNamed(LoanApiConstants.icReviewTermFrequency);
        final Integer termPeriodFrequencyEnum = command.integerValueOfParameterNamed(LoanApiConstants.icReviewTermPeriodFrequencyEnum);

        loanDecisionStateUtilService.validateIcReviewDecisionLevelThreeBusinessRule(command, loan, loanDecision, icReviewOn);
        LoanApprovalMatrix approvalMatrix = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(loan.getCurrencyCode());

        if (approvalMatrix == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.does.not.exist.",
                    String.format("Loan Approval Matrix with Currency [ %s ] doesn't exist. Approval matrix is expected to continue ",
                            loan.getCurrencyCode()));
        }

        /* TODO Get max loan amount from cash flow calculation */
        final BigDecimal maxLoanAmount = BigDecimal.valueOf(100000);
        final int comparisonResult = maxLoanAmount.compareTo(recommendedAmount);
        if(comparisonResult < 0) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.ic.review.recommended.amount.must.be.lower.than.max.loan.amount",
                    String.format("IC Review recommended value must be lower than Loan maximum amount from cash flow calculation"));
        }
        // Get Loan Matrix
        // Determine which cycle of this Loan Account
        // Determine the Next Level or stage to review
        // Add custom Params in Decision Table
        List<Loan> loanIndividualCounter = loanDecisionStateUtilService.getLoanCounter(loan);

        Boolean isLoanFirstCycle = loanDecisionStateUtilService.isLoanFirstCycle(loanIndividualCounter);
        Boolean isLoanUnsecure = loanDecisionStateUtilService.isLoanUnSecure(loan);

        loanDecisionStateUtilService.validateLoanAccountToComplyToApprovalMatrixStage(loan, approvalMatrix, isLoanFirstCycle,
                isLoanUnsecure, LoanDecisionState.IC_REVIEW_LEVEL_THREE);
        // generate the next stage based on loan approval matrix via amounts to be disbursed
        loanDecisionStateUtilService.determineTheNextDecisionStage(loan, loanDecision, approvalMatrix, isLoanFirstCycle, isLoanUnsecure,
                LoanDecisionState.IC_REVIEW_LEVEL_THREE);

        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleIcReviewDecisionLevelThreeFrom(command, currentUser, loanDecision,
                Boolean.FALSE, icReviewOn, recommendedAmount, termFrequency, termPeriodFrequencyEnum);
        LoanDecision savedObj = loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_THREE.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getIcReviewDecisionLevelThreeNote())) {
            final Note note = Note.loanNote(loanObj,
                    "Approve IC Review-Decision Level Three : " + loanDecisionObj.getIcReviewDecisionLevelThreeNote() +
                            " Recommended Amount : " + recommendedAmount + " " + loan.getCurrencyCode() +
                            " Loan Term : " + termFrequency + " " + PeriodFrequencyType.fromInt(termPeriodFrequencyEnum));
            this.noteRepository.save(note);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(savedObj.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .withResourceIdAsString(savedObj.getId().toString()).build();
    }

    @Override
    public CommandProcessingResult acceptIcReviewDecisionLevelFour(Long loanId, JsonCommand command) {
        final AppUser currentUser = getAppUserIfPresent();

        this.loanDecisionTransitionApiJsonValidator.validateIcReviewStage(command.json());

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());

        LocalDate icReviewOn = command.localDateValueOfParameterNamed(LoanApiConstants.icReviewOnDateParameterName);
        final BigDecimal recommendedAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.icReviewRecommendedAmount);
        final Integer termFrequency = command.integerValueOfParameterNamed(LoanApiConstants.icReviewTermFrequency);
        final Integer termPeriodFrequencyEnum = command.integerValueOfParameterNamed(LoanApiConstants.icReviewTermPeriodFrequencyEnum);

        loanDecisionStateUtilService.validateIcReviewDecisionLevelFourBusinessRule(command, loan, loanDecision, icReviewOn);
        LoanApprovalMatrix approvalMatrix = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(loan.getCurrencyCode());

        if (approvalMatrix == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.does.not.exist.",
                    String.format("Loan Approval Matrix with Currency [ %s ] doesn't exist. Approval matrix is expected to continue ",
                            loan.getCurrencyCode()));
        }

        /* TODO Get max loan amount from cash flow calculation */
        final BigDecimal maxLoanAmount = BigDecimal.valueOf(100000);
        final int comparisonResult = maxLoanAmount.compareTo(recommendedAmount);
        if(comparisonResult < 0) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.ic.review.recommended.amount.must.be.lower.than.max.loan.amount",
                    String.format("IC Review recommended value must be lower than Loan maximum amount from cash flow calculation"));
        }
        // Get Loan Matrix
        // Determine which cycle of this Loan Account
        // Determine the Next Level or stage to review
        // Add custom Params in Decision Table
        List<Loan> loanIndividualCounter = loanDecisionStateUtilService.getLoanCounter(loan);

        Boolean isLoanFirstCycle = loanDecisionStateUtilService.isLoanFirstCycle(loanIndividualCounter);
        Boolean isLoanUnsecure = loanDecisionStateUtilService.isLoanUnSecure(loan);

        loanDecisionStateUtilService.validateLoanAccountToComplyToApprovalMatrixStage(loan, approvalMatrix, isLoanFirstCycle,
                isLoanUnsecure, LoanDecisionState.IC_REVIEW_LEVEL_FOUR);
        // generate the next stage based on loan approval matrix via amounts to be disbursed
        loanDecisionStateUtilService.determineTheNextDecisionStage(loan, loanDecision, approvalMatrix, isLoanFirstCycle, isLoanUnsecure,
                LoanDecisionState.IC_REVIEW_LEVEL_FOUR);

        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleIcReviewDecisionLevelFourFrom(command, currentUser, loanDecision,
                Boolean.FALSE, icReviewOn, recommendedAmount, termFrequency, termPeriodFrequencyEnum);
        LoanDecision savedObj = loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_FOUR.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getIcReviewDecisionLevelFourNote())) {
            final Note note = Note.loanNote(loanObj,
                    "Approve IC Review-Decision Level Four : " + loanDecisionObj.getIcReviewDecisionLevelFourNote() +
                            " Recommended Amount : " + recommendedAmount + " " + loan.getCurrencyCode() +
                            " Loan Term : " + termFrequency + " " + PeriodFrequencyType.fromInt(termPeriodFrequencyEnum));
            this.noteRepository.save(note);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(savedObj.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .withResourceIdAsString(savedObj.getId().toString()).build();
    }

    @Override
    public CommandProcessingResult acceptIcReviewDecisionLevelFive(Long loanId, JsonCommand command) {
        final AppUser currentUser = getAppUserIfPresent();

        this.loanDecisionTransitionApiJsonValidator.validateIcReviewStage(command.json());

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());

        LocalDate icReviewOn = command.localDateValueOfParameterNamed(LoanApiConstants.icReviewOnDateParameterName);
        final BigDecimal recommendedAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.icReviewRecommendedAmount);
        final Integer termFrequency = command.integerValueOfParameterNamed(LoanApiConstants.icReviewTermFrequency);
        final Integer termPeriodFrequencyEnum = command.integerValueOfParameterNamed(LoanApiConstants.icReviewTermPeriodFrequencyEnum);

        loanDecisionStateUtilService.validateIcReviewDecisionLevelFiveBusinessRule(command, loan, loanDecision, icReviewOn);
        LoanApprovalMatrix approvalMatrix = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(loan.getCurrencyCode());

        if (approvalMatrix == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.does.not.exist.",
                    String.format("Loan Approval Matrix with Currency [ %s ] doesn't exist. Approval matrix is expected to continue ",
                            loan.getCurrencyCode()));
        }

        /* TODO Get max loan amount from cash flow calculation */
        final BigDecimal maxLoanAmount = BigDecimal.valueOf(100000);
        final int comparisonResult = maxLoanAmount.compareTo(recommendedAmount);
        if(comparisonResult < 0) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.ic.review.recommended.amount.must.be.lower.than.max.loan.amount",
                    String.format("IC Review recommended value must be lower than Loan maximum amount from cash flow calculation"));
        }
        // Get Loan Matrix
        // Determine which cycle of this Loan Account
        // Determine the Next Level or stage to review
        // Add custom Params in Decision Table
        List<Loan> loanIndividualCounter = loanDecisionStateUtilService.getLoanCounter(loan);

        Boolean isLoanFirstCycle = loanDecisionStateUtilService.isLoanFirstCycle(loanIndividualCounter);
        Boolean isLoanUnsecure = loanDecisionStateUtilService.isLoanUnSecure(loan);

        loanDecisionStateUtilService.validateLoanAccountToComplyToApprovalMatrixStage(loan, approvalMatrix, isLoanFirstCycle,
                isLoanUnsecure, LoanDecisionState.IC_REVIEW_LEVEL_FIVE);

        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleIcReviewDecisionLevelFiveFrom(command, currentUser, loanDecision,
                Boolean.FALSE, icReviewOn, recommendedAmount, termFrequency, termPeriodFrequencyEnum);
        LoanDecision savedObj = loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_FIVE.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getIcReviewDecisionLevelFiveNote())) {
            final Note note = Note.loanNote(loanObj,
                    "Approve IC Review-Decision Level Five : " + loanDecisionObj.getIcReviewDecisionLevelFiveNote() +
                            " Recommended Amount : " + recommendedAmount + " " + loan.getCurrencyCode() +
                            " Loan Term : " + termFrequency + " " + PeriodFrequencyType.fromInt(termPeriodFrequencyEnum));
            this.noteRepository.save(note);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(savedObj.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .withResourceIdAsString(savedObj.getId().toString()).build();
    }

    @Override
    public CommandProcessingResult acceptPrepareAndSignContract(Long loanId, JsonCommand command) {
        final AppUser currentUser = getAppUserIfPresent();

        final Collection<DocumentData> documentData = this.documentReadPlatformService.retrieveLoanDocumentsFilterByDocumentType(LoanApiConstants.loanEntityType, loanId, LoanApiConstants.loanDocumentTypeContract);
        if (documentData.size() < 1) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.document.with.type.contract.not.found",
                    "Loan contract document not found. Please upload loan document with type Contract");
        }

        this.loanDecisionTransitionApiJsonValidator.validatePrepareAndSignContractStage(command.json());

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());

        loanDecisionStateUtilService.validatePrepareAndSignContractBusinessRule(command, loan, loanDecision);
        LoanApprovalMatrix approvalMatrix = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(loan.getCurrencyCode());

        if (approvalMatrix == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.does.not.exist.",
                    String.format("Loan Approval Matrix with Currency [ %s ] doesn't exist. Approval matrix is expected to continue ",
                            loan.getCurrencyCode()));
        }

        LoanDecision loanDecisionObj = loanDecisionAssembler.assemblePrepareAndSignContractFrom(command, currentUser, loanDecision);
        LoanDecision savedObj = loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.PREPARE_AND_SIGN_CONTRACT.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getPrepareAndSignContractNote())) {
            final Note note = Note.loanNote(loanObj, "Prepare And Sign Contract : " + loanDecisionObj.getPrepareAndSignContractNote());
            this.noteRepository.save(note);
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(savedObj.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .withResourceIdAsString(savedObj.getId().toString()).build();
    }

    private void validateDueDiligenceBusinessRule(JsonCommand command, Loan loan, LoanDecision loanDecision) {
        Boolean isExtendLoanLifeCycleConfig = loanDecisionStateUtilService.getExtendLoanLifeCycleConfig().isEnabled();

        if (!isExtendLoanLifeCycleConfig) {
            throw new GeneralPlatformDomainRuleException("error.msg.Add-More-Stages-To-A-Loan-Life-Cycle.is.not.set",
                    "Add-More-Stages-To-A-Loan-Life-Cycle settings is not set. So this operation is not permitted");
        }

        if (loanDecision == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.account.should.not.found.in.decision.engine",
                    "Loan Account not found in decision engine. Operation [Due Diligence] is not allowed");
        }
        loanDecisionStateUtilService.checkClientOrGroupActive(loan);

        loanDecisionStateUtilService.validateLoanDisbursementDataWithMeetingDate(loan);
        loanDecisionStateUtilService.validateLoanTopUp(loan);
        LocalDate dueDiligenceOn = command.localDateValueOfParameterNamed(LoanApiConstants.dueDiligenceOnDateParameterName);
        // Review Loan Application should not be before Due Diligence date
        if (dueDiligenceOn.isBefore(loanDecision.getReviewApplicationOn())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.due.diligence.date.should.be.after.review.application.date",
                    "Approve Due Diligence date" + dueDiligenceOn + " should be after Loan Review Application date "
                            + loanDecision.getReviewApplicationOn());
        }
        // Due Diligence date should not be before loan submission date
        if (dueDiligenceOn.isBefore(loan.getSubmittedOnDate())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.review.application.date.should.be.after.submission.date",
                    "Approve Due Diligence date " + dueDiligenceOn + " should be after Loan submission date " + loan.getSubmittedOnDate());
        }

        if (!loan.status().isSubmittedAndPendingApproval()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.current.status.is.invalid",
                    "Loan Account current status is invalid. Expected" + loan.status().getCode() + " but found " + loan.status().getCode());
        }
        if (!LoanDecisionState.fromInt(loan.getLoanDecisionState()).isReviewApplication()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.is.invalid",
                    "Loan Account Decision state is invalid. Expected" + LoanDecisionState.REVIEW_APPLICATION.getValue() + " but found "
                            + loan.getLoanDecisionState());
        }
        if (!loan.getLoanDecisionState().equals(loanDecision.getLoanDecisionState())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.decision.state.does.not.reconcile",
                    "Loan Account Decision state Does not reconcile . Operation is terminated");
        }

    }

    private AppUser getAppUserIfPresent() {
        AppUser user = null;
        if (this.context != null) {
            user = this.context.getAuthenticatedUserIfPresent();
        }
        return user;
    }

}
