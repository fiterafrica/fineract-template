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
 * Payload published to ActiveMQ (FDRolloverConfirmation queue) when a fixed deposit account is rolled over into a
 * new term. Maps to the Archer investment_rolled_over event (transaction_type = investment_deposit).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FDRolloverNotificationData {

    private Long originalFixedDepositAccountId;
    private Long newFixedDepositAccountId;
    private Long clientId;
    private String externalId;
    private String clientName;
    private BigDecimal rolledOverAmount;
    private String currency;
    private String transactionType;
    private String rolloverDate;
    private String newMaturityDate;
    private Long productId;
    private String productType;
}
