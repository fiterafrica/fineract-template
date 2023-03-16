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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.springframework.stereotype.Component;

@Component
public class CommandWrapperDeserializer extends JsonDeserializer<CommandWrapper> {

    @Override
    public CommandWrapper deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        CommandWrapper value = null;

        ObjectMapper oc = (ObjectMapper) parser.getCodec();
        JsonNode root = oc.readTree(parser);

        Long commandId = null;
        Long officeId = null;
        Long groupId = null;
        Long clientId = null;
        Long loanId = null;
        Long savingsId = null;
        String actionName = null;
        String entityName = null;
        Long entityId = null;
        Long subentityId = null;
        String href = null;
        String json = null;
        String transactionId = null;
        Long productId = null;
        Long creditBureauId = null;
        Long organisationCreditBureauId = null;
        Long templateId = null;
        String jobName = null;
        String idempotencyKey = null;

        Iterator<Map.Entry<String, JsonNode>> it = root.fields();

        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();

            if ("commandId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                commandId = entry.getValue().asLong();
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
            } else if ("actionName".equals(entry.getKey()) && !entry.getValue().isNull()) {
                actionName = entry.getValue().asText();
            } else if ("entityName".equals(entry.getKey()) && !entry.getValue().isNull()) {
                entityName = entry.getValue().asText();
            } else if ("entityId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                entityId = entry.getValue().asLong();
            } else if ("subentityId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                subentityId = entry.getValue().asLong();
            } else if ("href".equals(entry.getKey()) && !entry.getValue().isNull()) {
                href = entry.getValue().asText();
            } else if ("json".equals(entry.getKey()) && !entry.getValue().isNull()) {
                json = entry.getValue().asText();
            } else if ("transactionId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                transactionId = entry.getValue().asText();
            } else if ("productId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                productId = entry.getValue().asLong();
            } else if ("jobName".equals(entry.getKey()) && !entry.getValue().isNull()) {
                creditBureauId = entry.getValue().asLong();
            } else if ("organisationCreditBureauId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                organisationCreditBureauId = entry.getValue().asLong();
            } else if ("templateId".equals(entry.getKey()) && !entry.getValue().isNull()) {
                templateId = entry.getValue().asLong();
            } else if ("jobName".equals(entry.getKey()) && !entry.getValue().isNull()) {
                jobName = entry.getValue().asText();
            } else if ("idempotencyKey".equals(entry.getKey()) && !entry.getValue().isNull()) {
                idempotencyKey = entry.getValue().asText();
            }

            value = new CommandWrapper(officeId, groupId, clientId, loanId, savingsId, actionName, entityName, entityId, subentityId, href,
                    json, transactionId, productId, templateId, creditBureauId, organisationCreditBureauId, jobName, idempotencyKey);

            // value = new CommandWrapper(commandId, officeId, groupId, clientId, loanId, savingsId, actionName,
            // entityName, entityId,
            // subentityId, href, json, transactionId, productId, templateId, creditBureauId,
            // organisationCreditBureauId, null);
        }

        return value;
    }
}
