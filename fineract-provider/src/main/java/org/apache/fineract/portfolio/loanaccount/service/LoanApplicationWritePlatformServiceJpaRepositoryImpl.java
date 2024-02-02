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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormat;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormatRepositoryWrapper;
import org.apache.fineract.infrastructure.accountnumberformat.domain.EntityAccountType;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationProperty;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.api.JsonQuery;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksWritePlatformService;
import org.apache.fineract.infrastructure.entityaccess.FineractEntityAccessConstants;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityAccessType;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityRelation;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityRelationRepository;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityToEntityMapping;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityToEntityMappingRepository;
import org.apache.fineract.infrastructure.entityaccess.exception.NotOfficeSpecificProductException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.portfolio.account.data.PortfolioAccountData;
import org.apache.fineract.portfolio.account.domain.AccountAssociationType;
import org.apache.fineract.portfolio.account.domain.AccountAssociations;
import org.apache.fineract.portfolio.account.domain.AccountAssociationsRepository;
import org.apache.fineract.portfolio.account.service.AccountAssociationsReadPlatformService;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.businessevent.domain.loan.LoanApprovedBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.loan.LoanCreatedBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.loan.LoanRejectedBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.loan.LoanUndoApprovalBusinessEvent;
import org.apache.fineract.portfolio.businessevent.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.domain.CalendarFrequencyType;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstance;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstanceRepository;
import org.apache.fineract.portfolio.calendar.domain.CalendarRepository;
import org.apache.fineract.portfolio.calendar.domain.CalendarType;
import org.apache.fineract.portfolio.calendar.exception.CalendarNotFoundException;
import org.apache.fineract.portfolio.calendar.service.CalendarReadPlatformService;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.client.domain.AccountNumberGenerator;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.collateralmanagement.domain.ClientCollateralManagement;
import org.apache.fineract.portfolio.collateralmanagement.service.LoanCollateralAssembler;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.fund.domain.Fund;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.domain.GroupRepositoryWrapper;
import org.apache.fineract.portfolio.group.exception.GroupMemberNotFoundInGSIMException;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.data.LoanCashFlowData;
import org.apache.fineract.portfolio.loanaccount.data.LoanCashFlowProjectionData;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.data.LoanFinancialRatioData;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.DefaultLoanLifecycleStateMachine;
import org.apache.fineract.portfolio.loanaccount.domain.GLIMAccountInfoRepository;
import org.apache.fineract.portfolio.loanaccount.domain.GroupLoanIndividualMonitoringAccount;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanAccountDomainService;
import org.apache.fineract.portfolio.loanaccount.domain.LoanApprovalMatrix;
import org.apache.fineract.portfolio.loanaccount.domain.LoanApprovalMatrixRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCashFlowProjection;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCashFlowProjectionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCollateralManagement;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecision;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecisionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecisionState;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDisbursementDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanLifecycleStateMachine;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallmentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleTransactionProcessorFactory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanSummaryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTopupDetails;
import org.apache.fineract.portfolio.loanaccount.exception.GLIMLoanCannotBeApprovedException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanApplicationDateException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanApplicationNotInSubmittedAndPendingApprovalStateCannotBeDeleted;
import org.apache.fineract.portfolio.loanaccount.exception.LoanApplicationNotInSubmittedAndPendingApprovalStateCannotBeModified;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.AprCalculator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanApplicationTerms;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleAssembler;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleCalculationPlatformService;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanApplicationCommandFromApiJsonHelper;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanApplicationTransitionApiJsonValidator;
import org.apache.fineract.portfolio.loanproduct.LoanProductConstants;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRelatedDetail;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.domain.LoanTransactionProcessingStrategy;
import org.apache.fineract.portfolio.loanproduct.domain.RecalculationFrequencyType;
import org.apache.fineract.portfolio.loanproduct.exception.LinkedAccountRequiredException;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.apache.fineract.portfolio.loanproduct.serialization.LoanProductDataValidator;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.rate.service.RateAssembler;
import org.apache.fineract.portfolio.savings.data.GroupSavingsIndividualMonitoringAccountData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.service.GSIMReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class LoanApplicationWritePlatformServiceJpaRepositoryImpl implements LoanApplicationWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(LoanApplicationWritePlatformServiceJpaRepositoryImpl.class);

    private final PlatformSecurityContext context;
    private final FromJsonHelper fromJsonHelper;
    private final LoanApplicationTransitionApiJsonValidator loanApplicationTransitionApiJsonValidator;
    private final LoanProductDataValidator loanProductCommandFromApiJsonDeserializer;
    private final LoanApplicationCommandFromApiJsonHelper fromApiJsonDeserializer;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final NoteRepository noteRepository;
    private final LoanScheduleCalculationPlatformService calculationPlatformService;
    private final LoanAssembler loanAssembler;
    private final ClientRepositoryWrapper clientRepository;
    private final LoanProductRepository loanProductRepository;
    private final LoanChargeAssembler loanChargeAssembler;
    private final LoanCollateralAssembler loanCollateralAssembler;
    private final AprCalculator aprCalculator;
    private final AccountNumberGenerator accountNumberGenerator;
    private final LoanSummaryWrapper loanSummaryWrapper;
    private final GroupRepositoryWrapper groupRepository;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;
    private final CalendarRepository calendarRepository;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final SavingsAccountAssembler savingsAccountAssembler;
    private final AccountAssociationsRepository accountAssociationsRepository;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository;
    private final AccountNumberFormatRepositoryWrapper accountNumberFormatRepository;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final ConfigurationDomainService configurationDomainService;
    private final LoanScheduleAssembler loanScheduleAssembler;
    private final LoanUtilService loanUtilService;
    private final CalendarReadPlatformService calendarReadPlatformService;
    private final EntityDatatableChecksWritePlatformService entityDatatableChecksWritePlatformService;
    private final GlobalConfigurationRepositoryWrapper globalConfigurationRepository;
    private final FineractEntityToEntityMappingRepository repository;
    private final FineractEntityRelationRepository fineractEntityRelationRepository;
    private final LoanProductReadPlatformService loanProductReadPlatformService;

    private final RateAssembler rateAssembler;
    private final GLIMAccountInfoWritePlatformService glimAccountInfoWritePlatformService;
    private final GLIMAccountInfoRepository glimRepository;
    private final LoanRepository loanRepository;
    private final GSIMReadPlatformService gsimReadPlatformService;
    private final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService;
    private final LoanAccountDomainService loanAccountDomainService;

    private final LoanDecisionStateUtilService loanDecisionStateUtilService;
    private final LoanDecisionRepository loanDecisionRepository;
    private final LoanApprovalMatrixRepository loanApprovalMatrixRepository;
    private final LoanDecisionAssembler loanDecisionAssembler;
    private final LoanCashFlowProjectionRepository loanCashFlowProjectionRepository;

    private LoanLifecycleStateMachine defaultLoanLifecycleStateMachine() {
        final List<LoanStatus> allowedLoanStatuses = Arrays.asList(LoanStatus.values());
        return new DefaultLoanLifecycleStateMachine(allowedLoanStatuses);
    }

    @Transactional
    @Override
    public CommandProcessingResult submitApplication(final JsonCommand command) {

        try {
            boolean isMeetingMandatoryForJLGLoans = configurationDomainService.isMeetingMandatoryForJLGLoans();
            final Long productId = this.fromJsonHelper.extractLongNamed("productId", command.parsedJson());
            final LoanProduct loanProduct = this.loanProductRepository.findById(productId)
                    .orElseThrow(() -> new LoanProductNotFoundException(productId));

            final Long clientId = this.fromJsonHelper.extractLongNamed("clientId", command.parsedJson());
            if (clientId != null) {
                Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
                officeSpecificLoanProductValidation(productId, client.getOffice().getId());
            }
            final Long groupId = this.fromJsonHelper.extractLongNamed("groupId", command.parsedJson());
            if (groupId != null) {
                Group group = this.groupRepository.findOneWithNotFoundDetection(groupId);
                officeSpecificLoanProductValidation(productId, group.getOffice().getId());
            }

            this.fromApiJsonDeserializer.validateForCreate(command.json(), isMeetingMandatoryForJLGLoans, loanProduct);

            // Validate If the externalId is already registered
            final String externalId = this.fromJsonHelper.extractStringNamed("externalId", command.parsedJson());
            if (StringUtils.isNotBlank(externalId)) {
                final boolean existByExternalId = this.loanRepositoryWrapper.existLoanByExternalId(externalId);
                if (existByExternalId) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.with.externalId.already.used",
                            "Loan with externalId is already registered.");
                }
            }

            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");

            if (loanProduct.useBorrowerCycle()) {
                Integer cycleNumber = 0;
                if (clientId != null) {
                    cycleNumber = this.loanReadPlatformService.retriveLoanCounter(clientId, loanProduct.getId());
                } else if (groupId != null) {
                    cycleNumber = this.loanReadPlatformService.retriveLoanCounter(groupId, AccountType.GROUP.getValue(),
                            loanProduct.getId());
                }
                this.loanProductCommandFromApiJsonDeserializer.validateMinMaxConstraints(command.parsedJson(), baseDataValidator,
                        loanProduct, cycleNumber);
            } else {
                this.loanProductCommandFromApiJsonDeserializer.validateMinMaxConstraints(command.parsedJson(), baseDataValidator,
                        loanProduct);
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }

            final Loan newLoanApplication = this.loanAssembler.assembleFrom(command);

            checkForProductMixRestrictions(newLoanApplication);

            validateSubmittedOnDate(newLoanApplication);

            final LoanProductRelatedDetail productRelatedDetail = newLoanApplication.repaymentScheduleDetail();

            if (loanProduct.getLoanProductConfigurableAttributes() != null) {
                updateProductRelatedDetails(productRelatedDetail, newLoanApplication);
            }
            final Boolean isTopup = command.booleanObjectValueOfParameterNamed(LoanApiConstants.isTopup);
            final Boolean loanTermIncludesToppedUpLoanTerm = command
                    .booleanObjectValueOfParameterNamed(LoanApiConstants.LOAN_TERM_INCLUDES_TOPPED_UP_LOAN_TERM);
            final Long loanIdToClose = command.longValueOfParameterNamed(LoanApiConstants.loanIdToClose);
            final Integer numberOfRepaymentsToCarryForward = command
                    .integerValueOfParameterNamedDefaultToNullIfZero(LoanApiConstants.NUMBER_OF_REPAYMENT_TO_CARRY_FORWARD);

            this.fromApiJsonDeserializer.validateLoanTermAndRepaidEveryValues(newLoanApplication.getTermFrequency(),
                    newLoanApplication.getTermPeriodFrequencyType(), productRelatedDetail.getNumberOfRepayments(),
                    productRelatedDetail.getRepayEvery(), productRelatedDetail.getRepaymentPeriodFrequencyType().getValue(),
                    newLoanApplication, numberOfRepaymentsToCarryForward);

            if (loanProduct.canUseForTopup() && clientId != null) {

                if (null == isTopup) {
                    newLoanApplication.setIsTopup(false);
                } else {
                    newLoanApplication.setIsTopup(isTopup);
                }

                if (null == loanTermIncludesToppedUpLoanTerm) {
                    newLoanApplication.setLoanTermIncludesToppedUpLoanTerm(false);
                } else {
                    newLoanApplication.setLoanTermIncludesToppedUpLoanTerm(loanTermIncludesToppedUpLoanTerm);
                }

                if (newLoanApplication.isTopup()) {
                    final Loan loanToClose = this.loanRepositoryWrapper.findNonClosedLoanThatBelongsToClient(loanIdToClose, clientId);
                    if (loanToClose == null) {
                        throw new GeneralPlatformDomainRuleException(
                                "error.msg.loan.loanIdToClose.no.active.loan.associated.to.client.found",
                                "loanIdToClose is invalid, No Active Loan associated with the given Client ID found.");
                    }
                    if (loanToClose.isMultiDisburmentLoan() && !loanToClose.isInterestRecalculationEnabledForProduct()) {
                        throw new GeneralPlatformDomainRuleException(
                                "error.msg.loan.topup.on.multi.tranche.loan.without.interest.recalculation.not.supported",
                                "Topup on loan with multi-tranche disbursal and without interest recalculation is not supported.");
                    }
                    final LocalDate disbursalDateOfLoanToClose = loanToClose.getDisbursementDate();
                    if (!newLoanApplication.getSubmittedOnDate().isAfter(disbursalDateOfLoanToClose)) {
                        throw new GeneralPlatformDomainRuleException(
                                "error.msg.loan.submitted.date.should.be.after.topup.loan.disbursal.date",
                                "Submitted date of this loan application " + newLoanApplication.getSubmittedOnDate()
                                        + " should be after the disbursed date of loan to be closed " + disbursalDateOfLoanToClose);
                    }
                    if (!loanToClose.getCurrencyCode().equals(newLoanApplication.getCurrencyCode())) {
                        throw new GeneralPlatformDomainRuleException("error.msg.loan.to.be.closed.has.different.currency",
                                "loanIdToClose is invalid, Currency code is different.");
                    }
                    final LocalDate lastUserTransactionOnLoanToClose = loanToClose.getLastUserTransactionDate();
                    if (newLoanApplication.getDisbursementDate().isBefore(lastUserTransactionOnLoanToClose)) {
                        throw new GeneralPlatformDomainRuleException(
                                "error.msg.loan.disbursal.date.should.be.after.last.transaction.date.of.loan.to.be.closed",
                                "Disbursal date of this loan application " + newLoanApplication.getDisbursementDate()
                                        + " should be after last transaction date of loan to be closed "
                                        + lastUserTransactionOnLoanToClose);
                    }
                    BigDecimal loanOutstanding = this.loanReadPlatformService
                            .retrieveLoanForeclosureTemplate(loanIdToClose, newLoanApplication.getDisbursementDate()).getAmount();
                    final BigDecimal firstDisbursalAmount = newLoanApplication.getFirstDisbursalAmount();
                    if (loanOutstanding.compareTo(firstDisbursalAmount) > 0) {
                        throw new GeneralPlatformDomainRuleException("error.msg.loan.amount.less.than.outstanding.of.loan.to.be.closed",
                                "Topup loan amount should be greater than outstanding amount of loan to be closed.");
                    }

                    final LoanTopupDetails topupDetails = new LoanTopupDetails(newLoanApplication, loanIdToClose);
                    newLoanApplication.setTopupLoanDetails(topupDetails);
                }
            }

            this.loanRepositoryWrapper.saveAndFlush(newLoanApplication);

            if (loanProduct.isInterestRecalculationEnabled()) {
                this.fromApiJsonDeserializer.validateLoanForInterestRecalculation(newLoanApplication);
                createAndPersistCalendarInstanceForInterestRecalculation(newLoanApplication);
            }

            // loan account number generation
            String accountNumber = "";
            GroupLoanIndividualMonitoringAccount glimAccount;
            BigDecimal applicationId = BigDecimal.ZERO;
            Boolean isLastChildApplication = false;

            if (newLoanApplication.isAccountNumberRequiresAutoGeneration()) {

                final AccountNumberFormat accountNumberFormat = this.accountNumberFormatRepository
                        .findByAccountType(EntityAccountType.LOAN);
                // if application is of GLIM type
                if (newLoanApplication.getLoanType() == 4) {
                    Group group = this.groupRepository.findOneWithNotFoundDetection(groupId);

                    // GLIM specific parameters
                    if (command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("applicationId") != null) {
                        applicationId = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("applicationId");
                    }

                    if (command.booleanObjectValueOfParameterNamed("lastApplication") != null) {
                        isLastChildApplication = command.booleanPrimitiveValueOfParameterNamed("lastApplication");
                    }

                    if (command.booleanObjectValueOfParameterNamed("isParentAccount") != null) {

                        // empty table check
                        if (glimRepository.count() != 0) {
                            // **************Parent-Not an empty
                            // table********************
                            accountNumber = this.accountNumberGenerator.generate(newLoanApplication, accountNumberFormat);
                            newLoanApplication.updateAccountNo(accountNumber + "1");
                            glimAccountInfoWritePlatformService.addGLIMAccountInfo(accountNumber, group,
                                    command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("totalLoan"), Long.valueOf(1), true,
                                    LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(), applicationId,
                                    command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("totalLoan"));
                            newLoanApplication.setGlim(glimRepository.findOneByAccountNumber(accountNumber));
                            this.loanRepositoryWrapper.save(newLoanApplication);

                        } else {
                            // ************** Parent-empty
                            // table********************

                            accountNumber = this.accountNumberGenerator.generate(newLoanApplication, accountNumberFormat);
                            newLoanApplication.updateAccountNo(accountNumber + "1");
                            glimAccountInfoWritePlatformService.addGLIMAccountInfo(accountNumber, group,
                                    command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("totalLoan"), Long.valueOf(1), true,
                                    LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(), applicationId,
                                    command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("totalLoan"));
                            newLoanApplication.setGlim(glimRepository.findOneByAccountNumber(accountNumber));
                            this.loanRepositoryWrapper.save(newLoanApplication);

                        }

                    } else {

                        if (glimRepository.count() != 0) {
                            // Child-Not an empty table

                            glimAccount = glimRepository.findOneByIsAcceptingChildAndApplicationId(true, applicationId);
                            accountNumber = glimAccount.getAccountNumber() + (glimAccount.getChildAccountsCount() + 1);
                            newLoanApplication.updateAccountNo(accountNumber);
                            this.glimAccountInfoWritePlatformService.incrementChildAccountCount(glimAccount);
                            newLoanApplication.setGlim(glimAccount);
                            this.loanRepositoryWrapper.save(newLoanApplication);

                        } else {
                            // **************Child-empty
                            // table********************
                            // if the glim info is empty set the current account
                            // as parent
                            accountNumber = this.accountNumberGenerator.generate(newLoanApplication, accountNumberFormat);
                            newLoanApplication.updateAccountNo(accountNumber + "1");
                            glimAccountInfoWritePlatformService.addGLIMAccountInfo(accountNumber, group,
                                    command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("totalLoan"), Long.valueOf(1), true,
                                    LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(), applicationId,
                                    command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("totalLoan"));
                            newLoanApplication.setGlim(glimRepository.findOneByAccountNumber(accountNumber));
                            this.loanRepositoryWrapper.save(newLoanApplication);

                        }

                        // reset in cases of last child application of glim

                        if (isLastChildApplication) {
                            this.glimAccountInfoWritePlatformService
                                    .resetIsAcceptingChild(glimRepository.findOneByIsAcceptingChildAndApplicationId(true, applicationId));
                        }

                    }
                } else { // for applications other than GLIM
                    newLoanApplication.updateAccountNo(this.accountNumberGenerator.generate(newLoanApplication, accountNumberFormat));
                    this.loanRepositoryWrapper.saveAndFlush(newLoanApplication);
                }
            }

            final String submittedOnNote = command.stringValueOfParameterNamed("submittedOnNote");
            if (StringUtils.isNotBlank(submittedOnNote)) {
                final Note note = Note.loanNote(newLoanApplication, submittedOnNote);
                this.noteRepository.save(note);
            }

            // Save calendar instance
            final Long calendarId = command.longValueOfParameterNamed("calendarId");
            Calendar calendar = null;

            if (calendarId != null && calendarId != 0) {
                calendar = this.calendarRepository.findById(calendarId).orElseThrow(() -> new CalendarNotFoundException(calendarId));

                final CalendarInstance calendarInstance = new CalendarInstance(calendar, newLoanApplication.getId(),
                        CalendarEntityType.LOANS.getValue());
                this.calendarInstanceRepository.save(calendarInstance);
            } else {
                final LoanApplicationTerms loanApplicationTerms = this.loanScheduleAssembler.assembleLoanTerms(command.parsedJson());
                final Integer repaymentFrequencyNthDayType = command.integerValueOfParameterNamed("repaymentFrequencyNthDayType");
                if (loanApplicationTerms.getRepaymentPeriodFrequencyType() == PeriodFrequencyType.MONTHS
                        && repaymentFrequencyNthDayType != null) {
                    final String title = "loan_schedule_" + newLoanApplication.getId();
                    LocalDate calendarStartDate = loanApplicationTerms.getRepaymentsStartingFromLocalDate();
                    if (calendarStartDate == null) {
                        calendarStartDate = loanApplicationTerms.getExpectedDisbursementDate();
                    }
                    final CalendarFrequencyType calendarFrequencyType = CalendarFrequencyType.MONTHLY;
                    final Integer frequency = loanApplicationTerms.getRepaymentEvery();
                    final Integer repeatsOnDay = loanApplicationTerms.getWeekDayType().getValue();
                    final Integer repeatsOnNthDayOfMonth = loanApplicationTerms.getNthDay();
                    final Integer calendarEntityType = CalendarEntityType.LOANS.getValue();
                    final Calendar loanCalendar = Calendar.createRepeatingCalendar(title, calendarStartDate,
                            CalendarType.COLLECTION.getValue(), calendarFrequencyType, frequency, repeatsOnDay, repeatsOnNthDayOfMonth);
                    this.calendarRepository.save(loanCalendar);
                    final CalendarInstance calendarInstance = CalendarInstance.from(loanCalendar, newLoanApplication.getId(),
                            calendarEntityType);
                    this.calendarInstanceRepository.save(calendarInstance);
                }
            }

            // Save linked account information
            SavingsAccount savingsAccount;
            final boolean backdatedTxnsAllowedTill = false;
            AccountAssociations accountAssociations;
            final Long savingsAccountId = command.longValueOfParameterNamed("linkAccountId");
            if (savingsAccountId != null) {
                if (newLoanApplication.getLoanType().equals(AccountType.GLIM.getValue())) {

                    List<GroupSavingsIndividualMonitoringAccountData> childSavings = (List<GroupSavingsIndividualMonitoringAccountData>) gsimReadPlatformService
                            .findGSIMAccountsByGSIMId(savingsAccountId);
                    // List<SavingsAccountSummaryData>
                    // childSavings=gsimAccount.getChildGSIMAccounts();
                    List<BigDecimal> gsimClientMembers = new ArrayList<BigDecimal>();
                    Map<BigDecimal, BigDecimal> clientAccountMappings = new HashMap<>();
                    for (GroupSavingsIndividualMonitoringAccountData childSaving : childSavings) {
                        gsimClientMembers.add(childSaving.getClientId());
                        clientAccountMappings.put(childSaving.getClientId(), childSaving.getChildAccountId());

                    }

                    if (gsimClientMembers.contains(BigDecimal.valueOf(newLoanApplication.getClientId()))) {
                        savingsAccount = this.savingsAccountAssembler.assembleFrom(
                                clientAccountMappings.get(BigDecimal.valueOf(newLoanApplication.getClientId())).longValue(),
                                backdatedTxnsAllowedTill);

                        this.fromApiJsonDeserializer.validatelinkedSavingsAccount(savingsAccount, newLoanApplication);
                        boolean isActive = true;
                        accountAssociations = AccountAssociations.associateSavingsAccount(newLoanApplication, savingsAccount,
                                AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue(), isActive);
                        this.accountAssociationsRepository.save(accountAssociations);

                    } else {
                        throw new GroupMemberNotFoundInGSIMException(newLoanApplication.getClientId());
                    }
                } else {

                    savingsAccount = this.savingsAccountAssembler.assembleFrom(savingsAccountId, backdatedTxnsAllowedTill);
                    this.fromApiJsonDeserializer.validatelinkedSavingsAccount(savingsAccount, newLoanApplication);
                    boolean isActive = true;
                    accountAssociations = AccountAssociations.associateSavingsAccount(newLoanApplication, savingsAccount,
                            AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue(), isActive);
                    this.accountAssociationsRepository.save(accountAssociations);

                }

                // Save linked vendor savings account information for bnpl loan
                SavingsAccount vendorSavingsAccount;
                AccountAssociations vendorAccountAssociations;
                final Long vendorSavingsAccountId = command.longValueOfParameterNamed("linkVendorAccountId");
                if (vendorSavingsAccountId != null) {
                    vendorSavingsAccount = this.savingsAccountAssembler.assembleFrom(vendorSavingsAccountId, backdatedTxnsAllowedTill);
                    this.fromApiJsonDeserializer.validateLinkedVendorSavingsAccountForBnplLoan(vendorSavingsAccount, savingsAccount);
                    boolean isActive = true;
                    vendorAccountAssociations = AccountAssociations.associateSavingsAccount(newLoanApplication, vendorSavingsAccount,
                            AccountAssociationType.BNPL_LINKED_ACCOUNT_ASSOCIATION.getValue(), isActive);
                    this.accountAssociationsRepository.save(vendorAccountAssociations);
                }
            }

            if (command.parameterExists(LoanApiConstants.datatables)) {
                this.entityDatatableChecksWritePlatformService.saveDatatables(StatusEnum.CREATE.getCode().longValue(),
                        EntityTables.LOAN.getName(), newLoanApplication.getId(), newLoanApplication.productId(),
                        command.arrayOfParameterNamed(LoanApiConstants.datatables));
            }

            loanRepositoryWrapper.flush();

            this.entityDatatableChecksWritePlatformService.runTheCheckForProduct(newLoanApplication.getId(), EntityTables.LOAN.getName(),
                    StatusEnum.CREATE.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(),
                    newLoanApplication.productId());

            businessEventNotifierService.notifyPostBusinessEvent(new LoanCreatedBusinessEvent(newLoanApplication));

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(newLoanApplication.getId()) //
                    .withOfficeId(newLoanApplication.getOfficeId()) //
                    .withClientId(newLoanApplication.getClientId()) //
                    .withGroupId(newLoanApplication.getGroupId()) //
                    .withLoanId(newLoanApplication.getId()).withGlimId(newLoanApplication.getGlimId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    public void checkForProductMixRestrictions(final Loan loan) {

        final List<Long> activeLoansLoanProductIds;
        final Long productId = loan.loanProduct().getId();

        if (loan.isGroupLoan()) {
            activeLoansLoanProductIds = this.loanRepositoryWrapper.findActiveLoansLoanProductIdsByGroup(loan.getGroupId(),
                    LoanStatus.ACTIVE.getValue());
        } else {
            activeLoansLoanProductIds = this.loanRepositoryWrapper.findActiveLoansLoanProductIdsByClient(loan.getClientId(),
                    LoanStatus.ACTIVE.getValue());
        }
        checkForProductMixRestrictions(activeLoansLoanProductIds, productId, loan.loanProduct().productName());
    }

    private void checkForProductMixRestrictions(final List<Long> activeLoansLoanProductIds, final Long productId,
            final String productName) {

        if (!CollectionUtils.isEmpty(activeLoansLoanProductIds)) {
            final Collection<LoanProductData> restrictedPrdouctsList = this.loanProductReadPlatformService
                    .retrieveRestrictedProductsForMix(productId);
            for (final LoanProductData restrictedProduct : restrictedPrdouctsList) {
                if (activeLoansLoanProductIds.contains(restrictedProduct.getId())) {
                    throw new GeneralPlatformDomainRuleException(
                            "error.msg.loan.applied.or.to.be.disbursed.can.not.co-exist.with.the.loan.already.active.to.this.client",
                            "This loan could not be applied/disbursed as the loan and `" + restrictedProduct
                                    + "` are not allowed to co-exist");
                }
            }
        }
    }

    private void updateProductRelatedDetails(LoanProductRelatedDetail productRelatedDetail, Loan loan) {
        final Boolean amortization = loan.loanProduct().getLoanProductConfigurableAttributes().getAmortizationBoolean();
        final Boolean arrearsTolerance = loan.loanProduct().getLoanProductConfigurableAttributes().getArrearsToleranceBoolean();
        final Boolean graceOnArrearsAging = loan.loanProduct().getLoanProductConfigurableAttributes().getGraceOnArrearsAgingBoolean();
        final Boolean interestCalcPeriod = loan.loanProduct().getLoanProductConfigurableAttributes().getInterestCalcPeriodBoolean();
        final Boolean interestMethod = loan.loanProduct().getLoanProductConfigurableAttributes().getInterestMethodBoolean();
        final Boolean graceOnPrincipalAndInterestPayment = loan.loanProduct().getLoanProductConfigurableAttributes()
                .getGraceOnPrincipalAndInterestPaymentBoolean();
        final Boolean repaymentEvery = loan.loanProduct().getLoanProductConfigurableAttributes().getRepaymentEveryBoolean();
        final Boolean transactionProcessingStrategy = loan.loanProduct().getLoanProductConfigurableAttributes()
                .getTransactionProcessingStrategyBoolean();

        if (!amortization) {
            productRelatedDetail.setAmortizationMethod(loan.loanProduct().getLoanProductRelatedDetail().getAmortizationMethod());
        }
        if (!arrearsTolerance) {
            productRelatedDetail.setInArrearsTolerance(loan.loanProduct().getLoanProductRelatedDetail().getArrearsTolerance());
        }
        if (!graceOnArrearsAging) {
            productRelatedDetail.setGraceOnArrearsAgeing(loan.loanProduct().getLoanProductRelatedDetail().getGraceOnArrearsAgeing());
        }
        if (!interestCalcPeriod) {
            productRelatedDetail.setInterestCalculationPeriodMethod(
                    loan.loanProduct().getLoanProductRelatedDetail().getInterestCalculationPeriodMethod());
        }
        if (!interestMethod) {
            productRelatedDetail.setInterestMethod(loan.loanProduct().getLoanProductRelatedDetail().getInterestMethod());
        }
        if (!graceOnPrincipalAndInterestPayment) {
            productRelatedDetail.setGraceOnInterestPayment(loan.loanProduct().getLoanProductRelatedDetail().getGraceOnInterestPayment());
            productRelatedDetail.setGraceOnPrincipalPayment(loan.loanProduct().getLoanProductRelatedDetail().getGraceOnPrincipalPayment());
        }
        if (!repaymentEvery) {
            productRelatedDetail.setRepayEvery(loan.loanProduct().getLoanProductRelatedDetail().getRepayEvery());
        }
        if (!transactionProcessingStrategy) {
            loan.updateTransactionProcessingStrategy(loan.loanProduct().getRepaymentStrategy());
        }
    }

    private void createAndPersistCalendarInstanceForInterestRecalculation(final Loan loan) {

        LocalDate calendarStartDate = loan.getExpectedDisbursedOnLocalDate();
        Integer repeatsOnDay = null;
        final RecalculationFrequencyType recalculationFrequencyType = loan.loanInterestRecalculationDetails().getRestFrequencyType();
        Integer recalculationFrequencyNthDay = loan.loanInterestRecalculationDetails().getRestFrequencyOnDay();
        if (recalculationFrequencyNthDay == null) {
            recalculationFrequencyNthDay = loan.loanInterestRecalculationDetails().getRestFrequencyNthDay();
            repeatsOnDay = loan.loanInterestRecalculationDetails().getRestFrequencyWeekday();
        }

        Integer frequency = loan.loanInterestRecalculationDetails().getRestInterval();
        CalendarEntityType calendarEntityType = CalendarEntityType.LOAN_RECALCULATION_REST_DETAIL;
        final String title = "loan_recalculation_detail_" + loan.loanInterestRecalculationDetails().getId();

        createCalendar(loan, calendarStartDate, recalculationFrequencyNthDay, repeatsOnDay, recalculationFrequencyType, frequency,
                calendarEntityType, title);

        if (loan.loanInterestRecalculationDetails().getInterestRecalculationCompoundingMethod().isCompoundingEnabled()) {
            LocalDate compoundingStartDate = loan.getExpectedDisbursedOnLocalDate();
            Integer compoundingRepeatsOnDay = null;
            final RecalculationFrequencyType recalculationCompoundingFrequencyType = loan.loanInterestRecalculationDetails()
                    .getCompoundingFrequencyType();
            Integer recalculationCompoundingFrequencyNthDay = loan.loanInterestRecalculationDetails().getCompoundingFrequencyOnDay();
            if (recalculationCompoundingFrequencyNthDay == null) {
                recalculationCompoundingFrequencyNthDay = loan.loanInterestRecalculationDetails().getCompoundingFrequencyNthDay();
                compoundingRepeatsOnDay = loan.loanInterestRecalculationDetails().getCompoundingFrequencyWeekday();
            }

            Integer compoundingFrequency = loan.loanInterestRecalculationDetails().getCompoundingInterval();
            CalendarEntityType compoundingCalendarEntityType = CalendarEntityType.LOAN_RECALCULATION_COMPOUNDING_DETAIL;
            final String compoundingCalendarTitle = "loan_recalculation_detail_compounding_frequency"
                    + loan.loanInterestRecalculationDetails().getId();

            createCalendar(loan, compoundingStartDate, recalculationCompoundingFrequencyNthDay, compoundingRepeatsOnDay,
                    recalculationCompoundingFrequencyType, compoundingFrequency, compoundingCalendarEntityType, compoundingCalendarTitle);
        }

    }

    private void createCalendar(final Loan loan, LocalDate calendarStartDate, Integer recalculationFrequencyNthDay,
            final Integer repeatsOnDay, final RecalculationFrequencyType recalculationFrequencyType, Integer frequency,
            CalendarEntityType calendarEntityType, final String title) {
        CalendarFrequencyType calendarFrequencyType = CalendarFrequencyType.INVALID;
        Integer updatedRepeatsOnDay = repeatsOnDay;
        switch (recalculationFrequencyType) {
            case DAILY:
                calendarFrequencyType = CalendarFrequencyType.DAILY;
            break;
            case MONTHLY:
                calendarFrequencyType = CalendarFrequencyType.MONTHLY;
            break;
            case SAME_AS_REPAYMENT_PERIOD:
                frequency = loan.repaymentScheduleDetail().getRepayEvery();
                calendarFrequencyType = CalendarFrequencyType.from(loan.repaymentScheduleDetail().getRepaymentPeriodFrequencyType());
                calendarStartDate = loan.getExpectedDisbursedOnLocalDate();
                if (updatedRepeatsOnDay == null) {
                    updatedRepeatsOnDay = calendarStartDate.get(ChronoField.DAY_OF_WEEK);
                }
            break;
            case WEEKLY:
                calendarFrequencyType = CalendarFrequencyType.WEEKLY;
            break;
            default:
            break;
        }

        final Calendar calendar = Calendar.createRepeatingCalendar(title, calendarStartDate, CalendarType.COLLECTION.getValue(),
                calendarFrequencyType, frequency, updatedRepeatsOnDay, recalculationFrequencyNthDay);
        final CalendarInstance calendarInstance = CalendarInstance.from(calendar, loan.loanInterestRecalculationDetails().getId(),
                calendarEntityType.getValue());
        this.calendarInstanceRepository.save(calendarInstance);
    }

    @Transactional
    @Override
    public CommandProcessingResult modifyApplication(final Long loanId, final JsonCommand command) {

        try {
            AppUser currentUser = getAppUserIfPresent();
            final Loan existingLoanApplication = retrieveLoanBy(loanId);
            if (!existingLoanApplication.isSubmittedAndPendingApproval()) {
                throw new LoanApplicationNotInSubmittedAndPendingApprovalStateCannotBeModified(loanId);
            }

            final String productIdParamName = "productId";
            LoanProduct newLoanProduct = null;
            if (command.isChangeInLongParameterNamed(productIdParamName, existingLoanApplication.loanProduct().getId())) {
                final Long productId = command.longValueOfParameterNamed(productIdParamName);
                newLoanProduct = this.loanProductRepository.findById(productId)
                        .orElseThrow(() -> new LoanProductNotFoundException(productId));
            }

            LoanProduct loanProductForValidations = newLoanProduct == null ? existingLoanApplication.loanProduct() : newLoanProduct;

            this.fromApiJsonDeserializer.validateForModify(command.json(), loanProductForValidations, existingLoanApplication);

            checkClientOrGroupActive(existingLoanApplication);

            final Set<LoanCharge> existingCharges = existingLoanApplication.charges();
            Map<Long, LoanChargeData> chargesMap = new HashMap<>();
            for (LoanCharge charge : existingCharges) {
                LoanChargeData chargeData = new LoanChargeData(charge.getId(), charge.getDueLocalDate(), charge.amountOrPercentage());
                chargesMap.put(charge.getId(), chargeData);
            }
            List<LoanDisbursementDetails> disbursementDetails = this.loanUtilService
                    .fetchDisbursementData(command.parsedJson().getAsJsonObject());

            /**
             * Stores all charges which are passed in during modify loan application
             **/
            final Set<LoanCharge> possiblyModifedLoanCharges = this.loanChargeAssembler.fromParsedJson(command.parsedJson(),
                    disbursementDetails);
            /** Boolean determines if any charge has been modified **/
            boolean isChargeModified = false;

            Set<Charge> newTrancheChages = this.loanChargeAssembler.getNewLoanTrancheCharges(command.parsedJson());
            for (Charge charge : newTrancheChages) {
                existingLoanApplication.addTrancheLoanCharge(charge);
            }

            /**
             * If there are any charges already present, which are now not passed in as a part of the request, deem the
             * charges as modified
             **/
            if (!possiblyModifedLoanCharges.isEmpty()) {
                if (!possiblyModifedLoanCharges.containsAll(existingCharges)) {
                    isChargeModified = true;
                }
            }

            /**
             * If any new charges are added or values of existing charges are modified
             **/
            for (LoanCharge loanCharge : possiblyModifedLoanCharges) {
                if (loanCharge.getId() == null) {
                    isChargeModified = true;
                } else {
                    LoanChargeData chargeData = chargesMap.get(loanCharge.getId());
                    if (loanCharge.amountOrPercentage().compareTo(chargeData.amountOrPercentage()) != 0
                            || ((loanCharge.isSpecifiedDueDate() || loanCharge.isDisburseToSavings())
                                    && !loanCharge.getDueLocalDate().equals(chargeData.getDueDate()))) {
                        isChargeModified = true;
                    }
                }
            }

            Set<LoanCollateralManagement> possiblyModifedLoanCollateralItems = null;

            if (command.parameterExists("loanType")) {
                final String loanTypeStr = command.stringValueOfParameterNamed("loanType");
                final AccountType loanType = AccountType.fromName(loanTypeStr);

                if (!StringUtils.isBlank(loanTypeStr) && loanType.isIndividualAccount()) {
                    possiblyModifedLoanCollateralItems = this.loanCollateralAssembler.fromParsedJson(command.parsedJson());
                }
            }

            final Map<String, Object> changes = existingLoanApplication.loanApplicationModification(command, possiblyModifedLoanCharges,
                    possiblyModifedLoanCollateralItems, this.aprCalculator, isChargeModified, loanProductForValidations);

            if (changes.containsKey("expectedDisbursementDate")) {
                this.loanAssembler.validateExpectedDisbursementForHolidayAndNonWorkingDay(existingLoanApplication);
            }

            final String clientIdParamName = "clientId";
            if (changes.containsKey(clientIdParamName)) {
                final Long clientId = command.longValueOfParameterNamed(clientIdParamName);
                final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
                if (client.isNotActive()) {
                    throw new ClientNotActiveException(clientId);
                }

                existingLoanApplication.updateClient(client);
            }

            final String groupIdParamName = "groupId";
            if (changes.containsKey(groupIdParamName)) {
                final Long groupId = command.longValueOfParameterNamed(groupIdParamName);
                final Group group = this.groupRepository.findOneWithNotFoundDetection(groupId);
                if (group.isNotActive()) {
                    throw new GroupNotActiveException(groupId);
                }

                existingLoanApplication.updateGroup(group);
            }

            if (newLoanProduct != null) {
                existingLoanApplication.updateLoanProduct(newLoanProduct);
                if (!changes.containsKey("interestRateFrequencyType")) {
                    existingLoanApplication.updateInterestRateFrequencyType();
                }
                final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
                final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
                if (newLoanProduct.useBorrowerCycle()) {
                    final Long clientId = this.fromJsonHelper.extractLongNamed("clientId", command.parsedJson());
                    final Long groupId = this.fromJsonHelper.extractLongNamed("groupId", command.parsedJson());
                    Integer cycleNumber = 0;
                    if (clientId != null) {
                        cycleNumber = this.loanReadPlatformService.retriveLoanCounter(clientId, newLoanProduct.getId());
                    } else if (groupId != null) {
                        cycleNumber = this.loanReadPlatformService.retriveLoanCounter(groupId, AccountType.GROUP.getValue(),
                                newLoanProduct.getId());
                    }
                    this.loanProductCommandFromApiJsonDeserializer.validateMinMaxConstraints(command.parsedJson(), baseDataValidator,
                            newLoanProduct, cycleNumber);
                } else {
                    this.loanProductCommandFromApiJsonDeserializer.validateMinMaxConstraints(command.parsedJson(), baseDataValidator,
                            newLoanProduct);
                }
                if (newLoanProduct.isLinkedToFloatingInterestRate()) {
                    existingLoanApplication.getLoanProductRelatedDetail().updateForFloatingInterestRates();
                } else {
                    existingLoanApplication.setInterestRateDifferential(null);
                    existingLoanApplication.setIsFloatingInterestRate(null);
                }
                if (!dataValidationErrors.isEmpty()) {
                    throw new PlatformApiDataValidationException(dataValidationErrors);
                }
            }

            existingLoanApplication.updateIsInterestRecalculationEnabled();
            validateSubmittedOnDate(existingLoanApplication);

            final LoanProductRelatedDetail productRelatedDetail = existingLoanApplication.repaymentScheduleDetail();
            if (existingLoanApplication.loanProduct().getLoanProductConfigurableAttributes() != null) {
                updateProductRelatedDetails(productRelatedDetail, existingLoanApplication);
            }

            if (existingLoanApplication.getLoanProduct().canUseForTopup() && existingLoanApplication.getClientId() != null) {
                final Boolean isTopup = command.booleanObjectValueOfParameterNamed(LoanApiConstants.isTopup);
                if (command.isChangeInBooleanParameterNamed(LoanApiConstants.isTopup, existingLoanApplication.isTopup())) {
                    existingLoanApplication.setIsTopup(isTopup);
                    changes.put(LoanApiConstants.isTopup, isTopup);
                }

                if (existingLoanApplication.isTopup()) {
                    final Long loanIdToClose = command.longValueOfParameterNamed(LoanApiConstants.loanIdToClose);
                    LoanTopupDetails existingLoanTopupDetails = existingLoanApplication.getTopupLoanDetails();
                    if (existingLoanTopupDetails == null
                            || (existingLoanTopupDetails != null && !existingLoanTopupDetails.getLoanIdToClose().equals(loanIdToClose))
                            || changes.containsKey("submittedOnDate") || changes.containsKey("expectedDisbursementDate")
                            || changes.containsKey("principal") || changes.containsKey(LoanApiConstants.disbursementDataParameterName)) {
                        Long existingLoanIdToClose = null;
                        if (existingLoanTopupDetails != null) {
                            existingLoanIdToClose = existingLoanTopupDetails.getLoanIdToClose();
                        }
                        final Loan loanToClose = this.loanRepositoryWrapper.findNonClosedLoanThatBelongsToClient(loanIdToClose,
                                existingLoanApplication.getClientId());
                        if (loanToClose == null) {
                            throw new GeneralPlatformDomainRuleException(
                                    "error.msg.loan.loanIdToClose.no.active.loan.associated.to.client.found",
                                    "loanIdToClose is invalid, No Active Loan associated with the given Client ID found.");
                        }
                        if (loanToClose.isMultiDisburmentLoan() && !loanToClose.isInterestRecalculationEnabledForProduct()) {
                            throw new GeneralPlatformDomainRuleException(
                                    "error.msg.loan.topup.on.multi.tranche.loan.without.interest.recalculation.not.supported",
                                    "Topup on loan with multi-tranche disbursal and without interest recalculation is not supported.");
                        }
                        final LocalDate disbursalDateOfLoanToClose = loanToClose.getDisbursementDate();
                        if (!existingLoanApplication.getSubmittedOnDate().isAfter(disbursalDateOfLoanToClose)) {
                            throw new GeneralPlatformDomainRuleException(
                                    "error.msg.loan.submitted.date.should.be.after.topup.loan.disbursal.date",
                                    "Submitted date of this loan application " + existingLoanApplication.getSubmittedOnDate()
                                            + " should be after the disbursed date of loan to be closed " + disbursalDateOfLoanToClose);
                        }
                        if (!loanToClose.getCurrencyCode().equals(existingLoanApplication.getCurrencyCode())) {
                            throw new GeneralPlatformDomainRuleException("error.msg.loan.to.be.closed.has.different.currency",
                                    "loanIdToClose is invalid, Currency code is different.");
                        }
                        final LocalDate lastUserTransactionOnLoanToClose = loanToClose.getLastUserTransactionDate();
                        if (existingLoanApplication.getDisbursementDate().isBefore(lastUserTransactionOnLoanToClose)) {
                            throw new GeneralPlatformDomainRuleException(
                                    "error.msg.loan.disbursal.date.should.be.after.last.transaction.date.of.loan.to.be.closed",
                                    "Disbursal date of this loan application " + existingLoanApplication.getDisbursementDate()
                                            + " should be after last transaction date of loan to be closed "
                                            + lastUserTransactionOnLoanToClose);
                        }
                        BigDecimal loanOutstanding = this.loanReadPlatformService
                                .retrieveLoanForeclosureTemplate(loanIdToClose, existingLoanApplication.getDisbursementDate()).getAmount();
                        final BigDecimal firstDisbursalAmount = existingLoanApplication.getFirstDisbursalAmount();
                        if (loanOutstanding.compareTo(firstDisbursalAmount) > 0) {
                            throw new GeneralPlatformDomainRuleException("error.msg.loan.amount.less.than.outstanding.of.loan.to.be.closed",
                                    "Topup loan amount should be greater than outstanding amount of loan to be closed.");
                        }

                        if (!existingLoanIdToClose.equals(loanIdToClose)) {
                            final LoanTopupDetails topupDetails = new LoanTopupDetails(existingLoanApplication, loanIdToClose);
                            existingLoanApplication.setTopupLoanDetails(topupDetails);
                            changes.put(LoanApiConstants.loanIdToClose, loanIdToClose);
                        }
                    }
                } else {
                    existingLoanApplication.setTopupLoanDetails(null);
                }
            } else {
                if (existingLoanApplication.isTopup()) {
                    existingLoanApplication.setIsTopup(false);
                    existingLoanApplication.setTopupLoanDetails(null);
                    changes.put(LoanApiConstants.isTopup, false);
                }
            }

            final String fundIdParamName = "fundId";
            if (changes.containsKey(fundIdParamName)) {
                final Long fundId = command.longValueOfParameterNamed(fundIdParamName);
                final Fund fund = this.loanAssembler.findFundByIdIfProvided(fundId);

                existingLoanApplication.updateFund(fund);
            }

            final String loanPurposeIdParamName = "loanPurposeId";
            if (changes.containsKey(loanPurposeIdParamName)) {
                final Long loanPurposeId = command.longValueOfParameterNamed(loanPurposeIdParamName);
                final CodeValue loanPurpose = this.loanAssembler.findCodeValueByIdIfProvided(loanPurposeId);
                existingLoanApplication.updateLoanPurpose(loanPurpose);
            }

            if (changes.containsKey(LoanApiConstants.DEPARTMENT_PARAM)) {
                final Long departmentId = command.longValueOfParameterNamed(LoanApiConstants.DEPARTMENT_PARAM);
                final CodeValue department = this.loanAssembler.findCodeValueByIdIfProvided(departmentId);
                existingLoanApplication.updateDepartment(department);
            }

            final String loanOfficerIdParamName = "loanOfficerId";
            if (changes.containsKey(loanOfficerIdParamName)) {
                final Long loanOfficerId = command.longValueOfParameterNamed(loanOfficerIdParamName);
                final Staff newValue = this.loanAssembler.findLoanOfficerByIdIfProvided(loanOfficerId);
                existingLoanApplication.updateLoanOfficerOnLoanApplication(newValue);
            }

            final String strategyIdParamName = "transactionProcessingStrategyId";
            if (changes.containsKey(strategyIdParamName)) {
                final Long strategyId = command.longValueOfParameterNamed(strategyIdParamName);
                final LoanTransactionProcessingStrategy strategy = this.loanAssembler.findStrategyByIdIfProvided(strategyId);

                existingLoanApplication.updateTransactionProcessingStrategy(strategy);
            }

            /**
             * TODO: Allow other loan types if needed.
             */
            if (command.parameterExists("loanType")) {
                final String loanTypeStr = command.stringValueOfParameterNamed("loanType");
                final AccountType loanType = AccountType.fromName(loanTypeStr);

                if (!StringUtils.isBlank(loanTypeStr) && loanType.isIndividualAccount()) {
                    final String collateralParamName = "collateral";
                    if (changes.containsKey(collateralParamName)) {
                        existingLoanApplication.updateLoanCollateral(possiblyModifedLoanCollateralItems);
                    }
                }
            }

            final String chargesParamName = "charges";
            if (changes.containsKey(chargesParamName)) {
                existingLoanApplication.updateLoanCharges(possiblyModifedLoanCharges);
            }

            if (changes.containsKey("recalculateLoanSchedule")) {
                changes.remove("recalculateLoanSchedule");

                final JsonElement parsedQuery = this.fromJsonHelper.parse(command.json());
                final JsonQuery query = JsonQuery.from(command.json(), parsedQuery, this.fromJsonHelper);

                final LoanScheduleModel loanSchedule = this.calculationPlatformService.calculateLoanSchedule(query, false);
                existingLoanApplication.updateLoanSchedule(loanSchedule);
                existingLoanApplication.recalculateAllCharges();
            }

            // Changes to modify loan rates.
            if (command.hasParameter(LoanProductConstants.RATES_PARAM_NAME)) {
                existingLoanApplication.updateLoanRates(rateAssembler.fromParsedJson(command.parsedJson()));
            }

            final Integer numberOfRepaymentsToCarryForward = command
                    .integerValueOfParameterNamedDefaultToNullIfZero(LoanApiConstants.NUMBER_OF_REPAYMENT_TO_CARRY_FORWARD);

            this.fromApiJsonDeserializer.validateLoanTermAndRepaidEveryValues(existingLoanApplication.getTermFrequency(),
                    existingLoanApplication.getTermPeriodFrequencyType(), productRelatedDetail.getNumberOfRepayments(),
                    productRelatedDetail.getRepayEvery(), productRelatedDetail.getRepaymentPeriodFrequencyType().getValue(),
                    existingLoanApplication, numberOfRepaymentsToCarryForward);

            saveAndFlushLoanWithDataIntegrityViolationChecks(existingLoanApplication);

            final String submittedOnNote = command.stringValueOfParameterNamed("submittedOnNote");
            if (StringUtils.isNotBlank(submittedOnNote)) {
                final Note note = Note.loanNote(existingLoanApplication, submittedOnNote);
                this.noteRepository.save(note);
            }

            final Long calendarId = command.longValueOfParameterNamed("calendarId");
            Calendar calendar = null;
            if (calendarId != null && calendarId != 0) {
                calendar = this.calendarRepository.findById(calendarId).orElseThrow(() -> new CalendarNotFoundException(calendarId));
            }

            final List<CalendarInstance> ciList = (List<CalendarInstance>) this.calendarInstanceRepository
                    .findByEntityIdAndEntityTypeId(loanId, CalendarEntityType.LOANS.getValue());
            if (calendar != null) {

                // For loans, allow to attach only one calendar instance per
                // loan
                if (ciList != null && !ciList.isEmpty()) {
                    final CalendarInstance calendarInstance = ciList.get(0);
                    final boolean isCalendarAssociatedWithEntity = this.calendarReadPlatformService.isCalendarAssociatedWithEntity(
                            calendarInstance.getEntityId(), calendarInstance.getCalendar().getId(),
                            CalendarEntityType.LOANS.getValue().longValue());
                    if (isCalendarAssociatedWithEntity && calendarId == null) {
                        this.calendarRepository.delete(calendarInstance.getCalendar());
                    }
                    if (!calendarInstance.getCalendar().getId().equals(calendar.getId())) {
                        calendarInstance.updateCalendar(calendar);
                        this.calendarInstanceRepository.saveAndFlush(calendarInstance);
                    }
                } else {
                    // attaching new calendar
                    final CalendarInstance calendarInstance = new CalendarInstance(calendar, existingLoanApplication.getId(),
                            CalendarEntityType.LOANS.getValue());
                    this.calendarInstanceRepository.save(calendarInstance);
                }

            } else {
                if (ciList != null && !ciList.isEmpty()) {
                    final CalendarInstance existingCalendarInstance = ciList.get(0);
                    final boolean isCalendarAssociatedWithEntity = this.calendarReadPlatformService.isCalendarAssociatedWithEntity(
                            existingCalendarInstance.getEntityId(), existingCalendarInstance.getCalendar().getId(),
                            CalendarEntityType.GROUPS.getValue().longValue());
                    if (isCalendarAssociatedWithEntity) {
                        this.calendarInstanceRepository.delete(existingCalendarInstance);
                    }
                }
                if (changes.containsKey("repaymentFrequencyNthDayType") || changes.containsKey("repaymentFrequencyDayOfWeekType")) {
                    if (changes.get("repaymentFrequencyNthDayType") == null) {
                        if (ciList != null && !ciList.isEmpty()) {
                            final CalendarInstance calendarInstance = ciList.get(0);
                            final boolean isCalendarAssociatedWithEntity = this.calendarReadPlatformService.isCalendarAssociatedWithEntity(
                                    calendarInstance.getEntityId(), calendarInstance.getCalendar().getId(),
                                    CalendarEntityType.LOANS.getValue().longValue());
                            if (isCalendarAssociatedWithEntity) {
                                this.calendarInstanceRepository.delete(calendarInstance);
                                this.calendarRepository.delete(calendarInstance.getCalendar());
                            }
                        }
                    } else {
                        Integer repaymentFrequencyTypeInt = command.integerValueOfParameterNamed("repaymentFrequencyType");
                        if (repaymentFrequencyTypeInt != null) {
                            if (PeriodFrequencyType.fromInt(repaymentFrequencyTypeInt) == PeriodFrequencyType.MONTHS) {
                                final String title = "loan_schedule_" + existingLoanApplication.getId();
                                final Integer typeId = CalendarType.COLLECTION.getValue();
                                final CalendarFrequencyType repaymentFrequencyType = CalendarFrequencyType.MONTHLY;
                                final Integer interval = command.integerValueOfParameterNamed("repaymentEvery");
                                LocalDate startDate = command.localDateValueOfParameterNamed("repaymentsStartingFromDate");
                                if (startDate == null) {
                                    startDate = command.localDateValueOfParameterNamed("expectedDisbursementDate");
                                }
                                final Calendar newCalendar = Calendar.createRepeatingCalendar(title, startDate, typeId,
                                        repaymentFrequencyType, interval, (Integer) changes.get("repaymentFrequencyDayOfWeekType"),
                                        (Integer) changes.get("repaymentFrequencyNthDayType"));
                                if (ciList != null && !ciList.isEmpty()) {
                                    final CalendarInstance calendarInstance = ciList.get(0);
                                    final boolean isCalendarAssociatedWithEntity = this.calendarReadPlatformService
                                            .isCalendarAssociatedWithEntity(calendarInstance.getEntityId(),
                                                    calendarInstance.getCalendar().getId(),
                                                    CalendarEntityType.LOANS.getValue().longValue());
                                    if (isCalendarAssociatedWithEntity) {
                                        final Calendar existingCalendar = calendarInstance.getCalendar();
                                        if (existingCalendar != null) {
                                            String existingRecurrence = existingCalendar.getRecurrence();
                                            if (!existingRecurrence.equals(newCalendar.getRecurrence())) {
                                                existingCalendar.setRecurrence(newCalendar.getRecurrence());
                                                this.calendarRepository.save(existingCalendar);
                                            }
                                        }
                                    }
                                } else {
                                    this.calendarRepository.save(newCalendar);
                                    final Integer calendarEntityType = CalendarEntityType.LOANS.getValue();
                                    final CalendarInstance calendarInstance = new CalendarInstance(newCalendar,
                                            existingLoanApplication.getId(), calendarEntityType);
                                    this.calendarInstanceRepository.save(calendarInstance);
                                }
                            }
                        }
                    }
                }
            }

            // Save linked account information
            final String linkAccountIdParamName = "linkAccountId";
            final boolean backdatedTxnsAllowedTill = false;
            final Long savingsAccountId = command.longValueOfParameterNamed(linkAccountIdParamName);
            AccountAssociations accountAssociations = this.accountAssociationsRepository.findByLoanIdAndType(loanId,
                    AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue());
            boolean isLinkedAccPresent = false;
            SavingsAccount customerSavingsAccount = accountAssociations != null ? accountAssociations.linkedSavingsAccount() : null;
            if (savingsAccountId == null) {
                if (accountAssociations != null) {
                    if (this.fromJsonHelper.parameterExists(linkAccountIdParamName, command.parsedJson())) {
                        this.accountAssociationsRepository.delete(accountAssociations);
                        changes.put(linkAccountIdParamName, null);
                        customerSavingsAccount = null;
                    } else {
                        isLinkedAccPresent = true;
                    }
                }
            } else {
                isLinkedAccPresent = true;
                boolean isModified = false;
                if (accountAssociations == null) {
                    isModified = true;
                } else {
                    final SavingsAccount savingsAccount = accountAssociations.linkedSavingsAccount();
                    customerSavingsAccount = savingsAccount;
                    if (savingsAccount == null || !savingsAccount.getId().equals(savingsAccountId)) {
                        isModified = true;
                    }
                }
                if (isModified) {
                    final SavingsAccount savingsAccount = this.savingsAccountAssembler.assembleFrom(savingsAccountId,
                            backdatedTxnsAllowedTill);
                    customerSavingsAccount = savingsAccount;
                    this.fromApiJsonDeserializer.validatelinkedSavingsAccount(savingsAccount, existingLoanApplication);
                    if (accountAssociations == null) {
                        boolean isActive = true;
                        accountAssociations = AccountAssociations.associateSavingsAccount(existingLoanApplication, savingsAccount,
                                AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue(), isActive);
                    } else {
                        accountAssociations.updateLinkedSavingsAccount(savingsAccount);
                    }
                    changes.put(linkAccountIdParamName, savingsAccountId);
                    this.accountAssociationsRepository.save(accountAssociations);
                }
            }

            // Save vendor linked account information
            final Long vendorSavingsAccountId = command.longValueOfParameterNamed(LoanApiConstants.linkVendorAccountIdParamName);
            AccountAssociations vendorAccountAssociations = this.accountAssociationsRepository.findByLoanIdAndType(loanId,
                    AccountAssociationType.BNPL_LINKED_ACCOUNT_ASSOCIATION.getValue());
            if (vendorSavingsAccountId == null) {
                if (vendorAccountAssociations != null) {
                    if (this.fromJsonHelper.parameterExists(LoanApiConstants.linkVendorAccountIdParamName, command.parsedJson())) {
                        this.accountAssociationsRepository.delete(vendorAccountAssociations);
                        changes.put(LoanApiConstants.linkVendorAccountIdParamName, null);
                    }
                }
            } else {
                boolean isModified = false;
                if (vendorAccountAssociations == null) {
                    isModified = true;
                } else {
                    final SavingsAccount vendorSavingsAccount = vendorAccountAssociations.linkedSavingsAccount();
                    if (vendorSavingsAccount == null || !vendorSavingsAccount.getId().equals(vendorSavingsAccountId)) {
                        isModified = true;
                    }
                }
                if (isModified) {
                    final SavingsAccount vendorSavingsAccount = this.savingsAccountAssembler.assembleFrom(vendorSavingsAccountId,
                            backdatedTxnsAllowedTill);
                    this.fromApiJsonDeserializer.validateLinkedVendorSavingsAccountForBnplLoan(vendorSavingsAccount,
                            customerSavingsAccount);
                    if (vendorAccountAssociations == null) {
                        boolean isActive = true;
                        vendorAccountAssociations = AccountAssociations.associateSavingsAccount(existingLoanApplication,
                                vendorSavingsAccount, AccountAssociationType.BNPL_LINKED_ACCOUNT_ASSOCIATION.getValue(), isActive);
                    } else {
                        vendorAccountAssociations.updateLinkedSavingsAccount(vendorSavingsAccount);
                    }
                    changes.put(LoanApiConstants.linkVendorAccountIdParamName, vendorSavingsAccountId);
                    this.accountAssociationsRepository.save(vendorAccountAssociations);
                }
            }
            //

            if (!isLinkedAccPresent) {
                final Set<LoanCharge> charges = existingLoanApplication.charges();
                for (final LoanCharge loanCharge : charges) {
                    if (loanCharge.getChargePaymentMode().isPaymentModeAccountTransfer()) {
                        final String errorMessage = "one of the charges requires linked savings account for payment";
                        throw new LinkedAccountRequiredException("loanCharge", errorMessage);
                    }
                }
            }

            if ((command.longValueOfParameterNamed(productIdParamName) != null)
                    || (command.longValueOfParameterNamed(clientIdParamName) != null)
                    || (command.longValueOfParameterNamed(groupIdParamName) != null)) {
                Long OfficeId = null;
                if (existingLoanApplication.getClient() != null) {
                    OfficeId = existingLoanApplication.getClient().getOffice().getId();
                } else if (existingLoanApplication.getGroup() != null) {
                    OfficeId = existingLoanApplication.getGroup().getOffice().getId();
                }
                officeSpecificLoanProductValidation(existingLoanApplication.getLoanProduct().getId(), OfficeId);
            }

            // updating loan interest recalculation details throwing null
            // pointer exception after saveAndFlush
            // http://stackoverflow.com/questions/17151757/hibernate-cascade-update-gives-null-pointer/17334374#17334374
            this.loanRepositoryWrapper.saveAndFlush(existingLoanApplication);

            if (productRelatedDetail.isInterestRecalculationEnabled()) {
                this.fromApiJsonDeserializer.validateLoanForInterestRecalculation(existingLoanApplication);
                if (changes.containsKey(LoanProductConstants.IS_INTEREST_RECALCULATION_ENABLED_PARAMETER_NAME)) {
                    createAndPersistCalendarInstanceForInterestRecalculation(existingLoanApplication);

                }

            }

            return new CommandProcessingResultBuilder() //
                    .withEntityId(loanId) //
                    .withOfficeId(existingLoanApplication.getOfficeId()) //
                    .withClientId(existingLoanApplication.getClientId()) //
                    .withGroupId(existingLoanApplication.getGroupId()) //
                    .withLoanId(existingLoanApplication.getId()) //
                    .with(changes).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {

        if (realCause.getMessage().contains("loan_account_no_UNIQUE")
                || (realCause.getCause() != null && realCause.getCause().getMessage().contains("loan_account_no_UNIQUE"))) {

            final String accountNo = command.stringValueOfParameterNamed("accountNo");
            throw new PlatformDataIntegrityException("error.msg.loan.duplicate.accountNo",
                    "Loan with accountNo `" + accountNo + "` already exists", "accountNo", accountNo);
        } else if (realCause.getMessage().contains("loan_externalid_UNIQUE")
                || (realCause.getCause() != null && realCause.getCause().getMessage().contains("loan_externalid_UNIQUE"))) {
            final String externalId = command.stringValueOfParameterNamed("externalId");
            throw new PlatformDataIntegrityException("error.msg.loan.duplicate.externalId",
                    "Loan with externalId `" + externalId + "` already exists", "externalId", externalId);
        }

        logAsErrorUnexpectedDataIntegrityException(dve);
        throw new PlatformDataIntegrityException("error.msg.unknown.data.integrity.issue", "Unknown data integrity issue with resource.");
    }

    private void logAsErrorUnexpectedDataIntegrityException(final Exception dve) {
        LOG.error("Error occured.", dve);
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteApplication(final Long loanId) {

        final Loan loan = retrieveLoanBy(loanId);
        checkClientOrGroupActive(loan);

        if (loan.isNotSubmittedAndPendingApproval()) {
            throw new LoanApplicationNotInSubmittedAndPendingApprovalStateCannotBeDeleted(loanId);
        }

        final List<Note> relatedNotes = this.noteRepository.findByLoanId(loan.getId());
        this.noteRepository.deleteAllInBatch(relatedNotes);

        final AccountAssociations accountAssociations = this.accountAssociationsRepository.findByLoanIdAndType(loanId,
                AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue());
        if (accountAssociations != null) {
            this.accountAssociationsRepository.delete(accountAssociations);
        }

        Set<LoanCollateralManagement> loanCollateralManagements = loan.getLoanCollateralManagements();
        for (LoanCollateralManagement loanCollateralManagement : loanCollateralManagements) {
            BigDecimal quantity = loanCollateralManagement.getQuantity();
            ClientCollateralManagement clientCollateralManagement = loanCollateralManagement.getClientCollateralManagement();
            clientCollateralManagement.updateQuantityAfterLoanClosed(quantity);
            loanCollateralManagement.setIsReleased(true);
            loanCollateralManagement.setClientCollateralManagement(clientCollateralManagement);
        }

        this.loanRepositoryWrapper.delete(loanId);

        return new CommandProcessingResultBuilder() //
                .withEntityId(loanId) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loan.getId()) //
                .build();
    }

    public void validateMultiDisbursementData(final JsonCommand command, LocalDate expectedDisbursementDate) {
        final String json = command.json();
        final JsonElement element = this.fromJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
        final BigDecimal principal = this.fromJsonHelper.extractBigDecimalWithLocaleNamed("approvedLoanAmount", element);
        fromApiJsonDeserializer.validateLoanMultiDisbursementDate(element, baseDataValidator, expectedDisbursementDate, principal);
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult approveGLIMLoanAppication(final Long loanId, final JsonCommand command) {
        Boolean isExtendLoanLifeCycleConfig = loanDecisionStateUtilService.isExtendLoanLifeCycleConfig();

        final Long parentLoanId = loanId;
        GroupLoanIndividualMonitoringAccount parentLoan = glimRepository.findById(parentLoanId).orElseThrow();
        JsonArray approvalFormData = command.arrayOfParameterNamed("approvalFormData");

        JsonObject jsonObject = null;
        JsonCommand childCommand = null;
        Long[] childLoanId = new Long[approvalFormData.size()];
        BigDecimal parentPrincipalAmount = command.bigDecimalValueOfParameterNamed("glimPrincipal");

        for (int i = 0; i < approvalFormData.size(); i++) {

            jsonObject = approvalFormData.get(i).getAsJsonObject();

            childLoanId[i] = jsonObject.get("loanId").getAsLong();
        }

        CommandProcessingResult result = null;
        int count = 0;
        int j = 0;
        for (JsonElement approvals : approvalFormData) {

            childCommand = JsonCommand.fromExistingCommand(command, approvals);

            if (isExtendLoanLifeCycleConfig) {
                result = approveLoanApplicationAssociatedToGLIM(childLoanId[j++], childCommand);
            } else {
                result = approveApplication(childLoanId[j++], childCommand, Boolean.FALSE);
            }

            if (result != null && result.getLoanId() != null) {
                count++;
                // if all the child loans are approved, mark the parent loan as
                // approved
                if (count == parentLoan.getChildAccountsCount()) {
                    parentLoan.setPrincipalAmount(parentPrincipalAmount);
                    parentLoan.setLoanStatus(LoanStatus.APPROVED.getValue());
                    glimRepository.save(parentLoan);
                }

            }
            updateGlimActualPrincipal(parentLoan);

        }

        return result;
    }

    private void updateGlimActualPrincipal(GroupLoanIndividualMonitoringAccount parentLoan) {
        final Collection<Integer> loanStatuses = new ArrayList<>(Arrays.asList(LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(),
                LoanStatus.APPROVED.getValue(), LoanStatus.ACTIVE.getValue()));
        List<Loan> activeChild = this.loanRepository.findLoanByGlimIdAndLoanStatus(parentLoan.getId(), loanStatuses);

        if (!CollectionUtils.isEmpty(activeChild)) {
            BigDecimal sum = activeChild.stream().map(Loan::getProposedPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add);

            parentLoan.setPrincipalAmount(sum);
            glimRepository.save(parentLoan);
        } else {
            parentLoan.setPrincipalAmount(BigDecimal.ZERO);
            glimRepository.save(parentLoan);
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult approveApplication(final Long loanId, final JsonCommand command, Boolean isGlimBulkApproval) {

        final AppUser currentUser = getAppUserIfPresent();
        LocalDate expectedDisbursementDate = null;

        this.loanApplicationTransitionApiJsonValidator.validateApproval(command.json());

        final Loan loan = retrieveLoanBy(loanId);

        final Boolean isExtendLoanLifeCycleConfig = this.loanDecisionStateUtilService.isExtendLoanLifeCycleConfig();

        // block loan approval if loan is associated to GLIM and is direct Approval not passing through bulk GLIM
        // Operation call
        if (isExtendLoanLifeCycleConfig && loan.getLoanType().equals(AccountType.GLIM.getValue()) && !isGlimBulkApproval) {
            throw new GLIMLoanCannotBeApprovedException(loanId);
        }

        this.validateActiveLoanCount(loan.getClientId());
        this.loanDecisionStateUtilService.validateLoanAccountWithExtraLoanDecisionStagesConfiguredGlobally(loan, command);

        final JsonArray disbursementDataArray = command.arrayOfParameterNamed(LoanApiConstants.disbursementDataParameterName);

        expectedDisbursementDate = command.localDateValueOfParameterNamed(LoanApiConstants.disbursementDateParameterName);
        if (expectedDisbursementDate == null) {
            expectedDisbursementDate = loan.getExpectedDisbursedOnLocalDate();
        }
        if (loan.loanProduct().isMultiDisburseLoan()) {
            this.validateMultiDisbursementData(command, expectedDisbursementDate);
        }

        checkClientOrGroupActive(loan);

        // if bnpl loan and equity contribution, the check client's savings account for client's contribution for bnpl
        // vendor payment
        BigDecimal amountToDisburseForBnplEquityContributionLoan = BigDecimal.ZERO;
        Boolean isBnplEquityContributionLoan = loan.getBnplLoan() != null && loan.getBnplLoan()
                && loan.getRequiresEquityContribution() != null && loan.getRequiresEquityContribution();
        if (isBnplEquityContributionLoan) {
            Money disburseAmount = loan.adjustDisburseAmount(command, expectedDisbursementDate);
            Money amountToDisburse = disburseAmount.copy();
            // get amount to transfer as equity
            Money equityAmount = Money.zero(amountToDisburse.getCurrency());
            BigDecimal equityContributionLoanPercentage = loan.getEquityContributionLoanPercentage();
            if (equityContributionLoanPercentage.compareTo(BigDecimal.ZERO) > 0) {
                final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
                final MathContext mc = new MathContext(8, roundingMode);
                equityAmount = disburseAmount.percentageOf(equityContributionLoanPercentage, mc.getRoundingMode());
            } else {
                final String errorMessage = "Approval BNPL Loan with id:" + loan.getId()
                        + " requires percentage of loan which needs to be transfer to vendor if the loan RequiresEquityContribution has true";
                throw new LinkedAccountRequiredException("loan.approval.link.Savings", errorMessage, loan.getId());
            }

            // get client savings account
            final PortfolioAccountData clientPortfolioAccountData = this.accountAssociationsReadPlatformService
                    .retriveLoanLinkedAssociation(loan.getId());
            if (clientPortfolioAccountData == null) {
                final String errorMessage = "Approval BNPL Loan with id:" + loan.getId()
                        + " requires linked savings account for disbursement";
                throw new LinkedAccountRequiredException("loan.approval.links.savings", errorMessage, loan.getId());
            }
            final SavingsAccount clientSavingsAccount = this.savingsAccountAssembler.assembleFrom(clientPortfolioAccountData.accountId(),
                    false);
            clientSavingsAccount.validateAccountBalanceForBnplLoanWithEquityContribution(equityAmount.getAmount(), false);
            amountToDisburseForBnplEquityContributionLoan = amountToDisburse.getAmount().subtract(equityAmount.getAmount());
        }

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
            // final Calendar calendar = calendarInstance.getCalendar();
            boolean isSkipRepaymentOnFirstMonthEnabled = this.configurationDomainService.isSkippingMeetingOnFirstDayOfMonthEnabled();
            if (isSkipRepaymentOnFirstMonthEnabled) {
                isSkipRepaymentOnFirstMonth = this.loanUtilService.isLoanRepaymentsSyncWithMeeting(loan.group(), calendar);
                if (isSkipRepaymentOnFirstMonth) {
                    numberOfDays = configurationDomainService.retreivePeroidInNumberOfDaysForSkipMeetingDate().intValue();
                }
            }
            this.loanScheduleAssembler.validateDisbursementDateWithMeetingDates(expectedDisbursementDate, calendar,
                    isSkipRepaymentOnFirstMonth, numberOfDays);
        }

        final Map<String, Object> changes = loan.loanApplicationApproval(currentUser, command, disbursementDataArray,
                defaultLoanLifecycleStateMachine(), isBnplEquityContributionLoan, amountToDisburseForBnplEquityContributionLoan, isExtendLoanLifeCycleConfig);

        entityDatatableChecksWritePlatformService.runTheCheckForProduct(loanId, EntityTables.LOAN.getName(),
                StatusEnum.APPROVE.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(), loan.productId());

        if (!changes.isEmpty()) {

            // If loan approved amount less than loan demanded amount, then need
            // to recompute the schedule
            if (changes.containsKey(LoanApiConstants.approvedLoanAmountParameterName) || changes.containsKey("recalculateLoanSchedule")
                    || changes.containsKey("expectedDisbursementDate")) {
                LocalDate recalculateFrom = null;
                ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
                loan.regenerateRepaymentSchedule(scheduleGeneratorDTO);
            }

            if (loan.isTopup() && loan.getClientId() != null) {
                final Long loanIdToClose = loan.getTopupLoanDetails().getLoanIdToClose();
                final Loan loanToClose = this.loanRepositoryWrapper.findNonClosedLoanThatBelongsToClient(loanIdToClose, loan.getClientId());
                if (loanToClose == null) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.to.be.closed.with.topup.is.not.active",
                            "Loan to be closed with this topup is not active.");
                }

                final LocalDate lastUserTransactionOnLoanToClose = loanToClose.getLastUserTransactionDate();
                if (loan.getDisbursementDate().isBefore(lastUserTransactionOnLoanToClose)) {
                    throw new GeneralPlatformDomainRuleException(
                            "error.msg.loan.disbursal.date.should.be.after.last.transaction.date.of.loan.to.be.closed",
                            "Disbursal date of this loan application " + loan.getDisbursementDate()
                                    + " should be after last transaction date of loan to be closed " + lastUserTransactionOnLoanToClose);
                }
                BigDecimal loanOutstanding = this.loanReadPlatformService
                        .retrieveLoanForeclosureTemplate(loanIdToClose, expectedDisbursementDate).getAmount();
                final BigDecimal firstDisbursalAmount = loan.getFirstDisbursalAmount();
                if (loanOutstanding.compareTo(firstDisbursalAmount) > 0) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.amount.less.than.outstanding.of.loan.to.be.closed",
                            "Topup loan amount should be greater than outstanding amount of loan to be closed.");
                }
                BigDecimal netDisbursalAmount = loan.getApprovedPrincipal().subtract(loanOutstanding);
                loan.adjustNetDisbursalAmount(netDisbursalAmount);
            }

            saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.loanNote(loan, noteText);
                changes.put("note", noteText);
                this.noteRepository.save(note);
            }

            businessEventNotifierService.notifyPostBusinessEvent(new LoanApprovedBusinessEvent(loan));
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    private void validateActiveLoanCount(Long clientId) {
        if (this.configurationDomainService.isMaxActiveLoansEnabled()) {
            final int activeLoanCount = this.loanReadPlatformService.retrieveNumberOfActiveLoansByClientId(clientId);
            final int maxActiveLoans = this.configurationDomainService.getMaxActiveLoans().intValue();
            if (maxActiveLoans > 0 && ((activeLoanCount + 1) > maxActiveLoans)) {
                final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
                final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loanapplication");
                baseDataValidator.reset().parameter("loanId").value(clientId).failWithCode("max.active.loans.exceeded");
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors);
            }
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult undoGLIMLoanApplicationApproval(final Long loanId, final JsonCommand command) {

        // GroupLoanIndividualMonitoringAccount
        // glimAccount=glimRepository.findOne(loanId);
        final Long parentLoanId = loanId;
        GroupLoanIndividualMonitoringAccount parentLoan = glimRepository.findById(parentLoanId).orElseThrow();
        List<Loan> childLoans = this.loanRepository.findByGlimId(loanId);

        CommandProcessingResult result = null;
        int count = 0;
        for (Loan loan : childLoans) {
            result = undoApplicationApproval(loan.getId(), command);

            if (result.getLoanId() != null) {
                count++;
                // if all the child loans are approved, mark the parent loan as
                // approved
                if (count == parentLoan.getChildAccountsCount()) {
                    parentLoan.setLoanStatus(LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue());
                    glimRepository.save(parentLoan);
                }

            }

        }

        return result;
    }

    @Transactional
    @Override
    public CommandProcessingResult undoApplicationApproval(final Long loanId, final JsonCommand command) {

        AppUser currentUser = getAppUserIfPresent();

        this.fromApiJsonDeserializer.validateForUndo(command.json());

        final Loan loan = retrieveLoanBy(loanId);
        checkClientOrGroupActive(loan);

        final Map<String, Object> changes = loan.undoApproval(defaultLoanLifecycleStateMachine());
        if (!changes.isEmpty()) {

            // If loan approved amount is not same as loan amount demanded, then
            // during undo, restore the demand amount to principal amount.

            if (changes.containsKey(LoanApiConstants.approvedLoanAmountParameterName)
                    || changes.containsKey(LoanApiConstants.disbursementPrincipalParameterName)) {
                LocalDate recalculateFrom = null;
                ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
                loan.regenerateRepaymentSchedule(scheduleGeneratorDTO);
            }

            loan.adjustNetDisbursalAmount(loan.getProposedPrincipal());

            saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.loanNote(loan, noteText);
                this.noteRepository.save(note);
            }
            businessEventNotifierService.notifyPostBusinessEvent(new LoanUndoApprovalBusinessEvent(loan));
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult rejectGLIMApplicationApproval(final Long glimId, final JsonCommand command) {

        // GroupLoanIndividualMonitoringAccount
        // glimAccount=glimRepository.findOne(loanId);
        final Long parentLoanId = glimId;
        GroupLoanIndividualMonitoringAccount parentLoan = glimRepository.findById(parentLoanId).orElseThrow();
        List<Loan> childLoans = this.loanRepository.findByGlimId(glimId);

        CommandProcessingResult result = null;
        int count = 0;
        for (Loan loan : childLoans) {
            result = rejectApplication(loan.getId(), command);

            if (result.getLoanId() != null) {
                count++;
                // if all the child loans are Rejected, mark the parent loan as
                // rejected
                if (count == parentLoan.getChildAccountsCount()) {
                    parentLoan.setLoanStatus(LoanStatus.REJECTED.getValue());
                    glimRepository.save(parentLoan);
                }

            }

        }

        return result;
    }

    @Transactional
    @Override
    public CommandProcessingResult rejectApplication(final Long loanId, final JsonCommand command) {

        final AppUser currentUser = getAppUserIfPresent();

        this.loanApplicationTransitionApiJsonValidator.validateRejection(command.json());
        Boolean isExtendLoanLifeCycleConfig = loanDecisionStateUtilService.isExtendLoanLifeCycleConfig();
        final Loan loan = retrieveLoanBy(loanId);

        checkClientOrGroupActive(loan);

        entityDatatableChecksWritePlatformService.runTheCheckForProduct(loanId, EntityTables.LOAN.getName(),
                StatusEnum.REJECTED.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(), loan.productId());
        Map<String, Object> changes = new HashMap<>();
        if ((isExtendLoanLifeCycleConfig && loan.isSubmittedAndPendingApproval() && loan.getLoanDecisionState() != null
                && loanDecisionStateUtilService.isLoanAccountInICReview(LoanDecisionState.fromInt(loan.getLoanDecisionState())))) {

            // intercept the reject module and transition the loan to other stages
            switch (LoanDecisionState.fromInt(loan.getLoanDecisionState())) {
                case COLLATERAL_REVIEW:
                    changes = rejectLoanAccountForIcReviewLevelOne(command, currentUser, loan, changes);
                break;
                case IC_REVIEW_LEVEL_ONE:
                    changes = rejectLoanAccountForIcReviewLevelTwo(command, currentUser, loan, changes);
                break;
                case IC_REVIEW_LEVEL_TWO:
                    changes = rejectLoanAccountForIcReviewLevelThree(command, currentUser, loan, changes);
                break;
                case IC_REVIEW_LEVEL_THREE:
                    changes = rejectLoanAccountForIcReviewLevelFour(command, currentUser, loan, changes);
                break;
                case IC_REVIEW_LEVEL_FOUR:
                    changes = rejectLoanAccountForIcReviewLevelFive(command, currentUser, loan);

                break;
                default:
                    changes = rejectLoanAccountParentStatus(command, currentUser, loan);
                break;
            }
        } else {
            changes = rejectLoanAccountParentStatus(command, currentUser, loan);
        }
        // update GLIM Account on actual Amounts
        if (loan.getLoanType().equals(AccountType.GLIM.getValue())) {
            GroupLoanIndividualMonitoringAccount parentLoan = glimRepository.findById(loan.getGlimId()).orElseThrow();
            updateGlimActualPrincipal(parentLoan);
        }

        businessEventNotifierService.notifyPostBusinessEvent(new LoanRejectedBusinessEvent(loan));
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    @NotNull
    private Map<String, Object> rejectLoanAccountForIcReviewLevelFive(JsonCommand command, AppUser currentUser, Loan loan) {
        Map<String, Object> changes;
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());
        LocalDate rejectedOnDate = command.localDateValueOfParameterNamed("rejectedOnDate");

        loanDecisionStateUtilService.validateIcReviewDecisionLevelFiveBusinessRule(command, loan, loanDecision, rejectedOnDate);
        LoanApprovalMatrix approvalMatrix = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(loan.getCurrencyCode());

        if (approvalMatrix == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.does.not.exist.",
                    String.format("Loan Approval Matrix with Currency [ %s ] doesn't exist. Approval matrix is expected to continue ",
                            loan.getCurrencyCode()));
        }
        // Get Loan Matrix
        // Determine which cycle of this Loan Account
        // Determine the Next Level or stage to review
        // Add custom Params in Decision Table
        List<Loan> loanIndividualCounter = loanDecisionStateUtilService.getLoanCounter(loan);

        Boolean isLoanFirstCycle = loanDecisionStateUtilService.isLoanFirstCycle(loanIndividualCounter);
        Boolean isLoanUnsecure = loanDecisionStateUtilService.isLoanUnSecure(loan);
        final BigDecimal dueDiligenceRecommendedAmount = loanDecision.getDueDiligenceRecommendedAmount();

        loanDecisionStateUtilService.validateLoanAccountToComplyToApprovalMatrixStage(loan, approvalMatrix, isLoanFirstCycle,
                isLoanUnsecure, LoanDecisionState.IC_REVIEW_LEVEL_FIVE, dueDiligenceRecommendedAmount);

        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleIcReviewDecisionLevelFiveFrom(command, currentUser, loanDecision,
                Boolean.TRUE, rejectedOnDate, null, null, null);
        loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_FIVE.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getIcReviewDecisionLevelFiveNote())) {
            final Note note = Note.loanNote(loanObj,
                    "Reject IC Review-Decision Level Five : " + loanDecisionObj.getIcReviewDecisionLevelFiveNote());
            this.noteRepository.save(note);
        }
        // By Default Completely Reject this Loan Account since this is a last stage of IC Review
        changes = rejectLoanAccountParentStatus(command, currentUser, loan);
        return changes;
    }

    private Map<String, Object> rejectLoanAccountForIcReviewLevelFour(JsonCommand command, AppUser currentUser, Loan loan,
            Map<String, Object> changes) {
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());
        LocalDate rejectedOnDate = command.localDateValueOfParameterNamed("rejectedOnDate");

        loanDecisionStateUtilService.validateIcReviewDecisionLevelFourBusinessRule(command, loan, loanDecision, rejectedOnDate);
        LoanApprovalMatrix approvalMatrix = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(loan.getCurrencyCode());

        if (approvalMatrix == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.does.not.exist.",
                    String.format("Loan Approval Matrix with Currency [ %s ] doesn't exist. Approval matrix is expected to continue ",
                            loan.getCurrencyCode()));
        }
        // Get Loan Matrix
        // Determine which cycle of this Loan Account
        // Determine the Next Level or stage to review
        // Add custom Params in Decision Table
        List<Loan> loanIndividualCounter = loanDecisionStateUtilService.getLoanCounter(loan);

        Boolean isLoanFirstCycle = loanDecisionStateUtilService.isLoanFirstCycle(loanIndividualCounter);
        Boolean isLoanUnsecure = loanDecisionStateUtilService.isLoanUnSecure(loan);
        final BigDecimal dueDiligenceRecommendedAmount = loanDecision.getDueDiligenceRecommendedAmount();

        loanDecisionStateUtilService.validateLoanAccountToComplyToApprovalMatrixStage(loan, approvalMatrix, isLoanFirstCycle,
                isLoanUnsecure, LoanDecisionState.IC_REVIEW_LEVEL_FOUR, dueDiligenceRecommendedAmount);
        // generate the next stage based on loan approval matrix via amounts to be disbursed
        loanDecisionStateUtilService.determineTheNextDecisionStage(loan, loanDecision, approvalMatrix, isLoanFirstCycle, isLoanUnsecure,
                LoanDecisionState.IC_REVIEW_LEVEL_FOUR, dueDiligenceRecommendedAmount);

        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleIcReviewDecisionLevelFourFrom(command, currentUser, loanDecision,
                Boolean.TRUE, rejectedOnDate, null, null, null);
        loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_FOUR.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getIcReviewDecisionLevelFourNote())) {
            final Note note = Note.loanNote(loanObj,
                    "Reject IC Review-Decision Level Four : " + loanDecisionObj.getIcReviewDecisionLevelFourNote());
            this.noteRepository.save(note);
        }
        // If the next state is outside the IC Review, then reject the loan account completely
        if (loanDecisionObj.getNextLoanIcReviewDecisionState().equals(LoanDecisionState.PREPARE_AND_SIGN_CONTRACT.getValue())) {
            changes = rejectLoanAccountParentStatus(command, currentUser, loan);
        }
        return changes;
    }

    private Map<String, Object> rejectLoanAccountForIcReviewLevelThree(JsonCommand command, AppUser currentUser, Loan loan,
            Map<String, Object> changes) {
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());
        LocalDate rejectedOnDate = command.localDateValueOfParameterNamed("rejectedOnDate");

        loanDecisionStateUtilService.validateIcReviewDecisionLevelThreeBusinessRule(command, loan, loanDecision, rejectedOnDate);
        LoanApprovalMatrix approvalMatrix = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(loan.getCurrencyCode());

        if (approvalMatrix == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.does.not.exist.",
                    String.format("Loan Approval Matrix with Currency [ %s ] doesn't exist. Approval matrix is expected to continue ",
                            loan.getCurrencyCode()));
        }
        // Get Loan Matrix
        // Determine which cycle of this Loan Account
        // Determine the Next Level or stage to review
        // Add custom Params in Decision Table
        List<Loan> loanIndividualCounter = loanDecisionStateUtilService.getLoanCounter(loan);

        Boolean isLoanFirstCycle = loanDecisionStateUtilService.isLoanFirstCycle(loanIndividualCounter);
        Boolean isLoanUnsecure = loanDecisionStateUtilService.isLoanUnSecure(loan);
        final BigDecimal dueDiligenceRecommendedAmount = loanDecision.getDueDiligenceRecommendedAmount();

        loanDecisionStateUtilService.validateLoanAccountToComplyToApprovalMatrixStage(loan, approvalMatrix, isLoanFirstCycle,
                isLoanUnsecure, LoanDecisionState.IC_REVIEW_LEVEL_THREE, dueDiligenceRecommendedAmount);
        // generate the next stage based on loan approval matrix via amounts to be disbursed
        loanDecisionStateUtilService.determineTheNextDecisionStage(loan, loanDecision, approvalMatrix, isLoanFirstCycle, isLoanUnsecure,
                LoanDecisionState.IC_REVIEW_LEVEL_THREE, dueDiligenceRecommendedAmount);

        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleIcReviewDecisionLevelThreeFrom(command, currentUser, loanDecision,
                Boolean.TRUE, rejectedOnDate, null, null, null);
        loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_THREE.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getIcReviewDecisionLevelThreeNote())) {
            final Note note = Note.loanNote(loanObj,
                    "Reject IC Review-Decision Level Three : " + loanDecisionObj.getIcReviewDecisionLevelThreeNote());
            this.noteRepository.save(note);
        }
        // If the next state is outside the IC Review, then reject the loan account completely
        if (loanDecisionObj.getNextLoanIcReviewDecisionState().equals(LoanDecisionState.PREPARE_AND_SIGN_CONTRACT.getValue())) {
            changes = rejectLoanAccountParentStatus(command, currentUser, loan);
        }
        return changes;
    }

    private Map<String, Object> rejectLoanAccountForIcReviewLevelTwo(JsonCommand command, AppUser currentUser, Loan loan,
            Map<String, Object> changes) {
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());

        LocalDate rejectedOnDate = command.localDateValueOfParameterNamed("rejectedOnDate");

        loanDecisionStateUtilService.validateIcReviewDecisionLevelTwoBusinessRule(command, loan, loanDecision, rejectedOnDate);
        LoanApprovalMatrix approvalMatrix = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(loan.getCurrencyCode());

        if (approvalMatrix == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.does.not.exist.",
                    String.format("Loan Approval Matrix with Currency [ %s ] doesn't exist. Approval matrix is expected to continue ",
                            loan.getCurrencyCode()));
        }
        // Get Loan Matrix
        // Determine which cycle of this Loan Account
        // Determine the Next Level or stage to review
        // Add custom Params in Decision Table
        List<Loan> loanIndividualCounter = loanDecisionStateUtilService.getLoanCounter(loan);

        Boolean isLoanFirstCycle = loanDecisionStateUtilService.isLoanFirstCycle(loanIndividualCounter);
        Boolean isLoanUnsecure = loanDecisionStateUtilService.isLoanUnSecure(loan);
        final BigDecimal dueDiligenceRecommendedAmount = loanDecision.getDueDiligenceRecommendedAmount();

        loanDecisionStateUtilService.validateLoanAccountToComplyToApprovalMatrixStage(loan, approvalMatrix, isLoanFirstCycle,
                isLoanUnsecure, LoanDecisionState.IC_REVIEW_LEVEL_TWO, dueDiligenceRecommendedAmount);
        // generate the next stage based on loan approval matrix via amounts to be disbursed
        loanDecisionStateUtilService.determineTheNextDecisionStage(loan, loanDecision, approvalMatrix, isLoanFirstCycle, isLoanUnsecure,
                LoanDecisionState.IC_REVIEW_LEVEL_TWO, dueDiligenceRecommendedAmount);

        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleIcReviewDecisionLevelTwoFrom(command, currentUser, loanDecision,
                Boolean.TRUE, rejectedOnDate, null, null, null);
        loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_TWO.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getIcReviewDecisionLevelTwoNote())) {
            final Note note = Note.loanNote(loanObj,
                    "Reject IC Review-Decision Level Two : " + loanDecisionObj.getIcReviewDecisionLevelTwoNote());
            this.noteRepository.save(note);
        }
        // If the next state is outside the IC Review, then reject the loan account completely
        if (loanDecisionObj.getNextLoanIcReviewDecisionState().equals(LoanDecisionState.PREPARE_AND_SIGN_CONTRACT.getValue())) {
            changes = rejectLoanAccountParentStatus(command, currentUser, loan);
        }
        return changes;
    }

    private Map<String, Object> rejectLoanAccountForIcReviewLevelOne(JsonCommand command, AppUser currentUser, Loan loan,
            Map<String, Object> changes) {
        final LoanDecision loanDecision = this.loanDecisionRepository.findLoanDecisionByLoanId(loan.getId());

        LocalDate rejectedOnDate = command.localDateValueOfParameterNamed("rejectedOnDate");

        loanDecisionStateUtilService.validateIcReviewDecisionLevelOneBusinessRule(command, loan, loanDecision, rejectedOnDate);
        LoanApprovalMatrix approvalMatrix = this.loanApprovalMatrixRepository.findLoanApprovalMatrixByCurrency(loan.getCurrencyCode());

        if (approvalMatrix == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.approval.matrix.with.this.currency.does.not.exist.",
                    String.format("Loan Approval Matrix with Currency [ %s ] doesn't exist. Approval matrix is expected to continue ",
                            loan.getCurrencyCode()));
        }

        List<Loan> loanIndividualCounter = loanDecisionStateUtilService.getLoanCounter(loan);

        Boolean isLoanFirstCycle = loanDecisionStateUtilService.isLoanFirstCycle(loanIndividualCounter);
        Boolean isLoanUnsecure = loanDecisionStateUtilService.isLoanUnSecure(loan);
        final BigDecimal dueDiligenceRecommendedAmount = loanDecision.getDueDiligenceRecommendedAmount();

        loanDecisionStateUtilService.validateLoanAccountToComplyToApprovalMatrixStage(loan, approvalMatrix, isLoanFirstCycle,
                isLoanUnsecure, LoanDecisionState.IC_REVIEW_LEVEL_ONE, dueDiligenceRecommendedAmount);
        // generate the next stage based on loan approval matrix via amounts to be disbursed
        loanDecisionStateUtilService.determineTheNextDecisionStage(loan, loanDecision, approvalMatrix, isLoanFirstCycle, isLoanUnsecure,
                LoanDecisionState.IC_REVIEW_LEVEL_ONE, dueDiligenceRecommendedAmount);

        LoanDecision loanDecisionObj = loanDecisionAssembler.assembleIcReviewDecisionLevelOneFrom(command, currentUser, loanDecision,
                Boolean.TRUE, rejectedOnDate, null, null, null);
        loanDecisionRepository.saveAndFlush(loanDecisionObj);

        Loan loanObj = loan;
        loanObj.setLoanDecisionState(LoanDecisionState.IC_REVIEW_LEVEL_ONE.getValue());
        this.loanRepositoryWrapper.saveAndFlush(loanObj);

        if (StringUtils.isNotBlank(loanDecisionObj.getIcReviewDecisionLevelOneNote())) {
            final Note note = Note.loanNote(loanObj,
                    "Reject IC Review-Decision Level One : " + loanDecisionObj.getIcReviewDecisionLevelOneNote());
            this.noteRepository.save(note);
        }
        // If the next state is outside the IC Review, then reject the loan account completely
        if (loanDecisionObj.getNextLoanIcReviewDecisionState().equals(LoanDecisionState.PREPARE_AND_SIGN_CONTRACT.getValue())) {
            changes = rejectLoanAccountParentStatus(command, currentUser, loan);
        }
        return changes;
    }

    @NotNull
    private Map<String, Object> rejectLoanAccountParentStatus(JsonCommand command, AppUser currentUser, Loan loan) {
        Map<String, Object> changes;
        changes = loan.loanApplicationRejection(currentUser, command, defaultLoanLifecycleStateMachine());
        if (!changes.isEmpty()) {
            final String noteText = command.stringValueOfParameterNamed("note");
            this.loanRepositoryWrapper.saveAndFlush(loan);
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.loanNote(loan, noteText);
                this.noteRepository.save(note);
            }
        }
        return changes;
    }

    @Transactional
    @Override
    public CommandProcessingResult applicantWithdrawsFromApplication(final Long loanId, final JsonCommand command) {

        final AppUser currentUser = getAppUserIfPresent();

        this.loanApplicationTransitionApiJsonValidator.validateApplicantWithdrawal(command.json());

        final Loan loan = retrieveLoanBy(loanId);
        checkClientOrGroupActive(loan);

        entityDatatableChecksWritePlatformService.runTheCheckForProduct(loanId, EntityTables.LOAN.getName(),
                StatusEnum.WITHDRAWN.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(), loan.productId());

        final Map<String, Object> changes = loan.loanApplicationWithdrawnByApplicant(currentUser, command,
                defaultLoanLifecycleStateMachine());

        // Release attached collaterals
        if (AccountType.fromInt(loan.getLoanType()).isIndividualAccount()) {
            Set<LoanCollateralManagement> loanCollateralManagements = loan.getLoanCollateralManagements();
            for (LoanCollateralManagement loanCollateralManagement : loanCollateralManagements) {
                ClientCollateralManagement clientCollateralManagement = loanCollateralManagement.getClientCollateralManagement();
                clientCollateralManagement
                        .updateQuantity(clientCollateralManagement.getQuantity().add(loanCollateralManagement.getQuantity()));
                loanCollateralManagement.setClientCollateralManagement(clientCollateralManagement);
                loanCollateralManagement.setIsReleased(true);
            }
            loan.updateLoanCollateral(loanCollateralManagements);
        }

        if (!changes.isEmpty()) {
            this.loanRepositoryWrapper.saveAndFlush(loan);

            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.loanNote(loan, noteText);
                this.noteRepository.save(note);
            }
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    private Loan retrieveLoanBy(final Long loanId) {
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        loan.setHelpers(defaultLoanLifecycleStateMachine(), this.loanSummaryWrapper, this.loanRepaymentScheduleTransactionProcessorFactory);
        return loan;
    }

    private void validateSubmittedOnDate(final Loan loan) {
        final LocalDate startDate = loan.loanProduct().getStartDate();
        final LocalDate closeDate = loan.loanProduct().getCloseDate();
        final LocalDate expectedFirstRepaymentOnDate = loan.getExpectedFirstRepaymentOnDate();
        final LocalDate submittedOnDate = loan.getSubmittedOnDate();

        String defaultUserMessage = "";
        if (startDate != null && submittedOnDate.isBefore(startDate)) {
            defaultUserMessage = "submittedOnDate cannot be before the loan product startDate.";
            throw new LoanApplicationDateException("submitted.on.date.cannot.be.before.the.loan.product.start.date", defaultUserMessage,
                    submittedOnDate.toString(), startDate.toString());
        }

        if (closeDate != null && submittedOnDate.isAfter(closeDate)) {
            defaultUserMessage = "submittedOnDate cannot be after the loan product closeDate.";
            throw new LoanApplicationDateException("submitted.on.date.cannot.be.after.the.loan.product.close.date", defaultUserMessage,
                    submittedOnDate.toString(), closeDate.toString());
        }

        if (expectedFirstRepaymentOnDate != null && submittedOnDate.isAfter(expectedFirstRepaymentOnDate)) {
            defaultUserMessage = "submittedOnDate cannot be after the loans  expectedFirstRepaymentOnDate.";
            throw new LoanApplicationDateException("submitted.on.date.cannot.be.after.the.loan.expected.first.repayment.date",
                    defaultUserMessage, submittedOnDate.toString(), expectedFirstRepaymentOnDate.toString());
        }
    }

    private void checkClientOrGroupActive(final Loan loan) {
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

    private void saveAndFlushLoanWithDataIntegrityViolationChecks(final Loan loan) {
        try {
            this.loanRepositoryWrapper.saveAndFlush(loan);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.application");
            if (realCause.getMessage().toLowerCase().contains("external_id_unique")) {
                baseDataValidator.reset().parameter("externalId").failWithCode("value.must.be.unique");
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors, e);
            }
        }
    }

    private AppUser getAppUserIfPresent() {
        AppUser user = null;
        if (this.context != null) {
            user = this.context.getAuthenticatedUserIfPresent();
        }
        return user;
    }

    private void officeSpecificLoanProductValidation(final Long productId, final Long officeId) {
        final GlobalConfigurationProperty restrictToUserOfficeProperty = this.globalConfigurationRepository
                .findOneByNameWithNotFoundDetection(FineractEntityAccessConstants.GLOBAL_CONFIG_FOR_OFFICE_SPECIFIC_PRODUCTS);
        if (restrictToUserOfficeProperty.isEnabled()) {
            FineractEntityRelation fineractEntityRelation = fineractEntityRelationRepository
                    .findOneByCodeName(FineractEntityAccessType.OFFICE_ACCESS_TO_LOAN_PRODUCTS.toStr());
            FineractEntityToEntityMapping officeToLoanProductMappingList = this.repository.findListByProductId(fineractEntityRelation,
                    productId, officeId);
            if (officeToLoanProductMappingList == null) {
                throw new NotOfficeSpecificProductException(productId, officeId);
            }

        }
    }

    @Transactional
    @Override
    public CommandProcessingResult updateArrearsTolerance(final Long loanId, final JsonCommand command) {

        try {
            final Loan existingLoanApplication = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
            final Map<String, Object> changes = new HashMap<>();
            if (existingLoanApplication.loanProduct().isAccountLevelArrearsToleranceEnable()) {
                if (existingLoanApplication.getLoanProductRelatedDetail().getGraceOnArrearsAgeing() != null
                        && existingLoanApplication.getLoanProductRelatedDetail().getGraceOnArrearsAgeing() == 0) {
                    throw new GeneralPlatformDomainRuleException("error.msg.arrears.tolerance.limit.exceed",
                            "Arrears Tolerance Limit Has Exceed.");
                }
                final Integer graceOnArrearsAging = command.integerValueOfParameterNamed("graceOnArrearsAging");
                final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
                final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
                baseDataValidator.reset().parameter("graceOnArrearsAging").value(graceOnArrearsAging).ignoreIfNull().positiveAmount();
                if (!dataValidationErrors.isEmpty()) {
                    throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                            dataValidationErrors);
                }
                LocalDate fromDate = null;
                LocalDate dueDate = null;
                for (LoanRepaymentScheduleInstallment inst : existingLoanApplication.getRepaymentScheduleInstallments()) {
                    if (inst.getDueDate().isAfter(DateUtils.getBusinessLocalDate())) {
                        if (fromDate == null) {
                            fromDate = inst.getDueDate();
                        } else if (dueDate == null) {
                            dueDate = inst.getDueDate();
                            break;
                        }
                    }
                }
                int daysInPeriod = Math.toIntExact(ChronoUnit.DAYS.between(fromDate, dueDate));
                if (daysInPeriod < graceOnArrearsAging) {
                    throw new GeneralPlatformDomainRuleException(
                            "error.grace.on.arrears.aging.days.should.not.be.greater.than.days.in.installment.period",
                            "Grace on Arrears aging days should be less than from period for installment available days ." + daysInPeriod);
                }
                existingLoanApplication.setGraceOnArrearsAging(graceOnArrearsAging);

                changes.put("graceOnArrearsAging", graceOnArrearsAging);
                this.loanRepositoryWrapper.saveAndFlush(existingLoanApplication);
            } else {
                throw new GeneralPlatformDomainRuleException(
                        "error.msg.account.level.arrears.tolerance.not.enabled,can't.update.that.value",
                        "Account level Arrears Tolerance checkbox not checked in product can't update the value for parameter GraceOnArrearsAging.");
            }
            return new CommandProcessingResultBuilder() //
                    .withEntityId(loanId) //
                    .withOfficeId(existingLoanApplication.getOfficeId()) //
                    .withClientId(existingLoanApplication.getClientId()) //
                    .withGroupId(existingLoanApplication.getGroupId()) //
                    .withLoanId(existingLoanApplication.getId()) //
                    .with(changes).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Override
    public CommandProcessingResult withdrawalFromRedraw(Long loanId, JsonCommand command) {
        this.loanApplicationTransitionApiJsonValidator.validateWithdrawalFromRedraw(command.json());
        final var transactionDate = command.localDateValueOfParameterNamed(LoanApiConstants.transactionDateParamName);
        final var transactionAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.principalDisbursedParameterName);
        final var notes = command.stringValueOfParameterNamed(LoanApiConstants.noteParameterName);
        final var commandProcessingResultBuilder = new CommandProcessingResultBuilder();
        this.loanAccountDomainService.withdrawFromRedraw(loanId, commandProcessingResultBuilder, transactionDate, transactionAmount, null,
                notes, null);
        return commandProcessingResultBuilder.withEntityId(loanId).withLoanId(loanId).build();
    }

    @Override
    public CommandProcessingResult applyRedrawPayment(Long loanId, JsonCommand command) {
        var user = this.context.authenticatedUser();
        this.loanApplicationTransitionApiJsonValidator.validateApplyRedrawPayment(command.json());
        final var transactionAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.principalDisbursedParameterName);
        final var loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
        final var transactionDate = command.localDateValueOfParameterNamed(LoanApiConstants.transactionDateParamName);
        loanRepositoryWrapper.updateRedrawAmount(loan, user, loanId, transactionAmount, true, transactionDate, null);
        return new CommandProcessingResultBuilder().withEntityId(loanId).withLoanId(loanId).build();
    }

    @Override
    public CommandProcessingResult generateCashFlow(Long loanId, JsonCommand command) {
        final Loan loan = retrieveLoanBy(loanId);

        if (loan.getLoanDecisionState() == null || !loan.status().isSubmittedAndPendingApproval()
                || !loan.getLoanDecisionState().equals(LoanDecisionState.REVIEW_APPLICATION.getValue())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.not.in.due.diligence.stage.so.cashflow.cannot.be.generated",
                    "Loan is not in Due Diligence Stage so CashFlow cannot be generated");
        }
        List<LoanCashFlowData> loanCashFlowDataList = this.loanReadPlatformService.retrieveCashFlow(loanId);
        if (CollectionUtils.isEmpty(loanCashFlowDataList)) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.cashflow.data.is.not.available.so.cashflow.cannot.be.generated",
                    "Loan CashFlow data is not available so CashFlow cannot be generated");
        }
        validateCashFlowType(loanCashFlowDataList, "Sales Income");
        validateCashFlowType(loanCashFlowDataList, "Purchases");

        Integer projectionRate = this.loanReadPlatformService.retrieveProjectionRate(loanId);
        if (projectionRate == null || projectionRate <= 0) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.projection.rate.is.not.available.so.cashflow.cannot.be.generated",
                    "Loan Projection Rate is not available so CashFlow cannot be generated");
        }
        List<LoanCashFlowProjectionData> cashFlowProjectionList = this.loanReadPlatformService.retrieveCashFlowProjection(loanId);

        final Integer cashFlowType = command.integerValueOfParameterNamed("cashFlowType");
        if (cashFlowType == null && !CollectionUtils.isEmpty(cashFlowProjectionList)) {
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.loan.cashflow.projection.data.is.already.available.so.cashflow.cannot.be.regenerated",
                    "Loan CashFlow Projection data is already Generated so CashFlow cannot be regenerated");
        }

        if (cashFlowType != null) {
            this.fromApiJsonDeserializer.validateCashFlowProjectionUpdate(command.json());
        }

        BigDecimal previousSales = BigDecimal.ZERO;
        BigDecimal previousPurchases = BigDecimal.ZERO;

        BigDecimal salesComputed;
        BigDecimal purchaseComputed;

        final Integer month = command.integerValueOfParameterNamed("month");
        final Integer updatedProjectionRate = command.integerValueOfParameterNamed("projectionRate");
        Integer incomeProjectionRate = projectionRate;
        Integer expenseProjectionRate = projectionRate;

        this.loanCashFlowProjectionRepository.deleteByLoanId(loanId);
        for (LoanRepaymentScheduleInstallment installment : loan.getRepaymentScheduleInstallments()) {
            LOG.info("installment id: " + installment.getId() + " Month " + installment.getInstallmentNumber());
            for (LoanCashFlowData cashFlow : loanCashFlowDataList) {

                if (cashFlow.getCashFlowType().equals("INCOME") && cashFlow.getParticularType().equals("Sales Income")) {
                    LOG.info("INCOME -- cashflow Data :- " + cashFlow.getName() + " " + cashFlow.getMonth0() + "    * *"
                            + cashFlow.getCashFlowType());
                    if (cashFlowType != null && cashFlowType == 1 && month != null && Objects.equals(month, installment.getInstallmentNumber())) {
                        incomeProjectionRate = updatedProjectionRate;
                    }
                    if (installment.getInstallmentNumber() == 1) {
                        salesComputed = computeProjection(incomeProjectionRate, cashFlow.getMonth0());
                        previousSales = salesComputed;
                    } else {
                        salesComputed = computeProjection(incomeProjectionRate, previousSales);
                        previousSales = salesComputed;
                    }
                    saveCashFlowProjection(incomeProjectionRate, salesComputed, installment, cashFlow);

                    LOG.info("INCOME -- Computed :- " + salesComputed + "Previous sales" + previousSales);
                }

                if (cashFlow.getCashFlowType().equals("EXPENSE") && cashFlow.getParticularType().equals("Purchases")) {
                    LOG.info("EXPENSE -- cashflow Data :- " + cashFlow.getName() + " " + cashFlow.getMonth0() + "    * *"
                            + cashFlow.getCashFlowType());
                    if (cashFlowType != null && cashFlowType == 2 && month != null && Objects.equals(month, installment.getInstallmentNumber())) {
                        expenseProjectionRate = updatedProjectionRate;
                    }

                    if (installment.getInstallmentNumber() == 1) {
                        purchaseComputed = computeProjection(expenseProjectionRate, cashFlow.getMonth0());
                        previousPurchases = purchaseComputed;
                    } else {
                        purchaseComputed = computeProjection(expenseProjectionRate, previousPurchases);
                        previousPurchases = purchaseComputed;
                    }
                    saveCashFlowProjection(expenseProjectionRate, purchaseComputed, installment, cashFlow);

                    LOG.info("EXPENSE -- Computed :- " + purchaseComputed + "Previous purchase" + previousPurchases);

                }
            }
        }
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId).build();
    }

    @Override
    public CommandProcessingResult generateFinancialRatios(Long loanId, JsonCommand command) {
        final Loan loan = retrieveLoanBy(loanId);

        if (loan.getLoanDecisionState() == null || !loan.status().isSubmittedAndPendingApproval()
                || !loan.getLoanDecisionState().equals(LoanDecisionState.REVIEW_APPLICATION.getValue())) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.not.in.due.diligence.stage.so.cashflow.cannot.be.generated",
                    "Loan is not in Due Diligence Stage so CashFlow cannot be generated");
        }
        LoanFinancialRatioData financialRatioData = this.loanReadPlatformService.retrieveLoanFinancialRatioData(loanId);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId).build();
    }

    private void saveCashFlowProjection(Integer projectionRate, BigDecimal amount, LoanRepaymentScheduleInstallment installment,
            LoanCashFlowData cashFlow) {
        LoanCashFlowProjection projection = new LoanCashFlowProjection(installment.getInstallmentNumber(), cashFlow.getId(), projectionRate, amount);
        this.loanCashFlowProjectionRepository.saveAndFlush(projection);
    }

    @NotNull
    private BigDecimal computeProjection(Integer projectionRate, BigDecimal amount) {
        return amount.add(BigDecimal.valueOf(projectionRate).divide(BigDecimal.valueOf(100)).multiply(amount));
    }

    @Transactional
    private CommandProcessingResult approveLoanApplicationAssociatedToGLIM(final Long loanId, final JsonCommand command) {
        final Loan loan = retrieveLoanBy(loanId);
        if (loan.status().isRejected()) {
            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(loan.getId()) //
                    .withOfficeId(loan.getOfficeId()) //
                    .withClientId(loan.getClientId()) //
                    .withGroupId(loan.getGroupId()) //
                    .withLoanId(loanId).build();
        }
        return approveApplication(loanId, command, Boolean.TRUE);

    }

    public void validateCashFlowType(List<LoanCashFlowData> cashFlowData, String particularType) {
        long particular = cashFlowData.stream().filter(data -> particularType.equals(data.getParticularType())).count();

        if (particular != 1 && particularType.equals("Sales Income")) {
            throw new GeneralPlatformDomainRuleException("error.msg.cashflow.expects.only.one.sales-income",
                    "There should be exactly one sales-income type in the cash flow data.");
        } else if (particular != 1 && particularType.equals("Purchases")) {
            throw new GeneralPlatformDomainRuleException("error.msg.cashflow.expects.only.one.Purchases",
                    "There should be exactly one Purchases type in the cash flow data.");
        }
    }

}
