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
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "m_loan_transaction_reprocess")
public class LoanTransactionReprocess extends AbstractPersistableCustom {

    @Column(name = "loan_id")
    private Long loanId;
    @Column(name = "is_processed")
    private Boolean isProcessed;

    @Column(name = "process_duration")
    private Long processDuration;

    @Column(name = "processed_on_date")
    private LocalDateTime processedOnDate;

    @Column(name = "exception_message")
    private String exceptionMessage;

    public LoanTransactionReprocess() {}

    public LoanTransactionReprocess(Long loanId, Boolean isProcessed, Long processDuration, LocalDateTime processedOnDate,
            String exceptionMessage) {
        this.loanId = loanId;
        this.isProcessed = isProcessed;
        this.processDuration = processDuration;
        this.processedOnDate = processedOnDate;
        this.exceptionMessage = exceptionMessage;
    }

    public void setProcessed(Boolean processed) {
        isProcessed = processed;
    }

    public void setProcessDuration(Long processDuration) {
        this.processDuration = processDuration;
    }

    public void setProcessedOnDate(LocalDateTime processedOnDate) {
        this.processedOnDate = processedOnDate;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }
}
