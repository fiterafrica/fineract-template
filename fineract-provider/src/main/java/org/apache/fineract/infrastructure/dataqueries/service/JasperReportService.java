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
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JasperReportService {

    private final RoutingDataSource routingDataSource;
    private final LoanProductRepository loanProductRepository;

    public JasperReportService(RoutingDataSource routingDataSource, LoanProductRepository loanProductRepository) {
        this.routingDataSource = routingDataSource;
        this.loanProductRepository = loanProductRepository;
    }

    public byte[] generateReport(String reportName, Map<String, Object> rawParams, String mediaType) {
        String resourcePath = "/jasperReports/" + reportName + ".jrxml";
        log.info("Looking for report at: {}", resourcePath);

        try (InputStream reportStream = getClass().getResourceAsStream(resourcePath)) {
            if (reportStream == null) {
                throw new IOException("Report file not found: " + resourcePath);
            }


            if (rawParams.containsKey("product_ids")){
                String PRODUCT_FILTERS = productFilters(rawParams.get("product_ids")).toString();
                rawParams.put("PRODUCT_FILTER", PRODUCT_FILTERS);
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            Map<String, Object> reportParams = normalizeParams(rawParams);

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    reportParams,
                    routingDataSource.getConnection()
            );

            log.info("Report compiled and filled successfully: {}", reportName);

            return exportReport(jasperPrint, mediaType);

        } catch (SQLException e) {
            throw new RuntimeException("SQL error while filling report: " + reportName, e);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while loading report: " + reportName, e);
        } catch (JRException e) {
            throw new RuntimeException("Jasper error while compiling/filling report: " + reportName, e);
        }
    }

    private Map<String, Object> normalizeParams(Map<String, Object> rawParams) {
        Map<String, Object> parsed = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        rawParams.forEach((key, value) -> {
            if (value == null) return;
            String upperKey = key.toUpperCase();
            String strVal = value.toString();

            switch (upperKey) {
                case "START_DATE":
                case "END_DATE":
                    LocalDate localDate = LocalDate.parse(strVal, formatter);
                    parsed.put(upperKey, java.sql.Date.valueOf(localDate));
                    break;
                case "INTEREST_PERCENTAGE":
                    parsed.put(upperKey, Double.parseDouble(strVal));
                    break;
                default:
                    parsed.put(upperKey, strVal);
            }
        });

        return parsed;
    }

    private byte[] exportReport(JasperPrint jasperPrint, String mediaType) throws JRException {
        switch (mediaType) {
            case "text/csv":
                JRCsvExporter csvExporter = new JRCsvExporter();
                ByteArrayOutputStream csvOut = new ByteArrayOutputStream();
                csvExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                csvExporter.setExporterOutput(new SimpleWriterExporterOutput(csvOut));
                csvExporter.exportReport();
                return csvOut.toByteArray();

            case "application/vnd.ms-excel":
                JRXlsExporter xlsExporter = new JRXlsExporter();
                ByteArrayOutputStream xlsOut = new ByteArrayOutputStream();
                xlsExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                xlsExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(xlsOut));
                xlsExporter.exportReport();
                return xlsOut.toByteArray();

            case "text/html":
                HtmlExporter htmlExporter = new HtmlExporter();
                ByteArrayOutputStream htmlOut = new ByteArrayOutputStream();
                htmlExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                htmlExporter.setExporterOutput(new SimpleHtmlExporterOutput(htmlOut));
                htmlExporter.exportReport();
                return htmlOut.toByteArray();

            case MediaType.APPLICATION_JSON:
                throw new UnsupportedOperationException("JSON export not implemented");

            default: // PDF
                return JasperExportManager.exportReportToPdf(jasperPrint);
        }
    }


    private List<String> productFilters(Object productIdsObj){
        if (productIdsObj != null) {
            String productIdsStr = productIdsObj.toString(); // e.g. "10,12,15"
            List<Long> productIds = Arrays.stream(productIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            return loanProductRepository.findAllById(productIds)
                    .stream()
                    .map(LoanProduct::getShortName)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
