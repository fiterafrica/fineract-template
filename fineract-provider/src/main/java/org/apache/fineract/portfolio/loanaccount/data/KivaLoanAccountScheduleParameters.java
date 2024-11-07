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
package org.apache.fineract.portfolio.loanaccount.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

public class KivaLoanAccountScheduleParameters {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date firstRepaymentDate;
    private Integer nInstallments;
    private Integer nRepeat;
    private String nRepeatUnit;
    private String interestRateType;
    private String annualRate;

    public KivaLoanAccountScheduleParameters(Date firstRepaymentDate, Integer nInstallments, Integer nRepeat, String nRepeatUnit,
            String interestRateType, String annualRate) {
        this.firstRepaymentDate = firstRepaymentDate;
        this.nInstallments = nInstallments;
        this.nRepeat = nRepeat;
        this.nRepeatUnit = nRepeatUnit;
        this.interestRateType = getInterestRateType(interestRateType);
        this.annualRate = annualRate;
    }

    @Override
    public String toString() {
        return "KivaLoanAccountScheduleParameters{" + "firstRepaymentDate=" + firstRepaymentDate + ", nInstallments=" + nInstallments
                + ", nRepeat=" + nRepeat + ", nRepeatUnit=" + nRepeatUnit + ", interestRateType=" + interestRateType + ", annualRate="
                + annualRate + '}';
    }

    public String getInterestRateType(String interestRateType) {
        if (interestRateType.equals("Declining Balance")) {
            interestRateType = "decliningBalance";
        } else if (interestRateType.equals("Flat")) {
            interestRateType = "flat";
        }
        return interestRateType;
    }
}
