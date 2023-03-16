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
package org.apache.fineract.commands.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.springframework.stereotype.Component;

@Component
public class CommandWrapperSerializer extends JsonSerializer<CommandWrapper> {

    @Override
    public void serialize(CommandWrapper value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("commandId", value.commandId());
        gen.writeObjectField("officeId", value.getOfficeId());
        gen.writeObjectField("groupId", value.getGroupId());
        gen.writeObjectField("clientId", value.getClientId());
        gen.writeObjectField("loanId", value.getLoanId());
        gen.writeObjectField("savingsId", value.getSavingsId());
        gen.writeObjectField("actionName", value.actionName());
        gen.writeObjectField("entityName", value.entityName());
        gen.writeObjectField("entityId", value.getEntityId());
        gen.writeObjectField("subentityId", value.getSubentityId());
        gen.writeObjectField("href", value.getHref());
        gen.writeObjectField("json", value.getJson());
        gen.writeObjectField("transactionId", value.getTransactionId());
        gen.writeObjectField("productId", value.getProductId());
        gen.writeObjectField("creditBureauId", value.getCreditBureauId());
        gen.writeObjectField("organisationCreditBureauId", value.getOrganisationCreditBureauId());
        gen.writeObjectField("templateId", value.getTemplateId());
        gen.writeEndObject();
    }
}
