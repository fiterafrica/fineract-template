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
package org.apache.fineract.portfolio.savings;

public enum WithdrawalFrequency {
    MONTHLY(0,"withdrawal.frequency.monthly"),
    QUARTERLY(1,"withdrawal.frequency.quarterly"),
    BI_ANNUAL(2,"withdrawal.frequency.biannual"),
    ANNUAL(3,"withdrawal.frequency.annual");

    private final Integer value;
    private final String code;

    WithdrawalFrequency(Integer value, String code) {
        this.value = value;
        this.code = code;
    }

    public static WithdrawalFrequency fromInt(int value) {
        switch (value) {
            case 0:
                return MONTHLY;
            case 1:
                return QUARTERLY;
            case 2:
                return BI_ANNUAL;
            case 3:
                return ANNUAL;
            default:
                throw new IllegalArgumentException("Invalid integer value for WithdrawalFrequency: " + value);
        }
    }

}
