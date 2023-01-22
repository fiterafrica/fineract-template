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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.fineract.commands.processor.CommandErrorProcessor;
import org.apache.fineract.commands.processor.CommandLogSyncProcessor;
import org.apache.fineract.commands.processor.CommandQueuingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("fineract.camel.backend.enabled")
public class CamelBackendAsyncRoute extends RouteBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamelBackendAsyncRoute.class);

    @Autowired
    private CommandLogSyncProcessor commandLogSyncProcessor;

    @Autowired
    private CommandErrorProcessor commandErrorProcessor;

    @Autowired
    private CommandQueuingProcessor commandQueuingProcessor;

    @Value("${fineract.camel.backend.enabled}")
    private String backendEnabled;

    @Override
    public void configure() throws Exception {

        LOGGER.info("Sleeping for 3 seconds to allow the server to start");
        TimeUnit.SECONDS.sleep(3);
        LOGGER.info("****** CamelBackendAsyncRoute.configure(): fineract.camel.backend.enabled = [{}] *****", backendEnabled);

        from(CamelEndpoints.RMQ_PROCESS_AND_LOG).log(
                "Received message from rabbitmq. exchangeId: ${header.CamelRabbitmqExchangeName} on Exchange: ${header.CamelRabbitmqExchangeName}")
                .bean(commandQueuingProcessor, "process").onException(Exception.class).handled(true).bean(commandErrorProcessor);

        from(CamelEndpoints.RMQ_FINERACT_LOG).bean(commandLogSyncProcessor).marshal().json(JsonLibrary.Jackson).multicast()
                .to(ExchangePattern.InOnly, CamelEndpoints.RMQ_FINERACT_RESULT);

        // TODO: these are only Hook events
        // from("spring-event://fineract-error")
        // .bean(commandErrorEventFilter, "filter")
        // .multicast()
        // .to(ExchangePattern.InOnly, "rabbitmq://fineract-result")
        // .end();
    }
}
