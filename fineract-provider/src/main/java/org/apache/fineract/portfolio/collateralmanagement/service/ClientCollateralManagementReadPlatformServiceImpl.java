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
package org.apache.fineract.portfolio.collateralmanagement.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.collateralmanagement.data.ClientCollateralManagementAdditionalData;
import org.apache.fineract.portfolio.collateralmanagement.data.ClientCollateralManagementData;
import org.apache.fineract.portfolio.collateralmanagement.data.LoanCollateralTemplateData;
import org.apache.fineract.portfolio.collateralmanagement.data.LoanTransactionData;
import org.apache.fineract.portfolio.collateralmanagement.domain.ClientCollateralManagement;
import org.apache.fineract.portfolio.collateralmanagement.domain.ClientCollateralManagementAdditionalDetails;
import org.apache.fineract.portfolio.collateralmanagement.domain.ClientCollateralManagementAdditionalDetailsRepository;
import org.apache.fineract.portfolio.collateralmanagement.domain.ClientCollateralManagementRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCollateralManagement;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanTransactionNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientCollateralManagementReadPlatformServiceImpl implements ClientCollateralManagementReadPlatformService {

    private final PlatformSecurityContext context;
    private final ClientCollateralManagementRepositoryWrapper clientCollateralManagementRepositoryWrapper;

    private final ClientCollateralManagementAdditionalDetailsRepository clientCollateralManagementAdditionalDetailsRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final ConfigurationReadPlatformService configurationReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;

    @Autowired
    public ClientCollateralManagementReadPlatformServiceImpl(final PlatformSecurityContext context,
            final ClientCollateralManagementRepositoryWrapper clientCollateralManagementRepositoryWrapper,
            ClientCollateralManagementAdditionalDetailsRepository clientCollateralManagementAdditionalDetailsRepository,
            final LoanTransactionRepository loanTransactionRepository, ConfigurationReadPlatformService configurationReadPlatformService,
            CodeValueReadPlatformService codeValueReadPlatformService) {
        this.context = context;
        this.clientCollateralManagementRepositoryWrapper = clientCollateralManagementRepositoryWrapper;
        this.clientCollateralManagementAdditionalDetailsRepository = clientCollateralManagementAdditionalDetailsRepository;
        this.loanTransactionRepository = loanTransactionRepository;
        this.configurationReadPlatformService = configurationReadPlatformService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
    }

    @Override
    public List<ClientCollateralManagementData> getClientCollaterals(final Long clientId, final Long prodId) {
        return this.clientCollateralManagementRepositoryWrapper.getClientCollateralData(clientId, prodId);
    }

    @Override
    public List<ClientCollateralManagementData> getClientCollaterals(final Long clientId) {
        final Collection<ClientCollateralManagement> clientCollateralManagements = this.clientCollateralManagementRepositoryWrapper
                .getCollateralsPerClient(clientId);
        final List<ClientCollateralManagementData> clientCollateralManagementDataList = new ArrayList<>();

        for (ClientCollateralManagement clientCollateralManagement : clientCollateralManagements) {
            BigDecimal total = clientCollateralManagement.getTotal();
            BigDecimal totalCollateral = clientCollateralManagement.getTotalCollateral(total);
            clientCollateralManagementDataList
                    .add(ClientCollateralManagementData.instance(clientCollateralManagement.getCollaterals().getName(), clientCollateralManagement.getQuantity(),
                            total, totalCollateral, clientId, null, clientCollateralManagement.getId(),
                            clientCollateralManagement.getCollaterals().getPctToBase(), clientCollateralManagement.getCollaterals().getBasePrice()));
        }

        return clientCollateralManagementDataList;
    }

    @Override
    public List<LoanCollateralTemplateData> getLoanCollateralTemplate(Long clientId) {
        this.context.authenticatedUser();
        Collection<ClientCollateralManagement> clientCollateralManagements = this.clientCollateralManagementRepositoryWrapper
                .getCollateralsPerClient(clientId);
        List<LoanCollateralTemplateData> loanCollateralTemplateDataList = new ArrayList<>();
        for (ClientCollateralManagement clientCollateralManagement : clientCollateralManagements) {
            loanCollateralTemplateDataList.add(LoanCollateralTemplateData.instanceOf(clientCollateralManagement));
        }
        return loanCollateralTemplateDataList;
    }

    @Override
    public ClientCollateralManagementData getClientCollateralManagementData(final Long collateralId) {
        final ClientCollateralManagement clientCollateralManagement = this.clientCollateralManagementRepositoryWrapper
                .getCollateral(collateralId);
        BigDecimal basePrice = clientCollateralManagement.getCollaterals().getBasePrice();
        BigDecimal pctToBase = clientCollateralManagement.getCollaterals().getPctToBase().divide(BigDecimal.valueOf(100));
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalCollateral = BigDecimal.ZERO;
        BigDecimal quantity = clientCollateralManagement.getQuantity();
        if (quantity.compareTo(BigDecimal.ZERO) != 0) {
            total = basePrice.multiply(quantity);
            totalCollateral = total.multiply(pctToBase);
        }
        Set<LoanCollateralManagement> loanCollateralManagementSet = clientCollateralManagement.getLoanCollateralManagementSet();

        List<LoanTransactionData> loanTransactionDataList = new ArrayList<>();
        for (LoanCollateralManagement loanCollateralManagement : loanCollateralManagementSet) {
            if (loanCollateralManagement.getLoanTransaction() != null) {
                Long transactionId = loanCollateralManagement.getLoanTransaction().getId();
                LoanTransaction loanTransaction = this.loanTransactionRepository.findById(transactionId)
                        .orElseThrow(() -> new LoanTransactionNotFoundException(transactionId));
                LoanTransactionData loanTransactionData = LoanTransactionData.instance(loanTransaction.getLoan().getId(),
                        loanTransaction.getCreatedDateTime(), loanTransaction.getOutstandingLoanBalance(),
                        loanTransaction.getPrincipalPortion());
                loanTransactionDataList.add(loanTransactionData);
            }
        }
        ClientCollateralManagementAdditionalDetails details = this.clientCollateralManagementAdditionalDetailsRepository
                .findByCollateralId(clientCollateralManagement);
        ClientCollateralManagementData data = ClientCollateralManagementData.instance(clientCollateralManagement.getCollaterals().getName(),
                clientCollateralManagement.getQuantity(), total, totalCollateral, clientCollateralManagement.getClient().getId(),
                loanTransactionDataList, clientCollateralManagement.getId(), clientCollateralManagement.getCollaterals().getPctToBase(),
                basePrice);
        final GlobalConfigurationPropertyData clientCollateralAdditionalDataConfig = this.configurationReadPlatformService
                .retrieveGlobalConfiguration("Enable-Client-Collateral-Addition_Details");
        final Boolean isClientCollateralAdditionalDataConfigEnable = clientCollateralAdditionalDataConfig.isEnabled();
        data.setAdditionalDetailsEnabled(isClientCollateralAdditionalDataConfigEnable);
        if (details != null) {
            data.setAdditionalDetails(prepareAdditionalData(details));
        }

        return data;
    }

    private ClientCollateralManagementAdditionalData prepareAdditionalData(ClientCollateralManagementAdditionalDetails details) {
        CodeValueData province = details.getProvince() != null
                ? CodeValueData.instance(details.getProvince().getId(), details.getProvince().label())
                : null;
        CodeValueData district = details.getDistrict() != null
                ? CodeValueData.instance(details.getDistrict().getId(), details.getDistrict().label())
                : null;
        CodeValueData sector = details.getSector() != null
                ? CodeValueData.instance(details.getSector().getId(), details.getSector().label())
                : null;
        CodeValueData cell = details.getCell() != null ? CodeValueData.instance(details.getCell().getId(), details.getCell().label())
                : null;
        CodeValueData village = details.getVillage() != null
                ? CodeValueData.instance(details.getVillage().getId(), details.getVillage().label())
                : null;

        return ClientCollateralManagementAdditionalData.instance(details, province, district, sector, cell, village);
    }

    @Override
    public ClientCollateralManagementAdditionalData getClientCollateralAdditionalTemplate(final Long clientId) {
        final List<CodeValueData> province = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.PROVINCE));
        final List<CodeValueData> district = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.DISTRICT));
        final List<CodeValueData> sector = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.SECTOR));
        final List<CodeValueData> cell = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.CELL));
        final List<CodeValueData> village = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.VILLAGE));
        return ClientCollateralManagementAdditionalData.template(province, district, sector, cell, village);
    }

}
