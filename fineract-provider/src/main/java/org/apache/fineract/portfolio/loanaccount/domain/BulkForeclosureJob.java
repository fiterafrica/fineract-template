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
package org.apache.fineract.portfolio.loanaccount.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@Entity
@Table(name = "m_bulk_foreclosure_job")
@Getter
@Setter
@NoArgsConstructor
public class BulkForeclosureJob extends AbstractPersistableCustom {

    @Column(name = "job_id", nullable = false, unique = true)
    private String jobId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "foreclosure_date", nullable = false)
    private LocalDate foreclosureDate;

    @Column(name = "total_loans", nullable = false)
    private Integer totalLoans;

    @Column(name = "successful", nullable = false)
    private Integer successful;

    @Column(name = "failed", nullable = false)
    private Integer failed;

    @Column(name = "submitted_on", nullable = false)
    private LocalDateTime submittedOn;

    @Column(name = "completed_on")
    private LocalDateTime completedOn;

    @Column(name = "submitted_by_user_id")
    private Long submittedByUserId;

    @Column(name = "execution_mode")
    private String executionMode;

    public static BulkForeclosureJob create(String jobId, LocalDate foreclosureDate, int totalLoans, Long userId, String executionMode) {
        BulkForeclosureJob job = new BulkForeclosureJob();
        job.setJobId(jobId);
        job.setStatus("PENDING");
        job.setForeclosureDate(foreclosureDate);
        job.setTotalLoans(totalLoans);
        job.setSuccessful(0);
        job.setFailed(0);
        job.setSubmittedOn(LocalDateTime.now(DateUtils.getDateTimeZoneOfTenant()));
        job.setSubmittedByUserId(userId);
        job.setExecutionMode(executionMode);
        return job;
    }
}
