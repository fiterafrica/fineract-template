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
package org.apache.fineract.portfolio.savings.data;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload published to ActiveMQ (FDActivationConfirmation queue) when a fixed deposit account is activated. Maps to
 * the Archer investment_activated event (transaction_type = investment_deposit).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FDActivationNotificationData {

    private Long fixedDepositAccountId;
    private Long clientId;
    private String externalId;
    private String clientName;
    private BigDecimal depositAmount;
    private String currency;
    private String transactionType;
    private String activationDate;
    private String maturityDate;
    private BigDecimal maturityAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private Integer tenureDays;
    private Long productId;
    private String productType;
}
