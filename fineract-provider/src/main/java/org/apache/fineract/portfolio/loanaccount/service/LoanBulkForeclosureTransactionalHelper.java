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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandProcessingService;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.springframework.stereotype.Service;

/**
 * Helper service for bulk foreclosure that executes foreclosure and records to audit trail.
 *
 * Note: Bulk foreclosure: 1. Requires BULKFORECLOSURE_LOAN permission at the bulk operation level 2. Records each loan
 * foreclosure to m_portfolio_command_source for audit trail (same as individual foreclosure) 3. Bypasses maker-checker
 * by setting isApprovedByChecker=true to ensure immediate execution 4. Also maintains audit in m_bulk_foreclosure_job
 * and m_bulk_foreclosure_job_detail tables
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoanBulkForeclosureTransactionalHelper {

    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final CommandProcessingService commandProcessingService;
    private final FromJsonHelper fromApiJsonHelper;

    private static final String DATE_FORMAT = "dd MMMM yyyy";
    private static final String LOCALE = "en";

    /**
     * Forecloses a single loan with audit trail in m_portfolio_command_source. Bypasses maker-checker to ensure
     * immediate execution.
     *
     * @param loanId
     *            the loan ID to foreclose
     * @param foreclosureDate
     *            the foreclosure date
     * @return null if successful, error message if validation fails
     */
    public String forecloseSingleLoan(Long loanId, LocalDate foreclosureDate) {
        // Pre-validation before calling command to avoid unnecessary command source entries for invalid loans
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

        // Validate loan is active
        if (!loan.status().isActive()) {
            return "Loan is not in active state. Current status: " + loan.status().toString();
        }

        // Validate foreclosure date is not in the future
        if (org.apache.fineract.infrastructure.core.service.DateUtils.isDateInTheFuture(foreclosureDate)) {
            return "Foreclosure date cannot be in the future";
        }

        // Validate foreclosure date is not before disbursement date
        if (foreclosureDate.isBefore(loan.getDisbursementDate())) {
            return "Foreclosure date cannot be before disbursement date: " + loan.getDisbursementDate();
        }

        // Validate foreclosure date is not before the last user transaction date (backdated foreclosure validation)
        LocalDate lastUserTransactionDate = loan.getLastUserTransactionDate();
        if (lastUserTransactionDate != null && lastUserTransactionDate.isAfter(foreclosureDate)) {
            return "Foreclosure date cannot be before the last transaction date: " + lastUserTransactionDate;
        }

        // Validate interest recalculation is not enabled
        if (loan.isInterestRecalculationEnabledForProduct()) {
            return "Loan with interest recalculation enabled cannot be foreclosed";
        }

        // Validate no undisbursed tranches before foreclosure date
        for (var dd : loan.getDisbursementDetails()) {
            if (!dd.expectedDisbursementDateAsLocalDate().isAfter(foreclosureDate) && dd.actualDisbursementDate() == null) {
                return "Loan has undisbursed tranche before foreclosure date";
            }
        }

        // Build JSON request for foreclosure command
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("transactionDate", foreclosureDate.format(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        jsonRequest.addProperty("dateFormat", DATE_FORMAT);
        jsonRequest.addProperty("locale", LOCALE);
        jsonRequest.addProperty("note", "Bulk foreclosure");

        final String json = jsonRequest.toString();

        // Build CommandWrapper for foreclosure
        final CommandWrapper wrapper = new CommandWrapperBuilder().loanForeclosure(loanId).withJson(json).build();

        // Parse JSON to create JsonCommand
        final JsonElement parsedCommand = this.fromApiJsonHelper.parse(json);
        final JsonCommand command = JsonCommand.from(json, parsedCommand, this.fromApiJsonHelper, wrapper.getEntityName(),
                wrapper.getEntityId(), wrapper.getSubentityId(), wrapper.getGroupId(), wrapper.getClientId(), wrapper.getLoanId(),
                wrapper.getSavingsId(), wrapper.getTransactionId(), wrapper.getHref(), wrapper.getProductId(), wrapper.getCreditBureauId(),
                wrapper.getOrganisationCreditBureauId());

        try {
            // Execute foreclosure with isApprovedByChecker=true to bypass maker-checker
            // This ensures immediate execution AND records to m_portfolio_command_source
            CommandProcessingResult result = this.commandProcessingService.processAndLogCommand(wrapper, command, true);
            log.debug("Successfully foreclosed loan {} with resource ID: {}", loanId, result.resourceId());
            return null;
        } catch (Exception e) {
            log.error("Error foreclosing loan {}: {}", loanId, e.getMessage());
            return e.getMessage() != null ? e.getMessage() : "Unknown error during foreclosure";
        }
    }
}
