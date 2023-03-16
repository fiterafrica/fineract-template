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

import static org.apache.fineract.commands.CommandConstants.ASYNC_COMMAND_PREFIX;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_APPROVED_BY_CHECKER;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_AUTH_TOKEN;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_RUN_AS;
import static org.apache.fineract.commands.CommandConstants.FINERACT_HEADER_TENANT_ID;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.fineract.commands.domain.CommandSource;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.springframework.stereotype.Component;

@Component
public class CommandAsyncFilter {

    public boolean filterProcessAndLog(CommandWrapper commandWrapper) {
        return filterProcessAndLog(null, null, null, null, commandWrapper);
    }

    public boolean filterProcessAndLog(@Header(FINERACT_HEADER_AUTH_TOKEN) String authToken,
            @Header(FINERACT_HEADER_RUN_AS) String username, @Header(FINERACT_HEADER_TENANT_ID) String tenantId,
            @Header(FINERACT_HEADER_APPROVED_BY_CHECKER) Boolean approvedByChecker, @Body CommandWrapper commandWrapper) {
        return commandWrapper != null && commandWrapper.actionName().startsWith(ASYNC_COMMAND_PREFIX);
    }

    public boolean filterLog(@Header(FINERACT_HEADER_AUTH_TOKEN) String authToken, @Header(FINERACT_HEADER_RUN_AS) String username,
            @Header(FINERACT_HEADER_TENANT_ID) String tenantId, @Body CommandSource commandSource) {
        return commandSource != null && commandSource.getActionName().startsWith(ASYNC_COMMAND_PREFIX);
    }
}
