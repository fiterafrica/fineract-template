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
package org.apache.fineract.portfolio.client.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;

@Entity
@Table(name = "m_client_additional_info")
public class ClientAdditionalInfo extends AbstractAuditableCustom {

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    @ManyToOne
    @JoinColumn(name = "title")
    private CodeValue title;

    @Column(name = "mnemonics", length = 1000)
    private String mnemonics;

    @Column(name = "alt_phone_no")
    private String alternatePhone;

    @Column(name = "initials")
    private String initials;
    @ManyToOne
    @JoinColumn(name = "marital_status")
    private CodeValue maritalStatus;

    public ClientAdditionalInfo() {}

    public ClientAdditionalInfo(Client client, CodeValue title, String mnemonics, String alternatePhone, CodeValue maritalStatus,
            String initials) {
        this.client = client;
        this.title = title;
        this.mnemonics = mnemonics;
        this.alternatePhone = alternatePhone;
        this.maritalStatus = maritalStatus;
        this.initials = initials;
    }

    public static ClientAdditionalInfo fromJson(final Client client, final CodeValue title, final CodeValue maritalStatus,
            final JsonCommand command) {
        final String mnemonics = command.stringValueOfParameterNamed(ClientApiConstants.mnemonicsParamNameParam);
        final String alternatePhoneNumber = command.stringValueOfParameterNamed(ClientApiConstants.altMobileNoParam);
        final String initials = command.stringValueOfParameterNamed(ClientApiConstants.initialsParam);
        return new ClientAdditionalInfo(client, title, mnemonics, alternatePhoneNumber, maritalStatus, initials);
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(4);

        final String titleIdParamName = "titleId";
        if (command.isChangeInLongParameterNamed(titleIdParamName, titleId())) {
            final Long newValue = command.longValueOfParameterNamed(titleIdParamName);
            actualChanges.put(titleIdParamName, newValue);
        }

        final String maritalStatusIdParamName = "maritalStatusId";
        if (command.isChangeInLongParameterNamed(maritalStatusIdParamName, maritalStatusId())) {
            final Long newValue = command.longValueOfParameterNamed(maritalStatusIdParamName);
            actualChanges.put(maritalStatusIdParamName, newValue);
        }

        final String mnemonicsParamName = "mnemonics";
        if (command.isChangeInStringParameterNamed(mnemonicsParamName, this.mnemonics)) {
            final String newValue = command.stringValueOfParameterNamed(mnemonicsParamName);
            actualChanges.put(mnemonicsParamName, newValue);
            this.mnemonics = StringUtils.defaultIfEmpty(newValue, null);
        }

        final String alternatePhoneParamName = "altMobileNo";
        if (command.isChangeInStringParameterNamed(alternatePhoneParamName, this.alternatePhone)) {
            final String newValue = command.stringValueOfParameterNamed(alternatePhoneParamName);
            actualChanges.put(alternatePhoneParamName, newValue);
            this.alternatePhone = StringUtils.defaultIfEmpty(newValue, null);
        }

        final String initialsParam = "initials";
        if (command.isChangeInStringParameterNamed(initialsParam, this.initials)) {
            final String newValue = command.stringValueOfParameterNamed(initialsParam);
            actualChanges.put(alternatePhoneParamName, newValue);
            this.initials = StringUtils.defaultIfEmpty(newValue, null);
        }

        return actualChanges;
    }

    private Long maritalStatusId() {
        Long maritalStatus = null;
        if (this.maritalStatus != null) {
            maritalStatus = this.maritalStatus.getId();
        }
        return maritalStatus;
    }

    private Long titleId() {
        Long title = null;
        if (this.title != null) {
            title = this.title.getId();
        }
        return title;
    }

    public void setTitle(CodeValue titleCodeValue) {
        this.title = titleCodeValue;
    }

    public void setMaritalStatus(CodeValue maritalStatusCodeValue) {
        this.maritalStatus = maritalStatusCodeValue;
    }
}
