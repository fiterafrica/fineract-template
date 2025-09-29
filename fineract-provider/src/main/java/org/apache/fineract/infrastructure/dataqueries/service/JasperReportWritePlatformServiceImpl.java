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
package org.apache.fineract.infrastructure.dataqueries.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.service.MinIOStorageService;
import org.apache.fineract.infrastructure.dataqueries.domain.JasperReport;
import org.apache.fineract.infrastructure.dataqueries.domain.JasperReportRepository;
import org.apache.fineract.infrastructure.dataqueries.exception.ReportNotFoundException;
import org.apache.fineract.infrastructure.dataqueries.serialization.JasperReportRequestFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.report.provider.ReportingProcessServiceProvider;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.useradministration.domain.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import javax.persistence.PersistenceException;
import java.util.Map;

@Service
@Slf4j
public class JasperReportWritePlatformServiceImpl implements JasperReportWritePlatformService {

    private final PlatformSecurityContext context;
    private final JasperReportRequestFromApiJsonDeserializer fromApiJsonDeserializer;
    private final JasperReportRepository jasperReportRepository;
    private final ReportingProcessServiceProvider reportingProcessServiceProvider;
    private final MinIOStorageService minIOStorageService;
    private final JasperReportService jasperReadWriteReportService;


    @Autowired
    public JasperReportWritePlatformServiceImpl(final PlatformSecurityContext context,
                                                final JasperReportRequestFromApiJsonDeserializer fromApiJsonDeserializer, JasperReportRepository jasperReportRepository,
                                                final PermissionRepository permissionRepository, final ReportingProcessServiceProvider reportingProcessServiceProvider, MinIOStorageService minIOStorageService, JasperReportService jasperReadWriteReportService) {
        this.context = context;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.jasperReportRepository = jasperReportRepository;
        this.reportingProcessServiceProvider = reportingProcessServiceProvider;
        this.minIOStorageService = minIOStorageService;
        this.jasperReadWriteReportService = jasperReadWriteReportService;
    }

    @Transactional
    @Override
    public CommandProcessingResult createReport(final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            final JasperReport report = JasperReport.fromJson(command);

            report.setRequestedBy(this.context.authenticatedUser().getUsername());

            this.jasperReportRepository.saveAndFlush(report);

            return new CommandProcessingResultBuilder()
                    .withCommandId(command.commandId())
                    .withEntityId(report.getId()) //
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException | PersistenceException dve) {
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    public CommandProcessingResult updateReport(final Long reportId, final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            final JasperReport report = this.jasperReportRepository.findById(reportId).orElseThrow(() -> new ReportNotFoundException(reportId));

            final Map<String, Object> changes = report.update(command, this.reportingProcessServiceProvider.findAllReportingTypes());

            return new CommandProcessingResultBuilder()
                    .withCommandId(command.commandId())
                    .withEntityId(report.getId())
                    .with(changes)
                    .build();
        } catch (final JpaSystemException | DataIntegrityViolationException | PersistenceException e) {
            return CommandProcessingResult.empty();
        }
    }


    @Override
    public CommandProcessingResult approveReport(final Long reportId,JsonCommand command) {
        final JasperReport report = this.jasperReportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        Map<String,Object> parameters =  report.getParameters();
        parameters.put("APPROVED_BY", this.context.authenticatedUser().getUsername());

        byte[] reportBytes = this.jasperReadWriteReportService.generateReport(
                "disbursement_report",
                parameters,
                report.getFileFormat()
        );

        // Upload to MinIO
        String extension = getFileExtension(report.getFileFormat());
        String objectName = "disbursement/" + report.getId() + "-" + report.getReportName() + extension;
        String fileUrl = this.minIOStorageService.upload(objectName, reportBytes, report.getFileFormat());

        log.info("generated file path:"+objectName);

        // Mark as approved + store file path
        report.approve(this.context.authenticatedUser().getUsername());
        report.setFilePath(objectName);
        this.jasperReportRepository.save(report);

        return new CommandProcessingResultBuilder()
                .withEntityId(report.getId())
                .withCommandId(command.commandId())
                .build();
    }

    private String getFileExtension(String mediaType) {
        return switch (mediaType) {
            case "application/pdf" -> ".pdf";
            case "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                    ".xlsx";
            case "text/csv" -> ".csv";
            default -> ".bin";
        };
    }
}
