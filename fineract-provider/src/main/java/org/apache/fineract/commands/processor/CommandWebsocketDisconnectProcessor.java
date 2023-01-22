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

import org.apache.camel.Exchange;
import org.apache.fineract.commands.service.CamelCommandWebsocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommandWebsocketDisconnectProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CommandWebsocketDisconnectProcessor.class);

    @Autowired
    private CamelCommandWebsocketSessionService camelCommandWebsocketSessionService;

    public void process(Exchange exchange) {
        String connectionKey = exchange.getIn().getHeader(CONNECTION_KEY, String.class);

        camelCommandWebsocketSessionService.removeUserByConnectionKey(connectionKey);
        exchange.getIn().setHeader(Exchange.AUTHENTICATION, null);
    }
}
