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
package org.apache.fineract.commands.processor;

import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_CORRELATION_ID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommandResultCorrelationIdProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CommandResultCorrelationIdProcessor.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    public String process(@Header(FINERACT_HEADER_CORRELATION_ID) String correlationId, @Body String body) {
        try {
            JsonNode node = objectMapper.readTree(body);

            if (node.isObject()) {
                ObjectNode objectNode = (ObjectNode) node;

                objectNode.put("correlationId", correlationId);

                // all results that do not have status are successful
                if (!objectNode.has("status")) {
                    objectNode.put("status", "SUCCESSFUL");
                }

                return objectNode.toString();
            }
        } catch (Exception e) {
            LOG.error("Command result correlation ID: ", e);
        }

        return body;
    }
}
