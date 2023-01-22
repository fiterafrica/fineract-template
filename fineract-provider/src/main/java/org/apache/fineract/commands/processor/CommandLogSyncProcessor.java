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

import static org.apache.fineract.commands.CommandConstants.*;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.fineract.commands.domain.CommandSource;
import org.apache.fineract.commands.service.CommandProcessingService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.service.TenantDetailsService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CommandLogSyncProcessor {

    @Autowired
    private TenantDetailsService tenantDetailsService;

    @Autowired
    @Qualifier("synchronousCommandProcessingService")
    private CommandProcessingService processingService;

    @Autowired
    private AppUserRepository appUserRepository;

    public CommandProcessingResult process(@Header(FINERACT_HEADER_CORRELATION_ID) String correlationId,
            @Header(FINERACT_HEADER_AUTH_TOKEN) String authToken, @Header(FINERACT_HEADER_RUN_AS) String username,
            @Header(FINERACT_HEADER_TENANT_ID) String tenantId, @Body CommandSource commandSource) {

        final FineractPlatformTenant tenant = this.tenantDetailsService.loadTenantById(tenantId);
        ThreadLocalContextUtil.setTenant(tenant);

        ThreadLocalContextUtil.setAuthToken(authToken);

        final AppUser user = appUserRepository.findAppUserByName(username);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword()));

        CommandProcessingResult result = processingService.logCommand(commandSource);
        result.setCorrelationId(correlationId);

        return result;
    }
}
