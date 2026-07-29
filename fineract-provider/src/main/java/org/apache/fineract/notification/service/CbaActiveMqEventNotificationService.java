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
package org.apache.fineract.notification.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.businessevent.BusinessEventListener;
import org.apache.fineract.portfolio.businessevent.domain.client.ClientCreateBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.client.ClientIdentifierCreateBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.deposit.FixedDepositAccountActivateBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.deposit.FixedDepositAccountCloseBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.deposit.FixedDepositAccountPreClosureBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.deposit.FixedDepositAccountRolloverBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.loan.LoanCreatedBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.savings.transaction.SavingsDepositBusinessEvent;
import org.apache.fineract.portfolio.businessevent.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.client.data.ClientCreationNotificationData;
import org.apache.fineract.portfolio.client.data.ClientFamilyMembersData;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientIdentifier;
import org.apache.fineract.portfolio.client.domain.ClientIdentifierRepository;
import org.apache.fineract.portfolio.client.service.ClientFamilyMembersReadPlatformService;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.loanaccount.data.LoanCreationNotificationData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.savings.SavingsPeriodFrequencyType;
import org.apache.fineract.portfolio.savings.data.FDActivationNotificationData;
import org.apache.fineract.portfolio.savings.data.FDClosureNotificationData;
import org.apache.fineract.portfolio.savings.data.FDRolloverNotificationData;
import org.apache.fineract.portfolio.savings.data.SavingsDepositNotificationData;
import org.apache.fineract.portfolio.savings.domain.FixedDepositAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Publishes the CBA-requested set of business events (client creation, loan creation, savings deposit, and fixed
 * deposit activation/closure/rollover) to ActiveMQ, following the same envelope/queue pattern already used for
 * LoanRepaymentConfirmation (see {@link ActiveMqNotificationDomainServiceImpl}).
 */
@Service
@RequiredArgsConstructor
public class CbaActiveMqEventNotificationService {

    private static final String PERMISSION = "ALL_FUNCTION";

    private final BusinessEventNotifierService businessEventNotifierService;
    private final ActiveMqNotificationDomainServiceImpl activeMqNotificationDomainService;
    private final Environment env;
    private final FromJsonHelper fromJsonHelper;
    private final PlatformSecurityContext context;
    private final ClientIdentifierRepository clientIdentifierRepository;
    private final ClientFamilyMembersReadPlatformService clientFamilyMembersReadPlatformService;

    @PostConstruct
    public void addListeners() {
        businessEventNotifierService.addPostBusinessEventListener(ClientCreateBusinessEvent.class, new ClientCreatedListener());
        businessEventNotifierService.addPostBusinessEventListener(ClientIdentifierCreateBusinessEvent.class,
                new ClientIdentifierCreatedListener());
        businessEventNotifierService.addPostBusinessEventListener(LoanCreatedBusinessEvent.class, new LoanCreatedListener());
        businessEventNotifierService.addPostBusinessEventListener(SavingsDepositBusinessEvent.class, new SavingsDepositListener());
        businessEventNotifierService.addPostBusinessEventListener(FixedDepositAccountActivateBusinessEvent.class,
                new FixedDepositAccountActivateListener());
        businessEventNotifierService.addPostBusinessEventListener(FixedDepositAccountCloseBusinessEvent.class,
                new FixedDepositAccountCloseListener());
        businessEventNotifierService.addPostBusinessEventListener(FixedDepositAccountPreClosureBusinessEvent.class,
                new FixedDepositAccountPreClosureListener());
        businessEventNotifierService.addPostBusinessEventListener(FixedDepositAccountRolloverBusinessEvent.class,
                new FixedDepositAccountRolloverListener());
    }

    private class ClientCreatedListener implements BusinessEventListener<ClientCreateBusinessEvent> {

        @Override
        public void onBusinessEvent(ClientCreateBusinessEvent event) {
            publishClientCreationConfirmation(event.get(), "created");
        }
    }

    private class ClientIdentifierCreatedListener implements BusinessEventListener<ClientIdentifierCreateBusinessEvent> {

        @Override
        public void onBusinessEvent(ClientIdentifierCreateBusinessEvent event) {
            publishClientCreationConfirmation(event.get().getClient(), "identifierAdded");
        }
    }

    private void publishClientCreationConfirmation(Client client, String action) {
        ClientCreationNotificationData data = buildClientCreationNotificationData(client);
        activeMqNotificationDomainService.buildNotification(PERMISSION, "ClientCreationConfirmation", client.getId(),
                fromJsonHelper.toJson(data), action, context.authenticatedUser().getId(), client.officeId(),
                env.getProperty("fineract.activemq.clientCreationQueue"));
    }

    private ClientCreationNotificationData buildClientCreationNotificationData(Client client) {
        List<ClientCreationNotificationData.Identifier> identifiers = new ArrayList<>();
        for (ClientIdentifier identifier : clientIdentifierRepository.findByClient(client)) {
            identifiers.add(new ClientCreationNotificationData.Identifier(identifier.documentType().label(), identifier.documentKey()));
        }

        List<ClientCreationNotificationData.FamilyMember> familyMembers = new ArrayList<>();
        Collection<ClientFamilyMembersData> clientFamilyMembers = clientFamilyMembersReadPlatformService
                .getClientFamilyMembers(client.getId());
        for (ClientFamilyMembersData familyMember : clientFamilyMembers) {
            familyMembers.add(new ClientCreationNotificationData.FamilyMember(familyMember.getFirstName(), familyMember.getLastName(),
                    familyMember.getRelationship(), familyMember.getMobileNumber(), familyMember.getEmail(),
                    familyMember.getDateOfBirth() == null ? null : familyMember.getDateOfBirth().toString(), familyMember.getGender(),
                    familyMember.getProfession(), familyMember.getIsDependent()));
        }

        LegalFormLabel legalFormLabel = LegalFormLabel.fromValue(client.getLegalForm());

        return new ClientCreationNotificationData(client.getId(), client.getAccountNumber(), client.getExternalId(),
                client.getDisplayName(), client.getFirstname(), client.getMiddlename(), client.getLastname(), client.mobileNo(),
                client.emailAddress(), client.dateOfBirth() == null ? null : client.dateOfBirth().toString(),
                client.gender() == null ? null : client.gender().label(), legalFormLabel.label,
                client.getSubmittedOnDate() == null ? null : client.getSubmittedOnDate().toString(), client.officeId(), identifiers,
                familyMembers);
    }

    private enum LegalFormLabel {

        PERSON(1, "INDIVIDUAL"), ENTITY(2, "ENTITY"), UNKNOWN(null, null);

        private final Integer value;
        private final String label;

        LegalFormLabel(Integer value, String label) {
            this.value = value;
            this.label = label;
        }

        static LegalFormLabel fromValue(Integer value) {
            if (PERSON.value.equals(value)) {
                return PERSON;
            } else if (ENTITY.value.equals(value)) {
                return ENTITY;
            }
            return UNKNOWN;
        }
    }

    private class LoanCreatedListener implements BusinessEventListener<LoanCreatedBusinessEvent> {

        @Override
        public void onBusinessEvent(LoanCreatedBusinessEvent event) {
            Loan loan = event.get();
            PeriodFrequencyType termPeriodFrequencyType = loan.getTermPeriodFrequencyType() == null ? null
                    : PeriodFrequencyType.fromInt(loan.getTermPeriodFrequencyType());
            LoanCreationNotificationData data = new LoanCreationNotificationData(loan.getId(), loan.getAccountNumber(),
                    loan.getClientId(), loan.getClient() == null ? null : loan.getClient().getDisplayName(), loan.productId(),
                    loan.loanProduct() == null ? null : loan.loanProduct().productName(), loan.getProposedPrincipal(),
                    loan.getSubmittedOnDate() == null ? null : loan.getSubmittedOnDate().toString(),
                    loan.getExpectedDisbursedOnLocalDate() == null ? null : loan.getExpectedDisbursedOnLocalDate().toString(),
                    loan.getExpectedMaturityDate() == null ? null : loan.getExpectedMaturityDate().toString(), loan.getTermFrequency(),
                    termPeriodFrequencyType == null ? null : termPeriodFrequencyType.name());

            activeMqNotificationDomainService.buildNotification(PERMISSION, "LoanCreationConfirmation", loan.getId(),
                    fromJsonHelper.toJson(data), "created", context.authenticatedUser().getId(), loan.getOfficeId(),
                    env.getProperty("fineract.activemq.loanCreationQueue"));
        }
    }

    private class SavingsDepositListener implements BusinessEventListener<SavingsDepositBusinessEvent> {

        @Override
        public void onBusinessEvent(SavingsDepositBusinessEvent event) {
            SavingsAccountTransaction transaction = event.get();
            SavingsDepositNotificationData data = new SavingsDepositNotificationData(transaction.getSavingsAccount().getId(),
                    transaction.getSavingsAccount().clientId(), transaction.getSavingsAccount().getExternalId(),
                    transaction.getSavingsAccount().getClient() == null ? null : transaction.getSavingsAccount().getClient()
                            .getDisplayName(),
                    transaction.getId(), transaction.getAmount(), transaction.getSavingsAccount().getCurrency().getCode(), "credit",
                    transaction.getRunningBalance(transaction.getSavingsAccount().getCurrency()).getAmount(),
                    transaction.getCreatedDate() == null ? null : transaction.getCreatedDate().toString(),
                    transaction.getDateOf() == null ? null : transaction.getDateOf().toString());

            activeMqNotificationDomainService.buildNotification(PERMISSION, "SavingsDepositConfirmation", transaction.getId(),
                    fromJsonHelper.toJson(data), "depositMade", context.authenticatedUser().getId(),
                    transaction.getSavingsAccount().officeId(), env.getProperty("fineract.activemq.savingsDepositQueue"));
        }
    }

    private class FixedDepositAccountActivateListener implements BusinessEventListener<FixedDepositAccountActivateBusinessEvent> {

        @Override
        public void onBusinessEvent(FixedDepositAccountActivateBusinessEvent event) {
            FixedDepositAccount account = event.get();
            FDActivationNotificationData data = new FDActivationNotificationData(account.getId(), account.clientId(),
                    account.getExternalId(), account.getClient() == null ? null : account.getClient().getDisplayName(),
                    account.getDepositAmount(), account.getCurrency().getCode(), "investment_deposit",
                    account.getActivationLocalDate() == null ? null : account.getActivationLocalDate().toString(),
                    account.maturityDate() == null ? null : account.maturityDate().toString(), account.maturityAmount(),
                    account.getNominalAnnualInterestRate(), tenureMonths(account), tenureDays(account), account.productId(),
                    "FIXED_DEPOSIT");

            activeMqNotificationDomainService.buildNotification(PERMISSION, "FDActivationConfirmation", account.getId(),
                    fromJsonHelper.toJson(data), "activated", context.authenticatedUser().getId(), account.officeId(),
                    env.getProperty("fineract.activemq.fdActivationQueue"));
        }
    }

    private class FixedDepositAccountCloseListener implements BusinessEventListener<FixedDepositAccountCloseBusinessEvent> {

        @Override
        public void onBusinessEvent(FixedDepositAccountCloseBusinessEvent event) {
            FixedDepositAccount account = event.get();
            publishFDClosureConfirmation(account, "MATURITY", "investment_payout", 0L);
        }
    }

    private class FixedDepositAccountPreClosureListener implements BusinessEventListener<FixedDepositAccountPreClosureBusinessEvent> {

        @Override
        public void onBusinessEvent(FixedDepositAccountPreClosureBusinessEvent event) {
            FixedDepositAccount account = event.get();
            Long daysRemaining = account.getClosedOnDate() == null || account.maturityDate() == null ? null
                    : ChronoUnit.DAYS.between(account.getClosedOnDate(), account.maturityDate());
            publishFDClosureConfirmation(account, "PREMATURE", "liquidation", daysRemaining);
        }
    }

    private void publishFDClosureConfirmation(FixedDepositAccount account, String closureType, String transactionType,
            Long daysRemainingAtClosure) {
        FDClosureNotificationData data = new FDClosureNotificationData(account.getId(), account.clientId(), account.getExternalId(),
                account.getClient() == null ? null : account.getClient().getDisplayName(), account.getAccountBalance(),
                account.getSummary() == null ? null : account.getSummary().getTotalInterestEarned(), account.getCurrency().getCode(),
                transactionType, account.getClosedOnDate() == null ? null : account.getClosedOnDate().toString(), closureType,
                daysRemainingAtClosure, account.productId(), "FIXED_DEPOSIT");

        activeMqNotificationDomainService.buildNotification(PERMISSION, "FDClosureConfirmation", account.getId(),
                fromJsonHelper.toJson(data), "closed", context.authenticatedUser().getId(), account.officeId(),
                env.getProperty("fineract.activemq.fdClosureQueue"));
    }

    private class FixedDepositAccountRolloverListener implements BusinessEventListener<FixedDepositAccountRolloverBusinessEvent> {

        @Override
        public void onBusinessEvent(FixedDepositAccountRolloverBusinessEvent event) {
            FixedDepositAccount oldAccount = event.get().getOldAccount();
            FixedDepositAccount newAccount = event.get().getNewAccount();

            FDRolloverNotificationData data = new FDRolloverNotificationData(oldAccount.getId(), newAccount.getId(),
                    oldAccount.clientId(), oldAccount.getExternalId(),
                    oldAccount.getClient() == null ? null : oldAccount.getClient().getDisplayName(), oldAccount.getAccountBalance(),
                    oldAccount.getCurrency().getCode(), "investment_deposit",
                    oldAccount.getClosedOnDate() == null ? null : oldAccount.getClosedOnDate().toString(),
                    newAccount.maturityDate() == null ? null : newAccount.maturityDate().toString(), oldAccount.productId(),
                    "FIXED_DEPOSIT");

            activeMqNotificationDomainService.buildNotification(PERMISSION, "FDRolloverConfirmation", oldAccount.getId(),
                    fromJsonHelper.toJson(data), "rolledOver", context.authenticatedUser().getId(), oldAccount.officeId(),
                    env.getProperty("fineract.activemq.fdRolloverQueue"));
        }
    }

    private static Integer tenureMonths(FixedDepositAccount account) {
        if (account.depositPeriodFrequencyType() == SavingsPeriodFrequencyType.MONTHS) {
            return account.depositPeriod();
        }
        return null;
    }

    private static Integer tenureDays(FixedDepositAccount account) {
        LocalDate start = account.getActivationLocalDate();
        LocalDate end = account.maturityDate();
        if (start == null || end == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(start, end);
    }
}
