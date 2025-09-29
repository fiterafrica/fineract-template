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
package org.apache.fineract.infrastructure.dataqueries.domain;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "jasper_report_requests")
@Getter
@Setter
public class JasperReport extends AbstractPersistableCustom {
    @Column(name = "report_name", nullable = false, length = 100)
    private String reportName;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

    @Column(name = "requested_on", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime requestedOn;

    @Column(name = "parameters", columnDefinition = "JSON")
    private String parameters;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_on")
    private LocalDateTime approvedOn;

    @Column(name = "file_format", length = 20)
    private String fileFormat; // PDF, XLS, CSV

    @Column(name = "file_path", length = 255)
    private String filePath; // S3/Minion path or local path

    public JasperReport() {
    }

    public JasperReport(String reportName, String parameters, LocalDateTime requestedOn, String requestedBy, String approvedBy, String status, LocalDateTime approvedOn, String fileFormat, String filePath) {
        this.reportName = reportName;
        this.parameters = parameters;
        this.requestedOn = requestedOn;
        this.requestedBy = requestedBy;
        this.approvedBy = approvedBy;
        this.status = status;
        this.approvedOn = approvedOn;
        this.fileFormat = fileFormat;
        this.filePath = filePath;
    }

    public static JasperReport fromJson(JsonCommand command) {
        final String reportName = command.stringValueOfParameterNamed("report_name");
        String parameters = "{}";
        if (command.parameterExists("parameters")) {
            parameters = command.jsonFragment("parameters");
        }
        final String requestedBy = command.stringValueOfParameterNamed("requested_by");
        final String approvedBy = command.stringValueOfParameterNamed("approved_by");
        final String status = command.stringValueOfParameterNamed("status");
        final String fileFormat = command.stringValueOfParameterNamed("file_format");
        final String filePath = command.stringValueOfParameterNamed("file_path");

        // Parse timestamps safely
        final LocalDateTime requestedOn = command.localDateTimeValueOfParameterNamed("requested_on");
        final LocalDateTime approvedOn = command.localDateTimeValueOfParameterNamed("approved_on");

        return new JasperReport(reportName, parameters, requestedOn != null ? requestedOn : DateUtils.getLocalDateTimeOfSystem(),
                requestedBy, approvedBy, status, approvedOn, fileFormat, filePath);
    }

    public Map<String, Object> update(JsonCommand command, Collection<String> allReportingTypes) {
        return new LinkedHashMap<>(8);
    }

    public void approve(String approvedBy) {
        this.status = "APPROVED";
        this.approvedOn = LocalDate.now(ZoneId.systemDefault()).atStartOfDay();
        this.approvedBy = approvedBy;
    }

    public Map<String, Object> getParameters() {
        if (this.parameters == null || this.parameters.isBlank()) {
            return new HashMap<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(this.parameters, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse report parameters JSON: " + this.parameters, e);
        }
    }
}
