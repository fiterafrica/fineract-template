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

import javax.security.auth.Subject;
import org.apache.camel.Exchange;
import org.apache.fineract.commands.data.CredentialsData;
import org.apache.fineract.commands.service.CamelCommandWebsocketSessionService;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.service.BasicAuthTenantDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CommandWebsocketAuthenticationProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CommandWebsocketAuthenticationProcessor.class);

    @Autowired
    private CamelCommandWebsocketSessionService camelCommandWebsocketSessionService;

    @Autowired
    private BasicAuthTenantDetailsService basicAuthTenantDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    public void process(Exchange exchange) {
        String connectionKey = exchange.getIn().getHeader(CONNECTION_KEY, String.class);
        CredentialsData credentials = exchange.getIn().getBody(CredentialsData.class);

        FineractPlatformTenant tenant = basicAuthTenantDetailsService.loadTenantById(credentials.getTenantId(), false);
        ThreadLocalContextUtil.setTenant(tenant);

        UserDetails userDetails = userDetailsService.loadUserByUsername(credentials.getUsername());

        if (userDetails.isAccountNonExpired() && userDetails.isAccountNonLocked() && userDetails.isCredentialsNonExpired()
                && userDetails.isEnabled() && passwordEncoder.matches(credentials.getPassword(), userDetails.getPassword())) {

            camelCommandWebsocketSessionService.addUser(connectionKey, userDetails.getUsername());

            Authentication userPrincipal = new UsernamePasswordAuthenticationToken(userDetails.getUsername(), userDetails.getPassword(),
                    userDetails.getAuthorities());

            Subject subject = new Subject();
            subject.getPrincipals().add(userPrincipal);

            exchange.getIn().setHeader(Exchange.AUTHENTICATION, subject);
        } else {
            throw new AccessDeniedException("User do not have access to this resource.");
        }
    }
}
