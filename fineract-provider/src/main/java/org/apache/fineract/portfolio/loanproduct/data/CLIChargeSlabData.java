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
package org.apache.fineract.portfolio.loanproduct.data;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

/**
 * DTO for CLI (Credit Life Insurance) Charge Slab configuration.
 * Used in loan product API requests and responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CLIChargeSlabData implements Serializable {

    private Long id;
    private Long chargeId;
    private String chargeName;
    private Integer fromPeriod;
    private Integer toPeriod;
    private EnumOptionData periodType;
    private BigDecimal amountRangeFrom;
    private BigDecimal amountRangeTo;
    private BigDecimal rate;

    public static CLIChargeSlabData instance(Long id, Long chargeId, String chargeName,
            Integer fromPeriod, Integer toPeriod, EnumOptionData periodType,
            BigDecimal amountRangeFrom, BigDecimal amountRangeTo, BigDecimal rate) {
        return CLIChargeSlabData.builder()
                .id(id)
                .chargeId(chargeId)
                .chargeName(chargeName)
                .fromPeriod(fromPeriod)
                .toPeriod(toPeriod)
                .periodType(periodType)
                .amountRangeFrom(amountRangeFrom)
                .amountRangeTo(amountRangeTo)
                .rate(rate)
                .build();
    }
}

