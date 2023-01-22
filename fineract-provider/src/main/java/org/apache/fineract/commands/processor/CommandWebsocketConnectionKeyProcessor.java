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

import static org.apache.camel.component.atmosphere.websocket.WebsocketConstants.CONNECTION_KEY;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_RUN_AS;

import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.service.CamelCommandWebsocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommandWebsocketConnectionKeyProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CommandWebsocketConnectionKeyProcessor.class);

    @Autowired
    private CamelCommandWebsocketSessionService camelCommandWebsocketSessionService;

    public void process(Exchange exchange) {
        String username = exchange.getIn().getHeader(FINERACT_HEADER_RUN_AS, String.class);
        String connectionKey = camelCommandWebsocketSessionService.getUserConnectionKey(username);

        if (!StringUtils.isEmpty(connectionKey)) {
            exchange.getIn().setHeader(CONNECTION_KEY, connectionKey);
        } else {
            LOG.debug("No websocket session found for user: {}", username);
        }
    }
}
