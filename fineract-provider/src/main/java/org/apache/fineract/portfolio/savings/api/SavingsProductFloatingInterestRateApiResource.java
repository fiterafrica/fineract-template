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
package org.apache.fineract.portfolio.savings.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.savings.data.SavingsProductFloatingInterestRateData;
import org.apache.fineract.portfolio.savings.service.SavingsProductFloatingInterestRateReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/savingsproducts/{savingsProductId}/floatinginterestrates")
@Component
@Scope("singleton")
@Tag(name = "Floating Interest Rate", description = "This defines an floating interest rate scheme that can be associated to a Saving product.")
public class SavingsProductFloatingInterestRateApiResource {

    private final Set<String> responseDataParameters = new HashSet<>(
            Arrays.asList("id", "savingsProductId", "fromDate", "endDate", "floatingInterestRate"));
    private final String resourceNameForPermissions = "SavingsProductFloatingInterestRates";
    private final PlatformSecurityContext context;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ToApiJsonSerializer<SavingsProductFloatingInterestRateData> toApiJsonSerializer;
    private final SavingsProductFloatingInterestRateReadPlatformService readPlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @Autowired
    public SavingsProductFloatingInterestRateApiResource(final PlatformSecurityContext context,
            final ToApiJsonSerializer<SavingsProductFloatingInterestRateData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final SavingsProductFloatingInterestRateReadPlatformService readPlatformService,
            final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.context = context;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.readPlatformService = readPlatformService;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String add(@PathParam("savingsProductId") final long savingsProductId, final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().addSavingsProductFloatingInterestRate(savingsProductId)
                .withJson(apiRequestBodyAsJson).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{savingsProductFloatingInterestRateId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String update(@PathParam("savingsProductId") final long savingsProductId,
            @PathParam("savingsProductFloatingInterestRateId") final Long savingsProductFloatingInterestRateId,
            final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .updateSavingsProductFloatingInterestRate(savingsProductFloatingInterestRateId, savingsProductId)
                .withJson(apiRequestBodyAsJson).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveBusinessOwners(@Context final UriInfo uriInfo, @PathParam("savingsProductId") final long savingsProductId) {
        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);
        final Collection<SavingsProductFloatingInterestRateData> savingsProductFloatingInterestRates = this.readPlatformService
                .getSavingsProductFloatingInterestRateForSavingsProduct(savingsProductId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, savingsProductFloatingInterestRates, this.responseDataParameters);

    }

    @GET
    @Path("/{savingsProductFloatingInterestRateId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getSavingsProductFloatingInterestRateById(@Context final UriInfo uriInfo,
            @PathParam("savingsProductFloatingInterestRateId") final Long savingsProductFloatingInterestRateId,
            @PathParam("savingsProductId") final Long savingsProductId) {
        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);
        final SavingsProductFloatingInterestRateData savingsProductFloatingInterestRateData = this.readPlatformService
                .getSavingsProductFloatingInterestRateById(savingsProductFloatingInterestRateId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, savingsProductFloatingInterestRateData, this.responseDataParameters);
    }

    @DELETE
    @Path("{savingsProductFloatingInterestRateId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteCharge(@PathParam("savingsProductFloatingInterestRateId") final Long savingsProductFloatingInterestRateId,
            @PathParam("savingsProductId") final Long savingsProductId) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .deleteSavingsProductFloatingInterestRate(savingsProductFloatingInterestRateId, savingsProductId).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }
}
