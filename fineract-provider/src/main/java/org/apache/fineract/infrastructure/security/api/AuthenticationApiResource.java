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
package org.apache.fineract.infrastructure.security.api;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.constants.TwoFactorConstants;
import org.apache.fineract.infrastructure.security.data.AuthenticatedUserData;
import org.apache.fineract.infrastructure.security.service.SpringSecurityPlatformSecurityContext;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.useradministration.data.RoleData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
@ConditionalOnProperty("fineract.security.basicauth.enabled")
@Path("/authentication")
@Tag(name = "Authentication HTTP Basic", description = "An API capability that allows client applications to verify authentication details using HTTP Basic Authentication.")
public class AuthenticationApiResource {

    @Value("${fineract.security.2fa.enabled}")
    private boolean twoFactorEnabled;

    public static class AuthenticateRequest {

        public String username;
        public String password;
    }

    private final DaoAuthenticationProvider customAuthenticationProvider;
    private final ToApiJsonSerializer<AuthenticatedUserData> apiJsonSerializerService;
    private final SpringSecurityPlatformSecurityContext springSecurityPlatformSecurityContext;
    private final ClientReadPlatformService clientReadPlatformService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AuthenticationApiResource(
            @Qualifier("customAuthenticationProvider") final DaoAuthenticationProvider customAuthenticationProvider,
            final ToApiJsonSerializer<AuthenticatedUserData> apiJsonSerializerService,
            final SpringSecurityPlatformSecurityContext springSecurityPlatformSecurityContext,
            ClientReadPlatformService aClientReadPlatformService, final JdbcTemplate jdbcTemplate) {
        this.customAuthenticationProvider = customAuthenticationProvider;
        this.apiJsonSerializerService = apiJsonSerializerService;
        this.springSecurityPlatformSecurityContext = springSecurityPlatformSecurityContext;
        clientReadPlatformService = aClientReadPlatformService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Verify authentication", description = "Authenticates the credentials provided and returns the set roles and permissions allowed.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AuthenticationApiResourceSwagger.PostAuthenticationRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AuthenticationApiResourceSwagger.PostAuthenticationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Unauthenticated. Please login") })
    public String authenticate(@Parameter(hidden = true) final String apiRequestBodyAsJson,
            @QueryParam("returnClientList") @DefaultValue("false") boolean returnClientList, @Context HttpServletRequest servletRequest) {
        // TODO FINERACT-819: sort out Jersey so JSON conversion does not have
        // to be done explicitly via GSON here, but implicit by arg
        AuthenticateRequest request = new Gson().fromJson(apiRequestBodyAsJson, AuthenticateRequest.class);
        if (request == null) {
            throw new IllegalArgumentException(
                    "Invalid JSON in BODY (no longer URL param; see FINERACT-726) of POST to /authentication: " + apiRequestBodyAsJson);
        }
        if (request.username == null || request.password == null) {
            throw new IllegalArgumentException("Username or Password is null in JSON (see FINERACT-726) of POST to /authentication: "
                    + apiRequestBodyAsJson + "; username=" + request.username + ", password=" + request.password);
        }

        final Authentication authentication = new UsernamePasswordAuthenticationToken(request.username, request.password);
        final Authentication authenticationCheck = this.customAuthenticationProvider.authenticate(authentication);

        final Collection<String> permissions = new ArrayList<>();
        AuthenticatedUserData authenticatedUserData = new AuthenticatedUserData(request.username, permissions);

        if (authenticationCheck.isAuthenticated()) {
            final Collection<GrantedAuthority> authorities = new ArrayList<>(authenticationCheck.getAuthorities());
            for (final GrantedAuthority grantedAuthority : authorities) {
                permissions.add(grantedAuthority.getAuthority());
            }

            final byte[] base64EncodedAuthenticationKey = Base64.getEncoder()
                    .encode((request.username + ":" + request.password).getBytes(StandardCharsets.UTF_8));

            final AppUser principal = (AppUser) authenticationCheck.getPrincipal();
            final Collection<RoleData> roles = new ArrayList<>();
            final Set<Role> userRoles = principal.getRoles();
            for (final Role role : userRoles) {
                roles.add(role.toData());
            }

            final Long officeId = principal.getOffice().getId();
            final String officeName = principal.getOffice().getName();

            final Long staffId = principal.getStaffId();
            final String staffDisplayName = principal.getStaffDisplayName();

            final EnumOptionData organisationalRole = principal.organisationalRoleData();

            boolean isTwoFactorRequired = this.twoFactorEnabled
                    && !principal.hasSpecificPermissionTo(TwoFactorConstants.BYPASS_TWO_FACTOR_PERMISSION);
            Long userId = principal.getId();
            if (this.springSecurityPlatformSecurityContext.doesPasswordHasToBeRenewed(principal)) {
                authenticatedUserData = new AuthenticatedUserData(request.username, userId,
                        new String(base64EncodedAuthenticationKey, StandardCharsets.UTF_8), isTwoFactorRequired);
            } else {

                authenticatedUserData = new AuthenticatedUserData(request.username, officeId, officeName, staffId, staffDisplayName,
                        organisationalRole, roles, permissions, principal.getId(),
                        new String(base64EncodedAuthenticationKey, StandardCharsets.UTF_8), isTwoFactorRequired,
                        returnClientList ? clientReadPlatformService.retrieveUserClients(userId) : null);
            }

            String clientIp = "Unknown IP Address";
            if (servletRequest != null) {
                clientIp = servletRequest.getHeader("X-Forwarded-For");
                if (clientIp == null || clientIp.isEmpty()) {
                    clientIp = servletRequest.getRemoteAddr();
                } else {
                    // The X-Forwarded-For header can contain multiple IP addresses, in case of proxies.
                    // The first IP in the list is the original client.
                    clientIp = Iterables.get(Splitter.on(',').split(clientIp), 0);
                    ;
                }
            }
            this.jdbcTemplate.update("insert into m_portfolio_command_source "
                    + "(action_name,entity_name,office_id,api_get_url,command_as_json,resource_id,maker_id,made_on_date,processing_result_enum) "
                    + "values(?, ?,?,?,?,?,?,current_date,1) ", "LOGIN", "AUTHENTICATION", principal.getOffice().getId(),
                    "/authenticate/login", "{ipAddress:\"" + clientIp + "\"}", userId, userId);
        }

        return this.apiJsonSerializerService.serialize(authenticatedUserData);
    }

    @Path("/logout")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String processLogout(final String apiRequestBodyAsJson, @Context HttpServletRequest servletRequest) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().invalidateTwoFactorAccessToken().withJson(apiRequestBodyAsJson)
                .build();

        String clientIp = "Unknown IP Address";
        if (servletRequest != null) {
            clientIp = servletRequest.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = servletRequest.getRemoteAddr();
            } else {
                // The X-Forwarded-For header can contain multiple IP addresses, in case of proxies.
                // The first IP in the list is the original client.
                clientIp = Iterables.get(Splitter.on(',').split(clientIp), 0);
                ;
            }
        }

        AppUser appUser = this.springSecurityPlatformSecurityContext.authenticatedUser();
        Long userId = appUser.getId();
        this.jdbcTemplate.update("insert into m_portfolio_command_source "
                + "(action_name,entity_name,office_id,api_get_url,command_as_json,resource_id,maker_id,made_on_date,processing_result_enum) "
                + "values(?, ?,?,?,?,?,?,current_date,1) ", "LOGOUT", "AUTHENTICATION", appUser.getOffice().getId(), "/authenticate/logout",
                "{ipAddress:\"" + clientIp + "\"}", userId, userId);
        return this.apiJsonSerializerService.serialize("");
    }
}
