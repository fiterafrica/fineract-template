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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.springframework.stereotype.Component;

@Component
public class CommandProcessingResultDeserializer extends JsonDeserializer<CommandProcessingResult> {

    @Override
    public CommandProcessingResult deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        CommandProcessingResult value = null;

        ObjectMapper oc = (ObjectMapper) parser.getCodec();
        JsonNode root = oc.readTree(parser);

        Long commandId = null;
        Long entityId = null;
        ExternalId resourceExternalId = null;
        ExternalId subResourceExternalId = null;
        Long officeId = null;
        Long groupId = null;
        Long clientId = null;
        Long loanId = null;
        Long savingsId = null;
        Long resourceId = null;
        Long subResourceId = null;
        String transactionId = null;
        String[] transactionIds = null;
        Map<String, Object> changes = null;
        Map<String, Object> oldValues = null;
        Map<String, Object> creditBureauReportData = null;
        String resourceIdentifier = null;
        Long productId = null;
        Long gsimId = null;
        Long glimId = null;
        Boolean rollbackTransaction = null;
        String fromSavingsAccountNumber = null;
        String toSavingsAccountNumber = null;

        Iterator<Map.Entry<String, JsonNode>> it = root.fields();

        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();

            if ("commandId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                commandId = entry.getValue().asLong();
            }
            if ("entityId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                entityId = entry.getValue().asLong();
            }
            if ("resourceExternalId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                resourceExternalId = oc.readValue(entry.getValue().toString(), new TypeReference<ExternalId>() {});
            }
            if ("subResourceExternalId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                subResourceExternalId = oc.readValue(entry.getValue().toString(), new TypeReference<ExternalId>() {});
            } else if ("officeId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                officeId = entry.getValue().asLong();
            } else if ("groupId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                groupId = entry.getValue().asLong();
            } else if ("clientId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                clientId = entry.getValue().asLong();
            } else if ("loanId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                loanId = entry.getValue().asLong();
            } else if ("savingsId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                savingsId = entry.getValue().asLong();
            } else if ("resourceId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                resourceId = entry.getValue().asLong();
            } else if ("subResourceId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                subResourceId = entry.getValue().asLong();
            } else if ("transactionId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                transactionId = entry.getValue().asText();
            } else if ("transactionIds".equals(entry.getKey()) && !entry.getValue().isNull()) {
                transactionIds = oc.readValue(entry.getValue().toString(), new TypeReference<String[]>() {});
            } else if ("changes".equals(entry.getKey()) && !entry.getValue().isNull()) {
                changes = oc.readValue(entry.getValue().toString(), new TypeReference<Map<String, Object>>() {});
            } else if ("oldValues".equals(entry.getKey()) && !entry.getValue().isNull()) {
                oldValues = oc.readValue(entry.getValue().toString(), new TypeReference<Map<String, Object>>() {});
            } else if ("productId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                creditBureauReportData = oc.readValue(entry.getValue().toString(), new TypeReference<Map<String, Object>>() {});
            } else if ("resourceIdentifier".equals(entry.getKey()) && !entry.getValue().isNull()) {
                resourceIdentifier = entry.getValue().asText();
            } else if ("productId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                productId = entry.getValue().asLong();
            } else if ("gsimId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                gsimId = entry.getValue().asLong();
            } else if ("glimId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                glimId = entry.getValue().asLong();
            } else if ("rollbackTransaction".equals(entry.getKey()) && !entry.getValue().isNull()) {
                rollbackTransaction = entry.getValue().asBoolean();
            } else if ("fromSavingsAccountNumber".equals(entry.getKey()) && !entry.getValue().isNull()) {
                fromSavingsAccountNumber = entry.getValue().asText();
            } else if ("toSavingsAccountNumber".equals(entry.getKey()) && !entry.getValue().isNull()) {
                toSavingsAccountNumber = entry.getValue().asText();
            }

            value = CommandProcessingResult.fromDetails(commandId, officeId, groupId, clientId, loanId, savingsId, resourceIdentifier,
                    entityId, gsimId, glimId, creditBureauReportData, transactionId, changes, productId, rollbackTransaction, subResourceId,
                    resourceExternalId, subResourceExternalId);
        }

        return value;
    }
}
