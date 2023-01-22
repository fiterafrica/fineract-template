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
package org.apache.fineract.commands.route;

import java.util.concurrent.TimeUnit;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.fineract.commands.data.CredentialsData;
import org.apache.fineract.commands.processor.CommandProcessAndLogSyncProcessor;
import org.apache.fineract.commands.processor.CommandResultCorrelationIdProcessor;
import org.apache.fineract.commands.processor.CommandWebsocketAuthenticationProcessor;
import org.apache.fineract.commands.processor.CommandWebsocketConnectionKeyProcessor;
import org.apache.fineract.commands.processor.CommandWebsocketDisconnectProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("fineract.camel.frontend.enabled")
public class CamelFrontendAsyncRoute extends RouteBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamelFrontendAsyncRoute.class);

    @Autowired
    private CommandProcessAndLogSyncProcessor commandProcessAndLogSyncProcessor;

    @Autowired
    private CommandWebsocketDisconnectProcessor commandWebsocketDisconnectProcessor;

    @Autowired
    private CommandWebsocketAuthenticationProcessor commandWebsocketAuthenticationProcessor;

    @Autowired
    private CommandWebsocketConnectionKeyProcessor commandWebsocketConnectionKeyProcessor;

    @Autowired
    private CommandResultCorrelationIdProcessor commandResultCorrelationIdProcessor;

    @Value("${fineract.camel.frontend.enabled}")
    private String frontendEnabled;

    @Override
    public void configure() throws Exception {

        LOGGER.info("Sleeping for 3 seconds to allow the server to start");
        TimeUnit.SECONDS.sleep(3);
        LOGGER.info("**** CamelFrontEndAsyncRoute.configure():::  fineract.camel.frontend.enabled = [{}] *****", frontendEnabled);

        from(CamelEndpoints.SEDA_FINERACT_PROCESS_AND_LOG).threads(5, 10).maxQueueSize(-1).choice().otherwise()
                .bean(commandProcessAndLogSyncProcessor).when().method("commandAsyncFilter", "filterProcessAndLog").marshal()
                .json(JsonLibrary.Jackson).multicast().to(ExchangePattern.InOnly, CamelEndpoints.RMQ_PROCESS_AND_LOG);

        from(CamelEndpoints.SEDA_FINERACT_LOG).threads(5, 10).maxQueueSize(-1).choice().otherwise().bean(commandProcessAndLogSyncProcessor)
                .when().method("commandAsyncFilter", "filterLog").marshal().json(JsonLibrary.Jackson).multicast()
                .to(ExchangePattern.InOnly, CamelEndpoints.RMQ_FINERACT_LOG);

        from(CamelEndpoints.ATMOSPHERE_WEBSOCKET_FINERACT_RESULT)
                .log(LoggingLevel.ERROR, "Received: ${headers['websocket.connectionKey']} - ${body}").onException(Exception.class)
                .handled(true).end().choice().when(simple("${headers['websocket.eventType']} == 1"))
                .log(LoggingLevel.DEBUG, "Websocket Open").when(simple("${headers['websocket.eventType']} == 0"))
                .bean(commandWebsocketDisconnectProcessor).when(simple("${headers['websocket.eventType']} == -1"))
                .log(LoggingLevel.ERROR, "Websocket Error: ${headers['websocket.connectionKey']} - ${body}").otherwise().unmarshal()
                .json(JsonLibrary.Jackson, CredentialsData.class).bean(commandWebsocketAuthenticationProcessor);

        from(CamelEndpoints.RMQ_FINERACT_RESULT).onException(Exception.class).handled(true).end().threads(5, 10).maxQueueSize(-1)
                .bean(commandWebsocketConnectionKeyProcessor).bean(commandResultCorrelationIdProcessor).multicast()
                .to(ExchangePattern.InOnly, CamelEndpoints.ATMOSPHERE_WEBSOCKET_SENDING_FINERACT_RESULT);

        from(CamelEndpoints.RMQ_FINERACT_ERROR).onException(Exception.class).handled(true).end().threads(5, 10).maxQueueSize(-1)
                .bean(commandWebsocketConnectionKeyProcessor).bean(commandResultCorrelationIdProcessor).multicast()
                .to(ExchangePattern.InOnly, CamelEndpoints.ATMOSPHERE_WEBSOCKET_SENDING_FINERACT_RESULT);

    }

}
