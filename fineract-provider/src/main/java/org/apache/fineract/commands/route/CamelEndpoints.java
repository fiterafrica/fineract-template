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

public class CamelEndpoints {

    private CamelEndpoints() {}

    public static final String SEDA_FINERACT_DEADLETTER = "seda://fineract-deadletter";
    public static final String SEDA_FINERACT_PROCESS_AND_LOG = "seda://fineract-process-and-log";
    public static final String SEDA_FINERACT_LOG = "seda://fineract-log";

    public static final String RMQ_FINERACT_LOG = "rabbitmq://fineract-log?queue=log-queue";
    public static final String RMQ_FINERACT_RESULT = "rabbitmq://fineract-result?queue=result-queue";
    public static final String RMQ_FINERACT_ERROR = "rabbitmq://fineract-error?queue=error-queue";
    public static final String RMQ_PROCESS_AND_LOG = "rabbitmq://fineract-process-and-log?concurrentConsumers=1&queue=command-queue&autoDelete=false";

    public static final String ATMOSPHERE_WEBSOCKET_FINERACT_RESULT = "atmosphere-websocket:///fineract-result?servletName=CamelWsServlet";
    public static final String ATMOSPHERE_WEBSOCKET_SENDING_FINERACT_RESULT = "atmosphere-websocket:///fineract-result?sendToAll=false";

}
