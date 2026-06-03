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

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@Entity
@Table(name = "m_bulk_foreclosure_job_detail")
@Getter
@Setter
@NoArgsConstructor
public class BulkForeclosureJobDetail extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "bulk_foreclosure_job_id", nullable = false)
    private BulkForeclosureJob job;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "loan_account_no")
    private String loanAccountNo;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "status", nullable = false)
    private String status; // SUCCESS, FAILED

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "processed_on")
    private LocalDateTime processedOn;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_retried_on")
    private LocalDateTime lastRetriedOn;

    public static BulkForeclosureJobDetail success(BulkForeclosureJob job, Long loanId, String loanAccountNo, String clientName) {
        BulkForeclosureJobDetail detail = new BulkForeclosureJobDetail();
        detail.setJob(job);
        detail.setLoanId(loanId);
        detail.setLoanAccountNo(loanAccountNo);
        detail.setClientName(clientName);
        detail.setStatus("SUCCESS");
        detail.setProcessedOn(LocalDateTime.now(DateUtils.getDateTimeZoneOfTenant()));
        return detail;
    }

    public static BulkForeclosureJobDetail failure(BulkForeclosureJob job, Long loanId, String loanAccountNo, String clientName,
            String reason) {
        BulkForeclosureJobDetail detail = new BulkForeclosureJobDetail();
        detail.setJob(job);
        detail.setLoanId(loanId);
        detail.setLoanAccountNo(loanAccountNo);
        detail.setClientName(clientName);
        detail.setStatus("FAILED");
        detail.setFailureReason(reason);
        detail.setProcessedOn(LocalDateTime.now(DateUtils.getDateTimeZoneOfTenant()));
        return detail;
    }

    /**
     * Mark this detail as successfully retried.
     */
    public void markAsRetrySuccess() {
        this.status = "SUCCESS";
        this.failureReason = null;
        this.retryCount = this.retryCount + 1;
        this.lastRetriedOn = LocalDateTime.now(DateUtils.getDateTimeZoneOfTenant());
    }

    /**
     * Mark this detail as failed after retry.
     *
     * @param reason
     *            the failure reason
     */
    public void markAsRetryFailed(String reason) {
        this.status = "FAILED";
        this.failureReason = reason;
        this.retryCount = this.retryCount + 1;
        this.lastRetriedOn = LocalDateTime.now(DateUtils.getDateTimeZoneOfTenant());
    }

    /**
     * Check if this record is eligible for retry (only FAILED records can be retried).
     *
     * @return true if can be retried
     */
    public boolean canRetry() {
        return "FAILED".equals(this.status);
    }
}
