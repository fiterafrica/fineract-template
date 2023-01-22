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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ext.ExceptionMapper;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommandErrorProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CommandErrorProcessor.class);

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, ExceptionMapper<Throwable>> exceptionMappers = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Autowired
    public CommandErrorProcessor(List<ExceptionMapper> exceptionMappers) {
        for (ExceptionMapper<Throwable> exceptionMapper : exceptionMappers) {
            String ex = TypeUtils.getTypeArguments(exceptionMapper.getClass(), ExceptionMapper.class).entrySet().stream()
                    .map(entry -> ((Class) entry.getValue()).getCanonicalName()).findFirst()
                    .orElse("!!!! " + exceptionMapper.getClass().getCanonicalName() + " !!!!");

            this.exceptionMappers.put(ex, exceptionMapper);
        }
    }

    public String onException(Exchange exchange) {
        Throwable exception = (Throwable) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);

        String correlationId = exchange.getIn().getHeader(FINERACT_HEADER_CORRELATION_ID, String.class);

        try {
            ExceptionMapper<Throwable> exceptionMapper = this.exceptionMappers.get(exception.getClass().getCanonicalName());

            if (exceptionMapper != null) {
                String json = objectMapper.writeValueAsString(exceptionMapper.toResponse(exception).getEntity());
                JsonNode node = objectMapper.readTree(json);

                if (node.isObject()) {
                    ObjectNode objectNode = (ObjectNode) node;

                    objectNode.put("correlationId", correlationId);

                    return objectNode.toString();
                }
            }
        } catch (Exception e) {
            LOG.error("Command error processor: ", e);
        }

        return "{}";
    }
}
