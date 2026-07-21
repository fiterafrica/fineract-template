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
package org.apache.fineract.portfolio.account.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.portfolio.account.data.StandingInstructionData;
import org.springframework.stereotype.Service;

@Service
public class StandingInstructionPaydayDeferralServiceImpl implements StandingInstructionPaydayDeferralService {

    private final ConfigurationDomainService configurationDomainService;

    public StandingInstructionPaydayDeferralServiceImpl(final ConfigurationDomainService configurationDomainService) {
        this.configurationDomainService = configurationDomainService;
    }

    @Override
    public boolean shouldDeferPaydayRepayment(final StandingInstructionData data,
            @SuppressWarnings("unused") final LocalDate installmentDueDate, final LocalDate tenantDate, final LocalTime tenantTime) {
        if (!this.configurationDomainService.isStandingInstructionPaydayDeferralEnabled()) {
            return false;
        }
        if (data == null || tenantDate == null || tenantTime == null) {
            return false;
        }

        final Integer paydayDayOfMonth = this.configurationDomainService.retrieveStandingInstructionPaydayDayOfMonth();
        if (tenantDate.getDayOfMonth() != paydayDayOfMonth) {
            return false;
        }

        final Integer cutoffHour = this.configurationDomainService.retrieveStandingInstructionPaydayCutoffHour();
        if (tenantTime.getHour() >= cutoffHour) {
            return false;
        }

        if (data.toAccountType() == null || !data.toAccountType().isLoanAccount() || data.toAccount() == null) {
            return false;
        }

        final Long loanProductId = data.toAccount().productId();
        if (loanProductId == null) {
            return false;
        }

        final Set<Long> paydayLoanProductIds = this.configurationDomainService.retrieveStandingInstructionPaydayLoanProductIds();
        return paydayLoanProductIds.contains(loanProductId);
    }
}
