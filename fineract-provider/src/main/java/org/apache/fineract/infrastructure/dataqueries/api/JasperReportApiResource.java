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
package org.apache.fineract.infrastructure.dataqueries.api;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.dataqueries.domain.JasperReport;
import org.apache.fineract.infrastructure.dataqueries.service.JasperReportService;
import org.apache.fineract.infrastructure.dataqueries.service.ReadJasperReportingService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.HttpHeaders;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;


@Slf4j
@Path("/reports/jasper")
@Component
@Scope("singleton")
@Tag(name = "Jasper Reports", description = "Non-core reports can be added, updated and deleted.")
public class JasperReportApiResource {

    private final JasperReportService jasperReadWriteReportService;
    private final ToApiJsonSerializer<JasperReport> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final PlatformSecurityContext context;
    private final ReadJasperReportingService readReportingService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;


    private final Set<String> responseDataParameters = new HashSet<>(Arrays.asList(
            "id",
            "requestedOn",
            "approvedBy",
            "approvedOn",
            "status",
            "fileFormat",
            "filePath",
            "parameters")
    );

    @Autowired
    public JasperReportApiResource(final JasperReportService jasperReadWriteReportService, ToApiJsonSerializer<JasperReport> toApiJsonSerializer, PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, PlatformSecurityContext context, ReadJasperReportingService readReportingService, ApiRequestParameterHelper apiRequestParameterHelper) {
        this.jasperReadWriteReportService = jasperReadWriteReportService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.context = context;
        this.readReportingService = readReportingService;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }

    @GET
    @Path("{reportName}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON, "application/pdf", "text/csv", "application/vnd.ms-excel", "text/html" })
    public Response getReport(@PathParam("reportName") final String reportName,
                              @Context final UriInfo uriInfo,
                              @Context final HttpHeaders headers) {

        Map<String, Object> queryParams = new HashMap<>();
        uriInfo.getQueryParameters().forEach((key, values) -> {
            if (!values.isEmpty()) {
                queryParams.put(key, values.get(0));
            }
        });

        String mediaType = headers.getAcceptableMediaTypes().isEmpty()
                ? "application/pdf"
                : headers.getAcceptableMediaTypes().get(0).toString();

        byte[] reportData = jasperReadWriteReportService.generateReport(reportName, queryParams, mediaType);

        return Response.ok(reportData, mediaType)
                .header("Content-Disposition", "inline; filename=\"" + reportName + getFileExtension(mediaType) + "\"")
                .build();
    }

    private String getFileExtension(String mediaType) {
        return switch (mediaType) {
            case "text/csv" -> ".csv";
            case "application/vnd.ms-excel" -> ".xls";
            case "text/html" -> ".html";
            case MediaType.APPLICATION_JSON -> ".json";
            default -> ".pdf";
        };
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create a Jasper Report Request", description = "")
    public String createReport(@Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createJasperReport().withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List Jasper Reports", description = "Lists all jasper reports and their parameters.\n" + "\n" + "Example Request:\n" + "\n"
            + "reports")
    public String retrieveReportList(@QueryParam("status") String status,@Context final UriInfo uriInfo) {

        String resourceNameForPermissions = "JASPER_REPORT";
        this.context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);

        final Collection<JasperReport> result = this.readReportingService.retrieveReportList(status);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, result, this.responseDataParameters);
    }


    @POST
    @Path("{reportId}/approve")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Approve a Jasper Report Request", description = "Approves a pending Jasper Report request")
    public String approveReport(
            @PathParam("reportId") final Long reportId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .approveJasperReport(reportId)
                .withJson(apiRequestBodyAsJson)
                .build();

        final CommandProcessingResult result =
                this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }


    @GET
    @Path("{reportId}/view")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getPresignedDocument(@PathParam("reportId") final String reportId,@Context final UriInfo uriInfo){

        String resourceNameForPermissions = "READ_JASPER_REPORT";

        this.context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);

        final JasperReport result = this.readReportingService.retrieveSignedReport(reportId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return this.toApiJsonSerializer.serialize(settings, result, this.responseDataParameters);
    }



}
