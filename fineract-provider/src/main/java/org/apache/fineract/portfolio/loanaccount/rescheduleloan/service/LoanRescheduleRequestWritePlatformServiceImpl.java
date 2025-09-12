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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.account.service.AccountTransfersWritePlatformService;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.ChangedTransactionDetail;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanAccountDomainService;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanLifecycleStateMachine;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentReminderRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallmentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleTransactionProcessorFactory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRescheduleRequestToTermVariationMapping;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanSummaryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTermVariationType;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTermVariations;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.LoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleDTO;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.DefaultScheduledDateGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanApplicationTerms;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanRepaymentScheduleHistory;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanRepaymentScheduleHistoryRepository;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGeneratorFactory;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.RescheduleLoansApiConstants;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.ChargeHandlingOption;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.LoanRescheduleRequestDataValidator;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequestRepository;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.exception.LoanRescheduleRequestNotFoundException;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanaccount.service.LoanUtilService;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformService;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanRescheduleRequestWritePlatformServiceImpl implements LoanRescheduleRequestWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(LoanRescheduleRequestWritePlatformServiceImpl.class);

    private final CodeValueRepositoryWrapper codeValueRepositoryWrapper;
    private final PlatformSecurityContext platformSecurityContext;
    private final LoanRescheduleRequestDataValidator loanRescheduleRequestDataValidator;
    private final LoanRescheduleRequestRepository loanRescheduleRequestRepository;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository;
    private final LoanRepaymentScheduleHistoryRepository loanRepaymentScheduleHistoryRepository;
    private final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService;
    private final LoanTransactionRepository loanTransactionRepository;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanAssembler loanAssembler;
    private final LoanUtilService loanUtilService;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;
    private final LoanScheduleGeneratorFactory loanScheduleFactory;
    private final LoanSummaryWrapper loanSummaryWrapper;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final DefaultScheduledDateGenerator scheduledDateGenerator = new DefaultScheduledDateGenerator();
    private final LoanAccountDomainService loanAccountDomainService;
    private final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository;

    private final NoteRepository noteRepository;
    private final LoanRepaymentReminderRepository loanRepaymentReminderRepository;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final LoanChargeRepository loanChargeRepository;
    private final ChargeRepositoryWrapper chargeRepositoryWrapper;
    private final LoanWritePlatformService loanWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;


    /**
     * create a new instance of the LoanRescheduleRequest object from the JsonCommand object and persist
     *
     * @return CommandProcessingResult object
     **/
    @Override
    @Transactional
    public CommandProcessingResult create(JsonCommand jsonCommand) {

        try {
            // get the loan id from the JsonCommand object
            final Long loanId = jsonCommand.longValueOfParameterNamed(RescheduleLoansApiConstants.loanIdParamName);

            // use the loan id to get a Loan entity object
            final Loan loan = this.loanAssembler.assembleFrom(loanId);

            // validate the request in the JsonCommand object passed as
            // parameter
            this.loanRescheduleRequestDataValidator.validateForCreateAction(jsonCommand, loan);

            // get the reschedule reason code value id from the JsonCommand
            // object
            final Long rescheduleReasonId = jsonCommand.longValueOfParameterNamed(RescheduleLoansApiConstants.rescheduleReasonIdParamName);

            // use the reschedule reason code value id to get a CodeValue entity
            // object
            final CodeValue rescheduleReasonCodeValue = this.codeValueRepositoryWrapper.findOneWithNotFoundDetection(rescheduleReasonId);

            // get the grace on principal integer value from the JsonCommand
            // object
            final Integer graceOnPrincipal = jsonCommand
                    .integerValueOfParameterNamed(RescheduleLoansApiConstants.graceOnPrincipalParamName);

            // get the grace on interest integer value from the JsonCommand
            // object
            final Integer graceOnInterest = jsonCommand.integerValueOfParameterNamed(RescheduleLoansApiConstants.graceOnInterestParamName);

            // get the extra terms to be added at the end of the new schedule
            // from the JsonCommand object
            final Integer extraTerms = jsonCommand.integerValueOfParameterNamed(RescheduleLoansApiConstants.extraTermsParamName);

            // get the new interest rate that would be applied to the new loan
            // schedule
            final BigDecimal interestRate = jsonCommand
                    .bigDecimalValueOfParameterNamed(RescheduleLoansApiConstants.newInterestRateParamName);

            // get the reschedule reason comment text from the JsonCommand
            // object
            final String rescheduleReasonComment = jsonCommand
                    .stringValueOfParameterNamed(RescheduleLoansApiConstants.rescheduleReasonCommentParamName);

            // get the recalculate interest option
            final Boolean recalculateInterest = jsonCommand
                    .booleanObjectValueOfParameterNamed(RescheduleLoansApiConstants.recalculateInterestParamName);

            final LocalDate endDate = jsonCommand.localDateValueOfParameterNamed(RescheduleLoansApiConstants.endDateParamName);
            final BigDecimal emi = jsonCommand.bigDecimalValueOfParameterNamed(RescheduleLoansApiConstants.emiParamName);

            // initialize set the value to null
            LocalDate submittedOnDate = null;

            BigDecimal newPrincipalDueFixedAmount = null;

            if (jsonCommand.hasParameter(RescheduleLoansApiConstants.newPrincipalDueFixedAmount)) {

                newPrincipalDueFixedAmount = jsonCommand
                        .bigDecimalValueOfParameterNamed(RescheduleLoansApiConstants.newPrincipalDueFixedAmount);
            }

            // check if the parameter is in the JsonCommand object
            if (jsonCommand.hasParameter(RescheduleLoansApiConstants.submittedOnDateParamName)) {
                // create a LocalDate object from the "submittedOnDate" Date
                // string
                submittedOnDate = jsonCommand.localDateValueOfParameterNamed(RescheduleLoansApiConstants.submittedOnDateParamName);
            }

            // initially set the value to null
            LocalDate rescheduleFromDate = null;

            // start point of the rescheduling exercise
            Integer rescheduleFromInstallment = null;

            // initially set the value to null
            LocalDate adjustedDueDate = null;

            // check if the parameter is in the JsonCommand object
            if (jsonCommand.hasParameter(RescheduleLoansApiConstants.rescheduleFromDateParamName)) {
                // create a LocalDate object from the "rescheduleFromDate" Date
                // string
                LocalDate localDate = jsonCommand.localDateValueOfParameterNamed(RescheduleLoansApiConstants.rescheduleFromDateParamName);

                if (localDate != null) {
                    // get installment by due date
                    LoanRepaymentScheduleInstallment installment = loan.getRepaymentScheduleInstallment(localDate);
                    rescheduleFromInstallment = installment.getInstallmentNumber();

                    // update the value of the "rescheduleFromDate" variable
                    rescheduleFromDate = localDate;
                }
            }

            if (jsonCommand.hasParameter(RescheduleLoansApiConstants.adjustedDueDateParamName)) {
                // create a LocalDate object from the "adjustedDueDate" Date
                // string
                adjustedDueDate = jsonCommand.localDateValueOfParameterNamed(RescheduleLoansApiConstants.adjustedDueDateParamName);
            }

            final LoanRescheduleRequest loanRescheduleRequest = LoanRescheduleRequest.instance(loan,
                    LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(), rescheduleFromInstallment, rescheduleFromDate,
                    recalculateInterest, rescheduleReasonCodeValue, rescheduleReasonComment, submittedOnDate,
                    this.platformSecurityContext.authenticatedUser(), null, null, null, null);

            // update reschedule request to term variations mapping
            List<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings = new ArrayList<>();
            final Boolean isActive = false;
            final boolean isSpecificToInstallment = false;
            BigDecimal decimalValue = null;
            LocalDate dueDate = null;
            // create term variations for flat and declining balance loans
            createLoanTermVariationsForRegularLoans(loan, graceOnPrincipal, graceOnInterest, extraTerms, interestRate, rescheduleFromDate,
                    adjustedDueDate, loanRescheduleRequest, loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment,
                    decimalValue, dueDate, endDate, emi, newPrincipalDueFixedAmount);

            // create a new entry in the m_loan_reschedule_request table
            // read optional parameter from request
            final JsonElement chargeHandlingObject = jsonCommand.jsonElement(RescheduleLoansApiConstants.overdueChargeHandlingParamName);

            final JsonObject chargeHandlingjson = chargeHandlingObject.getAsJsonObject();

            String chargeHandlingParam = chargeHandlingjson.get("name").getAsString();

            String chargeHandlingEnum;

            if (RescheduleLoansApiConstants.WAIVE_CHARGES.equalsIgnoreCase(chargeHandlingParam))
                chargeHandlingEnum = "WAIVE";
            else if  (RescheduleLoansApiConstants.CARRY_CHARGES_FORWARD.equalsIgnoreCase(chargeHandlingParam))
                chargeHandlingEnum = "CARRY";
            else chargeHandlingEnum = "";


            ChargeHandlingOption chargeHandling = ChargeHandlingOption.fromString(chargeHandlingEnum);

            Long carryForwardChargeId = jsonCommand.longValueOfParameterNamed(
                    RescheduleLoansApiConstants.carryForwardChargeIdParamName);
            LocalDate carryForwardChargeDueDate = jsonCommand.localDateValueOfParameterNamed(
                    RescheduleLoansApiConstants.carryForwardChargeDueDateParamName);

            loanRescheduleRequest.setChargeHandling(chargeHandling);
            loanRescheduleRequest.setCarryForwardChargeId(carryForwardChargeId);
            loanRescheduleRequest.setCarryForwardChargeDueDate(carryForwardChargeDueDate);

            this.loanRescheduleRequestRepository.saveAndFlush(loanRescheduleRequest);

            return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).withEntityId(loanRescheduleRequest.getId())
                    .withLoanId(loan.getId()).withClientId(loan.getClientId()).withOfficeId(loan.getOfficeId())
                    .withGroupId(loan.getGroupId()).build();
        }

        catch (final JpaSystemException | DataIntegrityViolationException dve) {
            // handle the data integrity violation
            handleDataIntegrityViolation(dve);

            // return an empty command processing result object
            return CommandProcessingResult.empty();
        }
    }

    private void createLoanTermVariationsForRegularLoans(final Loan loan, final Integer graceOnPrincipal, final Integer graceOnInterest,
            final Integer extraTerms, final BigDecimal interestRate, LocalDate rescheduleFromDate, LocalDate adjustedDueDate,
            final LoanRescheduleRequest loanRescheduleRequest,
            List<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings, final Boolean isActive,
            final boolean isSpecificToInstallment, BigDecimal decimalValue, LocalDate dueDate, LocalDate endDate, final BigDecimal emi,
            final BigDecimal newPrincipalDueFixedAmount) {

        if (rescheduleFromDate != null && endDate != null && emi != null) {
            createEMIVariation(loan, rescheduleFromDate, loanRescheduleRequest, loanRescheduleRequestToTermVariationMappings, isActive,
                    endDate, emi);
        }

        if (rescheduleFromDate != null && newPrincipalDueFixedAmount != null) {
            createFixedPrincipalVariation(loan, rescheduleFromDate, loanRescheduleRequest, loanRescheduleRequestToTermVariationMappings,
                    isActive, newPrincipalDueFixedAmount);
        }

        if (rescheduleFromDate != null && adjustedDueDate != null) {
            LoanTermVariations parent = null;
            final Integer termType = LoanTermVariationType.DUE_DATE.getValue();
            createLoanTermVariations(loanRescheduleRequest, termType, loan, rescheduleFromDate, adjustedDueDate,
                    loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment, decimalValue, parent);
        }

        if (rescheduleFromDate != null && interestRate != null) {
            LoanTermVariations parent = null;
            final Integer termType = LoanTermVariationType.INTEREST_RATE_FROM_INSTALLMENT.getValue();
            createLoanTermVariations(loanRescheduleRequest, termType, loan, rescheduleFromDate, dueDate,
                    loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment, interestRate, parent);
        }

        if (rescheduleFromDate != null && graceOnPrincipal != null) {
            final Integer termType = LoanTermVariationType.GRACE_ON_PRINCIPAL.getValue();
            LoanTermVariations parent = null;
            parent = createLoanTermVariations(loanRescheduleRequest, termType, loan, rescheduleFromDate, dueDate,
                    loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment, BigDecimal.valueOf(graceOnPrincipal),
                    parent);

            BigDecimal extraTermsBasedOnGracePeriods = BigDecimal.valueOf(graceOnPrincipal);
            createLoanTermVariations(loanRescheduleRequest, LoanTermVariationType.EXTEND_REPAYMENT_PERIOD.getValue(), loan,
                    rescheduleFromDate, dueDate, loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment,
                    extraTermsBasedOnGracePeriods, parent);

        }

        if (rescheduleFromDate != null && graceOnInterest != null) {
            LoanTermVariations parent = null;
            final Integer termType = LoanTermVariationType.GRACE_ON_INTEREST.getValue();
            createLoanTermVariations(loanRescheduleRequest, termType, loan, rescheduleFromDate, dueDate,
                    loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment, BigDecimal.valueOf(graceOnInterest),
                    parent);
        }

        if (rescheduleFromDate != null && extraTerms != null) {
            LoanTermVariations parent = null;
            final Integer termType = LoanTermVariationType.EXTEND_REPAYMENT_PERIOD.getValue();
            createLoanTermVariations(loanRescheduleRequest, termType, loan, rescheduleFromDate, dueDate,
                    loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment, BigDecimal.valueOf(extraTerms),
                    parent);
        }
        loanRescheduleRequest.updateLoanRescheduleRequestToTermVariationMappings(loanRescheduleRequestToTermVariationMappings);
    }

    private void createEMIVariation(Loan loan, LocalDate rescheduleFromDate, LoanRescheduleRequest loanRescheduleRequest,
            List<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings, Boolean isActive,
            LocalDate endDate, BigDecimal emi) {
        LoanTermVariations parent = null;
        LocalDate rescheduleFromLocDate = rescheduleFromDate;
        LocalDate endDateLocDate = endDate;
        final Integer termType = LoanTermVariationType.EMI_AMOUNT.getValue();
        List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
        for (LoanRepaymentScheduleInstallment installment : installments) {
            if (installment.getDueDate().isEqual(rescheduleFromLocDate) || installment.getDueDate().isEqual(endDateLocDate)
                    || (installment.getDueDate().isAfter(rescheduleFromLocDate) && installment.getDueDate().isBefore(endDateLocDate))) {
                createLoanTermVariations(loanRescheduleRequest, termType, loan, installment.getDueDate(), installment.getDueDate(),
                        loanRescheduleRequestToTermVariationMappings, isActive, true, emi, parent);
            }
            if (installment.getDueDate().isAfter(endDateLocDate)) {
                break;
            }
        }
    }

    private void createFixedPrincipalVariation(Loan loan, LocalDate rescheduleFromDate, LoanRescheduleRequest loanRescheduleRequest,
            List<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings, Boolean isActive,
            BigDecimal principalAmount) {
        LoanTermVariations parent = null;
        LocalDate rescheduleFromLocDate = rescheduleFromDate;
        final Integer termType = LoanTermVariationType.PRINCIPAL_DUE_FIXED_AMOUNT.getValue();
        List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
        for (LoanRepaymentScheduleInstallment installment : installments) {
            if (installment.getDueDate().isEqual(rescheduleFromLocDate) || installment.getDueDate().isAfter(rescheduleFromLocDate)) {
                createLoanTermVariations(loanRescheduleRequest, termType, loan, installment.getDueDate(), installment.getDueDate(),
                        loanRescheduleRequestToTermVariationMappings, isActive, true, principalAmount, parent);
            }
        }
    }

    private LoanTermVariations createLoanTermVariations(LoanRescheduleRequest loanRescheduleRequest, final Integer termType,
            final Loan loan, LocalDate rescheduleFromDate, LocalDate adjustedDueDate,
            List<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings, final Boolean isActive,
            final boolean isSpecificToInstallment, final BigDecimal decimalValue, LoanTermVariations parent) {
        LoanTermVariations loanTermVariation = new LoanTermVariations(termType, rescheduleFromDate, decimalValue, adjustedDueDate,
                isSpecificToInstallment, loan, loan.status().getValue(), isActive, parent);
        loanRescheduleRequestToTermVariationMappings
                .add(LoanRescheduleRequestToTermVariationMapping.createNew(loanRescheduleRequest, loanTermVariation));
        return loanTermVariation;
    }

    @Override
    @Transactional
    public CommandProcessingResult approve(JsonCommand jsonCommand) {

        try {
            final Long loanRescheduleRequestId = jsonCommand.entityId();

            final LoanRescheduleRequest loanRescheduleRequest = this.loanRescheduleRequestRepository.findById(loanRescheduleRequestId)
                    .orElseThrow(() -> new LoanRescheduleRequestNotFoundException(loanRescheduleRequestId));

            // validate the request in the JsonCommand object passed as
            // parameter
            this.loanRescheduleRequestDataValidator.validateForApproveAction(jsonCommand, loanRescheduleRequest);

            final AppUser appUser = this.platformSecurityContext.authenticatedUser();
            final Map<String, Object> changes = new LinkedHashMap<>();

            LocalDate approvedOnDate = jsonCommand.localDateValueOfParameterNamed("approvedOnDate");
            final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(jsonCommand.dateFormat())
                    .withLocale(jsonCommand.extractLocale());

            changes.put("locale", jsonCommand.locale());
            changes.put("dateFormat", jsonCommand.dateFormat());
            changes.put("approvedOnDate", approvedOnDate.format(dateTimeFormatter));
            changes.put("approvedByUserId", appUser.getId());

            Loan loan = loanRescheduleRequest.getLoan();
            final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
            final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());

            ChargeHandlingOption chargeHandling = loanRescheduleRequest.getChargeHandling();

            BigDecimal totalChargesOutstanding =  waiveAllOutstandingCharges(loan);

            ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan,
                    loanRescheduleRequest.getRescheduleFromDate());

            Collection<LoanRepaymentScheduleHistory> loanRepaymentScheduleHistoryList = this.loanScheduleHistoryWritePlatformService
                    .createLoanScheduleArchive(loan.getRepaymentScheduleInstallments(), loan, loanRescheduleRequest);

            final LoanApplicationTerms loanApplicationTerms = loan.constructLoanApplicationTerms(scheduleGeneratorDTO);

            LocalDate rescheduleFromDate = null;
            Set<LoanTermVariations> activeLoanTermVariations = loan.getActiveLoanTermVariations();
            LoanTermVariations dueDateVariationInCurrentRequest = loanRescheduleRequest.getDueDateTermVariationIfExists();
            if (dueDateVariationInCurrentRequest != null && activeLoanTermVariations != null) {
                LocalDate fromScheduleDate = dueDateVariationInCurrentRequest.fetchTermApplicaDate();
                LocalDate currentScheduleDate = fromScheduleDate;
                LocalDate modifiedScheduleDate = dueDateVariationInCurrentRequest.fetchDateValue();
                Map<LocalDate, LocalDate> changeMap = new HashMap<>();
                changeMap.put(currentScheduleDate, modifiedScheduleDate);
                for (LoanTermVariations activeLoanTermVariation : activeLoanTermVariations) {
                    if (activeLoanTermVariation.getTermType().isDueDateVariation()
                            && activeLoanTermVariation.fetchDateValue().equals(dueDateVariationInCurrentRequest.fetchTermApplicaDate())) {
                        activeLoanTermVariation.markAsInactive();
                        rescheduleFromDate = activeLoanTermVariation.fetchTermApplicaDate();
                        dueDateVariationInCurrentRequest.setTermApplicableFrom(rescheduleFromDate);
                    } else if (!activeLoanTermVariation.fetchTermApplicaDate().isBefore(fromScheduleDate)) {
                        while (currentScheduleDate.isBefore(activeLoanTermVariation.fetchTermApplicaDate())) {
                            currentScheduleDate = this.scheduledDateGenerator.generateNextRepaymentDate(currentScheduleDate,
                                    loanApplicationTerms, false);
                            modifiedScheduleDate = this.scheduledDateGenerator.generateNextRepaymentDate(modifiedScheduleDate,
                                    loanApplicationTerms, false);
                            changeMap.put(currentScheduleDate, modifiedScheduleDate);
                        }
                        if (changeMap.containsKey(activeLoanTermVariation.fetchTermApplicaDate())) {
                            activeLoanTermVariation.setTermApplicableFrom(changeMap.get(activeLoanTermVariation.fetchTermApplicaDate()));
                        }
                    }
                }
            }
            for (LoanRescheduleRequestToTermVariationMapping mapping : loanRescheduleRequest
                    .getLoanRescheduleRequestToTermVariationMappings()) {
                mapping.getLoanTermVariations().updateIsActive(true);
            }
            BigDecimal annualNominalInterestRate = null;
            List<LoanTermVariationsData> loanTermVariations = new ArrayList<>();
            loan.constructLoanTermVariations(scheduleGeneratorDTO.getFloatingRateDTO(), annualNominalInterestRate, loanTermVariations);
            loanApplicationTerms.getLoanTermVariations().setExceptionData(loanTermVariations);

            /*
             * for (LoanTermVariationsData loanTermVariation :
             * loanApplicationTerms.getLoanTermVariations().getDueDateVariation( )) { if
             * (rescheduleFromDate.isBefore(loanTermVariation. getTermApplicableFrom())) { LocalDate applicableDate =
             * this.scheduledDateGenerator.generateNextRepaymentDate( rescheduleFromDate, loanApplicationTerms, false,
             * loanApplicationTerms.getHolidayDetailDTO()); if
             * (loanTermVariation.getTermApplicableFrom().equals(applicableDate) ) { LocalDate adjustedDate =
             * this.scheduledDateGenerator.generateNextRepaymentDate( adjustedApplicableDate, loanApplicationTerms,
             * false, loanApplicationTerms.getHolidayDetailDTO());
             * loanTermVariation.setApplicableFromDate(adjustedDate); } } }
             */

            final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
            final MathContext mathContext = new MathContext(8, roundingMode);
            final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.loanRepaymentScheduleTransactionProcessorFactory
                    .determineProcessor(loan.transactionProcessingStrategy());
            final LoanScheduleGenerator loanScheduleGenerator = this.loanScheduleFactory.create(loanApplicationTerms.getInterestMethod());
            final LoanLifecycleStateMachine loanLifecycleStateMachine = null;
            loan.setHelpers(loanLifecycleStateMachine, this.loanSummaryWrapper, this.loanRepaymentScheduleTransactionProcessorFactory);
            final LoanScheduleDTO loanSchedule = loanScheduleGenerator.rescheduleNextInstallments(mathContext, loanApplicationTerms, loan,
                    loanApplicationTerms.getHolidayDetailDTO(), loanRepaymentScheduleTransactionProcessor, rescheduleFromDate);

            loan.updateLoanSchedule(loanSchedule.getInstallments());
            loan.recalculateAllCharges();
            ChangedTransactionDetail changedTransactionDetail = loan.processTransactions();

            for (LoanRepaymentScheduleHistory loanRepaymentScheduleHistory : loanRepaymentScheduleHistoryList) {
                this.loanRepaymentScheduleHistoryRepository.save(loanRepaymentScheduleHistory);
            }

            loan.updateRescheduledByUser(appUser);
            loan.updateRescheduledOnDate(DateUtils.getBusinessLocalDate());

            // update the status of the request
            loanRescheduleRequest.approve(appUser, approvedOnDate);

            deleteOverdueInstallmentChargesAssociatedToThisLoanAccount(loan);
            deleteLoanRepaymentRemindersAssociatedToThisLoanAccount(loan);

            if (chargeHandling == ChargeHandlingOption.CARRY) {
                Long carryForwardChargeDefId = loanRescheduleRequest.getCarryForwardChargeId();
                createCarryForwardCharge(loan, carryForwardChargeDefId, loanRescheduleRequest.getCarryForwardChargeDueDate(), jsonCommand, totalChargesOutstanding);
            }

            loan.updateLoanSummaryDerivedFields();

            // clear arrears if any, the job will rerun to update the loan arrears state
            this.jdbcTemplate.update("DELETE FROM m_loan_arrears_aging WHERE loan_id = ?", loan.getId());

            // update the loan object
            saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

            if (changedTransactionDetail != null) {
                for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                    this.loanTransactionRepository.save(mapEntry.getValue());
                    // update loan with references to the newly created
                    // transactions
                    loan.addLoanTransaction(mapEntry.getValue());
                    this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                }
            }
            postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);

            this.loanAccountDomainService.recalculateAccruals(loan, true);

            return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).withEntityId(loanRescheduleRequestId)
                    .withLoanId(loanRescheduleRequest.getLoan().getId()).with(changes).withClientId(loan.getClientId())
                    .withOfficeId(loan.getOfficeId()).withGroupId(loan.getGroupId()).build();
        }

        catch (final JpaSystemException | DataIntegrityViolationException dve) {
            // handle the data integrity violation
            handleDataIntegrityViolation(dve);

            // return an empty command processing result object
            return CommandProcessingResult.empty();
        }
    }

    private void saveAndFlushLoanWithDataIntegrityViolationChecks(final Loan loan) {
        try {
            List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
            for (LoanRepaymentScheduleInstallment installment : installments) {
                if (installment.getId() == null) {
                    this.repaymentScheduleInstallmentRepository.save(installment);
                }
            }
            if (loan.getTotal_extensions() != null) {
                loan.setTotal_extensions(loan.getTotal_extensions() + 1);
            } else {
                loan.setTotal_extensions(1);
            }
            this.loanRepositoryWrapper.saveAndFlush(loan);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.transaction");
            if (realCause.getMessage().toLowerCase().contains("external_id_unique")) {
                baseDataValidator.reset().parameter("externalId").failWithCode("value.must.be.unique");
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors, e);
            }
        }
    }

    private void postJournalEntries(Loan loan, List<Long> existingTransactionIds, List<Long> existingReversedTransactionIds) {
        final MonetaryCurrency currency = loan.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);
        boolean isAccountTransfer = false;
        final Map<String, Object> accountingBridgeData = loan.deriveAccountingBridgeData(applicationCurrency.toData(),
                existingTransactionIds, existingReversedTransactionIds, isAccountTransfer);
        this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData);
    }

    @Override
    @Transactional
    public CommandProcessingResult reject(JsonCommand jsonCommand) {

        try {
            final Long loanRescheduleRequestId = jsonCommand.entityId();

            final LoanRescheduleRequest loanRescheduleRequest = loanRescheduleRequestRepository.findById(loanRescheduleRequestId)
                    .orElseThrow(() -> new LoanRescheduleRequestNotFoundException(loanRescheduleRequestId));

            // validate the request in the JsonCommand object passed as
            // parameter
            this.loanRescheduleRequestDataValidator.validateForRejectAction(jsonCommand, loanRescheduleRequest);

            final AppUser appUser = this.platformSecurityContext.authenticatedUser();
            final Map<String, Object> changes = new LinkedHashMap<>();

            LocalDate rejectedOnDate = jsonCommand.localDateValueOfParameterNamed("rejectedOnDate");
            final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(jsonCommand.dateFormat())
                    .withLocale(jsonCommand.extractLocale());

            changes.put("locale", jsonCommand.locale());
            changes.put("dateFormat", jsonCommand.dateFormat());
            changes.put("rejectedOnDate", rejectedOnDate.format(dateTimeFormatter));
            changes.put("rejectedByUserId", appUser.getId());

            if (!changes.isEmpty()) {
                loanRescheduleRequest.reject(appUser, rejectedOnDate);
                Set<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings = loanRescheduleRequest
                        .getLoanRescheduleRequestToTermVariationMappings();
                for (LoanRescheduleRequestToTermVariationMapping loanRescheduleRequestToTermVariationMapping : loanRescheduleRequestToTermVariationMappings) {
                    loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().markAsInactive();
                }
            }

            return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).withEntityId(loanRescheduleRequestId)
                    .withLoanId(loanRescheduleRequest.getLoan().getId()).with(changes)
                    .withClientId(loanRescheduleRequest.getLoan().getClientId()).withOfficeId(loanRescheduleRequest.getLoan().getOfficeId())
                    .withGroupId(loanRescheduleRequest.getLoan().getGroupId()).build();
        }

        catch (final JpaSystemException | DataIntegrityViolationException dve) {
            // handle the data integrity violation
            handleDataIntegrityViolation(dve);

            // return an empty command processing result object
            return CommandProcessingResult.empty();
        }
    }

    @Override
    public CommandProcessingResult undo(JsonCommand jsonCommand) {
        try {
            this.platformSecurityContext.authenticatedUser();
            this.loanRescheduleRequestDataValidator.validateForUndoAction(jsonCommand);
            final var loanId = jsonCommand.entityId();
            final var loan = loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
            final var loanRescheduleRequests = loanRescheduleRequestRepository.findByLoanIdAndStatusEnum(loanId,
                    LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue());

            if (loanRescheduleRequests.isEmpty()) {
                throw new LoanRescheduleRequestNotFoundException();
            }

            loanRescheduleRequests.stream()
                    .peek(loanRescheduleRequest -> loanRescheduleRequest.getLoanRescheduleRequestToTermVariationMappings().stream()
                            .forEach(loanRescheduleRequestToTermVariationMapping -> loanRescheduleRequestToTermVariationMapping
                                    .getLoanTermVariations().markAsInactive()))
                    .forEach(LoanRescheduleRequest::undo);
            loanRescheduleRequestRepository.saveAllAndFlush(loanRescheduleRequests);
            final var noteText = jsonCommand.stringValueOfParameterNamed(RescheduleLoansApiConstants.notesParamName);
            var note = Note.loanNote(loan, noteText);
            this.noteRepository.save(note);
            return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).withEntityId(loanId).withLoanId(loanId)
                    .build();
        }

        catch (final JpaSystemException | DataIntegrityViolationException dve) {
            // handle the data integrity violation
            handleDataIntegrityViolation(dve);

            // return an empty command processing result object
            return CommandProcessingResult.empty();
        }
    }

    /**
     * handles the data integrity violation exception for loan reschedule write services
     *
     * @param dve
     *            data integrity violation exception
     *
     **/
    private void handleDataIntegrityViolation(final NonTransientDataAccessException dve) {

        LOG.error("Error occured.", dve);

        throw new PlatformDataIntegrityException("error.msg.loan.reschedule.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private void deleteLoanRepaymentRemindersAssociatedToThisLoanAccount(Loan loan) {
        // delete dependencies on m_loan_repayment_reminder associated with this Loan Account
        this.namedParameterJdbcTemplate.getJdbcTemplate().update("DELETE FROM m_loan_repayment_reminder WHERE loan_id = ?", loan.getId());
    }

    private void deleteOverdueInstallmentChargesAssociatedToThisLoanAccount(Loan loan) {
        Long loanId = loan.getId();
        String query = "SELECT loan_charge_id FROM m_loan_overdue_installment_charge WHERE loan_schedule_id IN (SELECT id FROM m_loan_repayment_schedule WHERE loan_id = ? AND completed_derived = false)";
        // Select the chargeIds
        List<Long> chargeIds = this.namedParameterJdbcTemplate.getJdbcTemplate().queryForList(query, Long.class, loanId);
        if (!chargeIds.isEmpty()) {
            Map<String, Object> params = new HashMap<>();
            params.put("chargeIds", chargeIds);
            // Delete loan charges associated with overdue installments
            this.namedParameterJdbcTemplate.update("DELETE FROM m_loan_charge_paid_by WHERE loan_charge_id IN (:chargeIds)", params);
            this.namedParameterJdbcTemplate.update("DELETE FROM m_loan_overdue_installment_charge WHERE loan_charge_id IN (:chargeIds)",
                    params);
            // Delete chargeIds
            this.namedParameterJdbcTemplate.update("DELETE FROM m_loan_charge WHERE id IN (:chargeIds)", params);
        }
    }

    private BigDecimal waiveAllOutstandingCharges(Loan loan) {
        BigDecimal totalChargesOutstanding = BigDecimal.ZERO;

        JsonObject waiveJson = new JsonObject();
        waiveJson.addProperty("locale", "en");
        waiveJson.addProperty("dateFormat", "dd MMMM yyyy");
        final String jsonBody = waiveJson.toString();

        final JsonElement parsedCommand = this.fromApiJsonHelper.parse(jsonBody);

        for (LoanCharge charge : loan.getLoanCharges()) {
            if (charge.isOverdueInstallmentCharge() && charge.isChargePending()) {
                totalChargesOutstanding = totalChargesOutstanding.add(charge.amountOutstanding());
                JsonCommand waiveCommand = JsonCommand.from(
                        jsonBody,                  // raw JSON string
                        parsedCommand,                        // parsed JSON
                        fromApiJsonHelper,           // injected helper
                        "LOANCHARGE",                // entityName (for audit)
                        charge.getId(),                   // resourceId (loanChargeId)
                        null,                             // subresourceId
                        null,                             // groupId
                        loan.getClientId(),               // clientId
                        loan.getId(),                     // loanId
                        null,                             // savingsId
                        null,                             // transactionId
                        null,                             // url
                        loan.productId(),                 // productId
                        null,                             // creditBureauId
                        null                              // organisationCreditBureauId
                );
                loanWritePlatformService.waiveLoanCharge(loan.getId(), charge.getId(), waiveCommand);
            }
        }

        return totalChargesOutstanding;
    }

    private void createCarryForwardCharge(Loan loan, Long carryForwardChargeDefinitionId, LocalDate dueDate, JsonCommand command, BigDecimal totalOutstanding) {

        if (totalOutstanding.compareTo(BigDecimal.ZERO) > 0) {
            Charge carryForwardDef = chargeRepositoryWrapper.findOneWithNotFoundDetection(carryForwardChargeDefinitionId);

            JsonObject fakeJson = new JsonObject();
            fakeJson.addProperty("amount", totalOutstanding);
            fakeJson.addProperty("dueDate", dueDate.toString());
            fakeJson.addProperty("locale", command.locale());
            fakeJson.addProperty("dateFormat", "yyyy-MM-dd");

            JsonCommand fakeCommand = JsonCommand.fromExistingCommand(command, fakeJson);

            LoanCharge carryForwardCharge = LoanCharge.createNewFromJson(loan, carryForwardDef, fakeCommand, dueDate);
            loan.addLoanCharge(carryForwardCharge);
            loanChargeRepository.saveAndFlush(carryForwardCharge);
        }
    }

}
