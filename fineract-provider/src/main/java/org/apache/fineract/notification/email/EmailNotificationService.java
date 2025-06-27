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

package org.apache.fineract.notification.email;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.domain.EmailDetail;
import org.apache.fineract.infrastructure.core.service.GmailBackedPlatformEmailService;
import org.apache.fineract.portfolio.businessevent.BusinessEventListener;
import org.apache.fineract.portfolio.businessevent.domain.loan.LoanDecisionAcceptedEvent;
import org.apache.fineract.portfolio.businessevent.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecision;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDecisionState;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationService {

    private final GmailBackedPlatformEmailService emailService;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final AppUserRepository appUserRepository;

    @Value("${mifos.system.base-url}")
    private String baseUrl;
    @PostConstruct
    public void addListeners() {
        businessEventNotifierService.addPostBusinessEventListener(LoanDecisionAcceptedEvent.class,
                new EmailNotificationService.LoanDecisionAcceptedListener());
    }

    public void sendLoanDecisionNotification(Loan loan, LoanDecision decision) {
        Integer nextStage = decision.getNextLoanIcReviewDecisionState();
        if (nextStage == null) return;

        AppUser nextApprover = getNextApprover(decision, nextStage);

        if (nextApprover != null && StringUtils.isNotBlank(nextApprover.getEmail())) {
            EmailDetail emailDetail;
            if (decision.getLoanDecisionState().equals(LoanDecisionState.PREPARE_AND_SIGN_CONTRACT.getValue())){
                emailDetail = getLoanOfficerEmail(loan, nextStage, nextApprover);
            }else {
                emailDetail = getLoanDecisionApproverEmail(loan, nextStage, nextApprover);
            }
            emailService.sendDefinedEmail(emailDetail);
        }
    }

    private AppUser getNextApprover(LoanDecision decision, Integer stage) {
        return switch (LoanDecisionState.fromInt(stage)) {
            case IC_REVIEW_LEVEL_ONE -> decision.getIcReviewDecisionLevelOneBy();
            case IC_REVIEW_LEVEL_TWO -> decision.getIcReviewDecisionLevelTwoBy();
            case IC_REVIEW_LEVEL_THREE -> decision.getIcReviewDecisionLevelThreeBy();
            case IC_REVIEW_LEVEL_FOUR -> decision.getIcReviewDecisionLevelFourBy();
            case IC_REVIEW_LEVEL_FIVE -> decision.getIcReviewDecisionLevelFiveBy();
            case PREPARE_AND_SIGN_CONTRACT -> this.appUserRepository.findAppUserByStaff(decision.getLoan().getLoanOfficer());
            default -> null;
        };
    }

    @NotNull
    private EmailDetail getLoanDecisionApproverEmail(Loan loan, Integer nextStage, AppUser nextApprover) {
        String loanUrl = this.baseUrl + "/viewloanaccount/" + loan.getId();
        String subject = "Loan Approval Required: Stage " + LoanDecisionState.fromInt(nextStage).toString();
        String body = String.format(
                """
                        Dear %s,<br><br>

                        A business loan request for account <strong>%s</strong>, client <strong>%s</strong>, is awaiting your approval.<br><br>

                        Please <a href="%s">log in </a> to the system to review and take the next action.<br><br>
                        
                        Kind Regards.
                """,
                nextApprover.getDisplayName(),
                loan.getAccountNumber(),
                loan.getClient().getDisplayName(),
                loanUrl
        );
        return new EmailDetail(subject,body, nextApprover.getEmail(), nextApprover.getDisplayName());
    }

    @NotNull
    private EmailDetail getLoanOfficerEmail(Loan loan, Integer nextStage, AppUser nextApprover) {
        String loanUrl = this.baseUrl + "/viewloanaccount/" + loan.getId();
        String subject = "Loan Approval Required: Stage " + LoanDecisionState.fromInt(nextStage).toString();
        String body = String.format(
                """
                        Dear %s,<br><br>

                        A business loan for account <strong>%s</strong>, client <strong>%s</strong>, has been approved.<br><br>

                        Please assign it to the appropriate person for the next stage of processing.<br><br>
                        
                        Kind Regards.
                """,
                nextApprover.getDisplayName(),
                loan.getAccountNumber(),
                loan.getClient().getDisplayName(),
                loanUrl
        );
        return new EmailDetail(subject,body, nextApprover.getEmail(), nextApprover.getDisplayName());
    }


    private class LoanDecisionAcceptedListener implements BusinessEventListener<LoanDecisionAcceptedEvent> {


        @Override
        public void onBusinessEvent(LoanDecisionAcceptedEvent event) {
            Loan loan = event.get();
            LoanDecision loanDecision = event.getLoanDecision();
            sendLoanDecisionNotification(loan,loanDecision);
        }
    }
}


