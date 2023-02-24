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
package org.apache.fineract.portfolio.savings.service;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.savings.data.SavingsProductFloatingInterestRateApiJsonDeserializer;
import org.apache.fineract.portfolio.savings.domain.SavingsProduct;
import org.apache.fineract.portfolio.savings.domain.SavingsProductAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsProductFloatingInterestRate;
import org.apache.fineract.portfolio.savings.domain.SavingsProductFloatingInterestRateRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsProductRepositoryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SavingsProductFloatingInterestRateWritePlatformServiceImpl implements SavingsProductFloatingInterestRateWritePlatformService {

    private final PlatformSecurityContext context;
    private final SavingsProductFloatingInterestRateRepository savingsProductFloatingInterestRateRepository;
    private final SavingsProductRepositoryWrapper savingsProductRepositoryWrapper;
    private final SavingsProductAssembler savingsProductAssembler;
    private final SavingsProductFloatingInterestRateApiJsonDeserializer fromApiJsonDeserializer;
    private static final Logger LOG = LoggerFactory.getLogger(SavingsProductFloatingInterestRateWritePlatformServiceImpl.class);

    @Override
    public CommandProcessingResult addSavingsProductFloatingInterestRate(Long savingsProductId, JsonCommand command){
        JsonObject jsonObject = command.parsedJson().getAsJsonObject();
        context.authenticatedUser();
        fromApiJsonDeserializer.validateForCreate(savingsProductId, jsonObject.toString());
        final SavingsProduct savingsProduct = savingsProductRepositoryWrapper.findOneWithNotFoundDetection(savingsProductId);
        SavingsProductFloatingInterestRate floatingInterestRate = savingsProductAssembler.assembleSavingsProductFloatingInterestRateFrom(jsonObject,savingsProduct);
        savingsProductFloatingInterestRateRepository.saveAndFlush(floatingInterestRate);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(floatingInterestRate.getId()).build();
    }


    @Override
    public CommandProcessingResult updateSavingsProductFloatingInterestRate(Long floatingInterestRateId, JsonCommand command) {
        return null;
    }
}
