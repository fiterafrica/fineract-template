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
package org.apache.fineract.portfolio.client.service;

import java.util.Map;
import javax.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientOtherInfo;
import org.apache.fineract.portfolio.client.domain.ClientOtherInfoRepository;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.apache.fineract.portfolio.client.exception.ClientOtherInfoAlreadyPresentException;
import org.apache.fineract.portfolio.client.exception.ClientOtherInfoNotFoundException;
import org.apache.fineract.portfolio.client.serialization.ClientOtherInfoCommandFromApiJsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientOtherInfoWritePlatformServiceImpl implements ClientOtherInfoWritePlatformService {

    private final PlatformSecurityContext context;
    private final CodeValueRepositoryWrapper codeValueRepository;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final ClientOtherInfoRepository clientOtherInfoRepository;
    private final ClientOtherInfoCommandFromApiJsonDeserializer fromApiJsonDeserializer;

    private final ConfigurationReadPlatformService configurationReadPlatformService;
    private static final Logger LOG = LoggerFactory.getLogger(ClientOtherInfoWritePlatformServiceImpl.class);

    @Override
    public CommandProcessingResult create(final Long clientId, final JsonCommand command) {

        final ClientOtherInfo clientOtherInfo = this.clientOtherInfoRepository.getByClientId(clientId);
        if (clientOtherInfo != null) {
            throw new ClientOtherInfoAlreadyPresentException(clientId);
        }

        final GlobalConfigurationPropertyData otherInfoConfig = this.configurationReadPlatformService
                .retrieveGlobalConfiguration("Enable-other-client-info");
        final Boolean isClientOtherInfoEnable = otherInfoConfig.isEnabled();

        if (!isClientOtherInfoEnable) {
            throw new GeneralPlatformDomainRuleException("error.msg.enable.client.other.info",
                    "Enable Client Other Info for proceeding operation");
        }
        final Client client = clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);
        fromApiJsonDeserializer.validateForCreate(client.getLegalForm().intValue(), command.json());

        ClientOtherInfo otherInfo = null;

        CodeValue strata = null;
        final Long strataId = command.longValueOfParameterNamed(ClientApiConstants.strataIdParamName);
        if (strataId != null) {
            strata = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(ClientApiConstants.STRATA, strataId);
        }

        if (LegalForm.fromInt(client.getLegalForm().intValue()).isPerson()) {
            CodeValue nationality = null;
            final Long nationalityId = command.longValueOfParameterNamed(ClientApiConstants.nationalityIdParamName);
            if (nationalityId != null) {
                nationality = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection("COUNTRY", nationalityId);
            }

            CodeValue yearArrivedInHostCountry = null;
            final Long yearArrivedInHostCountryId = command.longValueOfParameterNamed(ClientApiConstants.yearArrivedInHostCountry);
            if (yearArrivedInHostCountryId != null) {
                yearArrivedInHostCountry = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(
                        ClientApiConstants.YEAR_ARRIVED_IN_HOST_COUNTRY, yearArrivedInHostCountryId);
            }
            otherInfo = ClientOtherInfo.createNew(command, client, strata, nationality, yearArrivedInHostCountry);
        } else if (LegalForm.fromInt(client.getLegalForm().intValue()).isEntity()) {
            otherInfo = ClientOtherInfo.createNewForEntity(command, client, strata);
        }

        ClientOtherInfo info = clientOtherInfoRepository.saveAndFlush(otherInfo);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withClientId(clientId).withEntityId(info.getId())
                .build();
    }

    @Override
    public CommandProcessingResult update(Long otherInfoId, JsonCommand command) {

        try {
            final GlobalConfigurationPropertyData otherInfoConfig = this.configurationReadPlatformService
                    .retrieveGlobalConfiguration("Enable-other-client-info");
            final Boolean isClientOtherInfoEnable = otherInfoConfig.isEnabled();

            if (!isClientOtherInfoEnable) {
                throw new GeneralPlatformDomainRuleException("error.msg.enable.client.other.info",
                        "Enable Client Other Info for proceeding operation");
            }

            final ClientOtherInfo clientOtherInfo = this.clientOtherInfoRepository.findById(otherInfoId)
                    .orElseThrow(() -> new ClientOtherInfoNotFoundException(otherInfoId));
            this.fromApiJsonDeserializer.validateForUpdate(command.json(), clientOtherInfo.getClient().getLegalForm().intValue());
            final Map<String, Object> changes = clientOtherInfo.update(command, clientOtherInfo.getClient().getLegalForm().intValue());

            if (changes.containsKey(ClientApiConstants.strataIdParamName)) {
                final Long strataId = command.longValueOfParameterNamed(ClientApiConstants.strataIdParamName);
                CodeValue strataCodeValue = null;
                if (strataId != null) {

                    strataCodeValue = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(ClientApiConstants.STRATA,
                            strataId);
                }
                clientOtherInfo.setStrata(strataCodeValue);
            }
            if (LegalForm.fromInt(clientOtherInfo.getClient().getLegalForm().intValue()).isPerson()) {
                if (changes.containsKey(ClientApiConstants.nationalityIdParamName)) {
                    final Long nationalityId = command.longValueOfParameterNamed(ClientApiConstants.nationalityIdParamName);
                    CodeValue nationalityCodeValue = null;
                    if (nationalityId != null) {

                        nationalityCodeValue = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection("COUNTRY",
                                nationalityId);
                    }
                    clientOtherInfo.setNationality(nationalityCodeValue);
                }

                if (changes.containsKey(ClientApiConstants.yearArrivedInHostCountry)) {
                    final Long yearArrivedInHostCountryId = command.longValueOfParameterNamed(ClientApiConstants.yearArrivedInHostCountry);
                    CodeValue yearArrivedInHostCountryCodeValue = null;
                    if (yearArrivedInHostCountryId != null) {

                        yearArrivedInHostCountryCodeValue = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(
                                ClientApiConstants.YEAR_ARRIVED_IN_HOST_COUNTRY, yearArrivedInHostCountryId);
                    }
                    clientOtherInfo.setYearArrivedInHostCountry(yearArrivedInHostCountryCodeValue);
                }
            }
            if (!changes.isEmpty()) {
                this.clientOtherInfoRepository.saveAndFlush(clientOtherInfo);
                LOG.info("Update successfully");
            }
            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withClientId(clientOtherInfo.clientId()) //
                    .withEntityId(clientOtherInfo.getId()) //
                    .with(changes) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            return CommandProcessingResult.empty();
        }

    }

}
