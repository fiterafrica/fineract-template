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
package org.apache.fineract.portfolio.loanproduct.service;

import java.math.BigDecimal;
import java.util.Collection;
import org.apache.fineract.portfolio.loanproduct.data.CLIChargeSlabData;

public interface CLIChargeSlabReadPlatformService {

    /**
     * Retrieve all CLI slabs for a loan product
     */
    Collection<CLIChargeSlabData> retrieveAllByLoanProductId(Long loanProductId);

    /**
     * Retrieve CLI slabs for a specific charge in a loan product
     */
    Collection<CLIChargeSlabData> retrieveByLoanProductIdAndChargeId(Long loanProductId, Long chargeId);

    /**
     * Resolve the applicable CLI rate based on tenor and loan amount
     * @param loanProductId the loan product
     * @param chargeId the CLI charge
     * @param tenorInMonths loan tenor in months
     * @param loanAmount the loan principal amount
     * @return the applicable CLI rate percentage, or null if no matching slab
     */
    BigDecimal resolveCLIRate(Long loanProductId, Long chargeId, Integer tenorInMonths, BigDecimal loanAmount);
}

