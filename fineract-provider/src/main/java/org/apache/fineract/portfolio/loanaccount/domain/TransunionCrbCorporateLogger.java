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

import lombok.Data;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;

@Data
@Entity
@Table(name = "m_transunion_crb_corporate_logger")
public class TransunionCrbCorporateLogger extends AbstractPersistableCustom {

    private static final long serialVersionUID = 9181640245194392646L;

    @Column(name = "batch_id")
    private String batchId;
    @Column(name = "has_passed")
    private Boolean hasPassed;
    @Column(name = "loan_id")
    private Integer loanId;
    @Column(name = "crb_response_id")
    private String crbResponseId;
    @Column(name = "error_logs")
    private String errorLogs;
    @Column(name = "created_on")
    private String created_on;

    public TransunionCrbCorporateLogger() {
    }

    public TransunionCrbCorporateLogger(String batchId, Boolean hasPassed, Integer loanId, String crbResponseId, String errorLogs, String created_on) {
        this.batchId = batchId;
        this.hasPassed = hasPassed;
        this.loanId = loanId;
        this.crbResponseId = crbResponseId;
        this.errorLogs = errorLogs;
        this.created_on = created_on;
    }
}
