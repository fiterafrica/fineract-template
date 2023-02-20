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
package org.apache.fineract.portfolio.savings.domain;

import java.io.Serializable;
import java.time.LocalDate;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public final class SavingsWithdrawalScheduleData implements Serializable {

    private final Long savingsAccountId;

    private final EnumOptionData withdrawalFrequency;

    private final LocalDate nextWithdrawalDate;

    public static SavingsWithdrawalScheduleData newInstance(Long savingsAccountId, EnumOptionData withdrawalFrequency,
            LocalDate nextWithdrawalDate) {
        return new SavingsWithdrawalScheduleData(savingsAccountId, withdrawalFrequency, nextWithdrawalDate);
    }

    private SavingsWithdrawalScheduleData(Long savingsAccountId, EnumOptionData withdrawalFrequency, LocalDate nextWithdrawalDate) {
        this.savingsAccountId = savingsAccountId;
        this.withdrawalFrequency = withdrawalFrequency;
        this.nextWithdrawalDate = nextWithdrawalDate;
    }

    public Long getSavingsAccountId() {
        return savingsAccountId;
    }

    public EnumOptionData getWithdrawalFrequency() {
        return withdrawalFrequency;
    }

    public LocalDate getNextWithdrawalDate() {
        return nextWithdrawalDate;
    }
}
