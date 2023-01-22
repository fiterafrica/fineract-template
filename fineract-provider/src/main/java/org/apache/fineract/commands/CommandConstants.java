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
package org.apache.fineract.commands;

public interface CommandConstants {

    String FINERACT_ROUTE_PROCESS_AND_LOG = "fineract-process-and-log";
    String FINERACT_ROUTE_LOG = "fineract-log";
    String FINERACT_HEADER_CORRELATION_ID = "X-FINERACT-CORRELATION-ID";
    String FINERACT_HEADER_TENANT_ID = "X-FINERACT-TENANT-ID";
    String FINERACT_HEADER_RUN_AS = "X-FINERACT-RUN-AS";
    String FINERACT_HEADER_AUTH_TOKEN = "X-FINERACT-AUTH_TOKEN";
    String FINERACT_HEADER_APPROVED_BY_CHECKER = "X-FINERACT-APPROVED-BY-CHECKER";
    String FINERACT_HEADER_ERROR_MESSAGE = "X-FINERACT-ERROR_MESSAGE";
    String ASYNC_COMMAND_PREFIX = "ASYNC";
}
