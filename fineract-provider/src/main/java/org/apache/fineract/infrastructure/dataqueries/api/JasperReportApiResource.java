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


import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.fineract.infrastructure.dataqueries.service.JasperReadReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


import javax.ws.rs.GET;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.HttpHeaders;
import java.util.HashMap;
import java.util.Map;

@Path("/reports/jasper")
@Component
@Scope("singleton")
@Tag(name = "Jasper Reports", description = "Non-core reports can be added, updated and deleted.")
public class JasperReportApiResource {

    private final JasperReadReportService jasperReadReportService;

    @Autowired
    public JasperReportApiResource(final JasperReadReportService jasperReadReportService) {
        this.jasperReadReportService = jasperReadReportService;
    }

    @GET
    @Path("{reportName}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON, "application/pdf", "text/csv", "application/vnd.ms-excel", "text/html" })
    public Response getReport(@PathParam("reportName") final String reportName,
                              @Context final UriInfo uriInfo,
                              @Context final HttpHeaders headers) throws Exception {

        Map<String, Object> queryParams = new HashMap<>();
        uriInfo.getQueryParameters().forEach((key, values) -> {
            if (!values.isEmpty()) {
                queryParams.put(key, values.get(0));
            }
        });

        // Determine requested format from Accept header (defaults to PDF)
        String mediaType = headers.getAcceptableMediaTypes().isEmpty()
                ? "application/pdf"
                : headers.getAcceptableMediaTypes().get(0).toString();

        byte[] reportData = jasperReadReportService.generateReport(reportName, queryParams, mediaType);

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

}
