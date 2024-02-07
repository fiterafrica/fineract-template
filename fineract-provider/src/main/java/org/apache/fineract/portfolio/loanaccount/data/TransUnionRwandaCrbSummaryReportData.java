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
package org.apache.fineract.portfolio.loanaccount.data;

import lombok.Data;

@Data
public class TransUnionRwandaCrbSummaryReportData {

    private Integer id;
    private Integer bcAllSectors;
    private Integer bcMySector;
    private Integer bcOtherSectors;
    private Integer bc180AllSectors;
    private Integer bc180MySector;
    private Integer bc180OtherSectors;
    private Integer bc90AllSectors;
    private Integer bc90MySector;
    private Integer bc90OtherSectors;
    private Integer bc365AllSectors;
    private Integer bc365MySector;
    private Integer bc365OtherSectors;
    private Integer fcAllSectors;
    private Integer fcMySector;
    private Integer fcOtherSectors;
    private String lastBouncedChequeDate;
    private String lastCreditApplicationDate;
    private String lastFraudDate;
    private String lastNPAListingDate;
    private String lastPAListingDate;
    private String lastInsurancePolicyDate;
    private Integer npaAccountsAllSectors;
    private Integer npaAccountsMySector;
    private Integer npaAccountsOtherSectors;
    private Integer openAccountsAllSectors;
    private Integer openAccountsMySector;
    private Integer openAccountsOtherSectors;
    private Integer paAccountsAllSectors;
    private Integer paAccountsMySector;
    private Integer paAccountsOtherSectors;
    private Integer paAccountsWithDhAllSectors;
    private Integer paAccountsWithDhMySector;
    private Integer paAccountsWithDhOtherSectors;
    private Integer closedAccountsAllSectors;
    private Integer closedAccountsMySector;
    private Integer closedAccountsOtherSectors;
    private Integer caAllSectors;
    private Integer caMySector;
    private Integer caOtherSectors;
    private Integer chAllSectors;
    private Integer chMySector;
    private Integer chOtherSectors;
    private Integer enq31to60DaysAllSectors;
    private Integer enq31to60DaysMySector;
    private Integer enq31to60DaysOtherSectors;
    private Integer enq61to90DaysAllSectors;
    private Integer enq61to90DaysMySector;
    private Integer enq61to90DaysOtherSectors;
    private Integer enq91to180DaysAllSectors;
    private Integer enq91to180DaysMySector;
    private Integer enq91to180DaysOtherSectors;
    private Integer enqLast30DaysAllSectors;
    private Integer enqLast30DaysMySector;
    private Integer enqLast30DaysOtherSectors;
    private Integer paClosedAccountsAllSectors;
    private Integer paClosedAccountsMySector;
    private Integer paClosedAccountsOtherSectors;
    private Integer paClosedAccountsWithDhAllSectors;
    private Integer paClosedAccountsWithDhMySector;
    private Integer paClosedAccountsWithDhOtherSectors;
    private Integer insurancePoliciesAllSectors;
    private Integer insurancePoliciesMySector;
    private Integer insurancePoliciesOtherSectors;

    public TransUnionRwandaCrbSummaryReportData() {}

    public TransUnionRwandaCrbSummaryReportData(Integer id, Integer bcAllSectors, Integer bcMySector, Integer bcOtherSectors,
            Integer bc180AllSectors, Integer bc180MySector, Integer bc180OtherSectors, Integer bc90AllSectors, Integer bc90MySector,
            Integer bc90OtherSectors, Integer bc365AllSectors, Integer bc365MySector, Integer bc365OtherSectors, Integer fcAllSectors,
            Integer fcMySector, Integer fcOtherSectors, String lastBouncedChequeDate, String lastCreditApplicationDate,
            String lastFraudDate, String lastNPAListingDate, String lastPAListingDate, String lastInsurancePolicyDate,
            Integer npaAccountsAllSectors, Integer npaAccountsMySector, Integer npaAccountsOtherSectors, Integer openAccountsAllSectors,
            Integer openAccountsMySector, Integer openAccountsOtherSectors, Integer paAccountsAllSectors, Integer paAccountsMySector,
            Integer paAccountsOtherSectors, Integer paAccountsWithDhAllSectors, Integer paAccountsWithDhMySector,
            Integer paAccountsWithDhOtherSectors, Integer closedAccountsAllSectors, Integer closedAccountsMySector,
            Integer closedAccountsOtherSectors, Integer caAllSectors, Integer caMySector, Integer caOtherSectors, Integer chAllSectors,
            Integer chMySector, Integer chOtherSectors, Integer enq31to60DaysAllSectors, Integer enq31to60DaysMySector,
            Integer enq31to60DaysOtherSectors, Integer enq61to90DaysAllSectors, Integer enq61to90DaysMySector,
            Integer enq61to90DaysOtherSectors, Integer enq91to180DaysAllSectors, Integer enq91to180DaysMySector,
            Integer enq91to180DaysOtherSectors, Integer enqLast30DaysAllSectors, Integer enqLast30DaysMySector,
            Integer enqLast30DaysOtherSectors, Integer paClosedAccountsAllSectors, Integer paClosedAccountsMySector,
            Integer paClosedAccountsOtherSectors, Integer paClosedAccountsWithDhAllSectors, Integer paClosedAccountsWithDhMySector,
            Integer paClosedAccountsWithDhOtherSectors, Integer insurancePoliciesAllSectors, Integer insurancePoliciesMySector,
            Integer insurancePoliciesOtherSectors) {
        this.id = id;
        this.bcAllSectors = bcAllSectors;
        this.bcMySector = bcMySector;
        this.bcOtherSectors = bcOtherSectors;
        this.bc180AllSectors = bc180AllSectors;
        this.bc180MySector = bc180MySector;
        this.bc180OtherSectors = bc180OtherSectors;
        this.bc90AllSectors = bc90AllSectors;
        this.bc90MySector = bc90MySector;
        this.bc90OtherSectors = bc90OtherSectors;
        this.bc365AllSectors = bc365AllSectors;
        this.bc365MySector = bc365MySector;
        this.bc365OtherSectors = bc365OtherSectors;
        this.fcAllSectors = fcAllSectors;
        this.fcMySector = fcMySector;
        this.fcOtherSectors = fcOtherSectors;
        this.lastBouncedChequeDate = lastBouncedChequeDate;
        this.lastCreditApplicationDate = lastCreditApplicationDate;
        this.lastFraudDate = lastFraudDate;
        this.lastNPAListingDate = lastNPAListingDate;
        this.lastPAListingDate = lastPAListingDate;
        this.lastInsurancePolicyDate = lastInsurancePolicyDate;
        this.npaAccountsAllSectors = npaAccountsAllSectors;
        this.npaAccountsMySector = npaAccountsMySector;
        this.npaAccountsOtherSectors = npaAccountsOtherSectors;
        this.openAccountsAllSectors = openAccountsAllSectors;
        this.openAccountsMySector = openAccountsMySector;
        this.openAccountsOtherSectors = openAccountsOtherSectors;
        this.paAccountsAllSectors = paAccountsAllSectors;
        this.paAccountsMySector = paAccountsMySector;
        this.paAccountsOtherSectors = paAccountsOtherSectors;
        this.paAccountsWithDhAllSectors = paAccountsWithDhAllSectors;
        this.paAccountsWithDhMySector = paAccountsWithDhMySector;
        this.paAccountsWithDhOtherSectors = paAccountsWithDhOtherSectors;
        this.closedAccountsAllSectors = closedAccountsAllSectors;
        this.closedAccountsMySector = closedAccountsMySector;
        this.closedAccountsOtherSectors = closedAccountsOtherSectors;
        this.caAllSectors = caAllSectors;
        this.caMySector = caMySector;
        this.caOtherSectors = caOtherSectors;
        this.chAllSectors = chAllSectors;
        this.chMySector = chMySector;
        this.chOtherSectors = chOtherSectors;
        this.enq31to60DaysAllSectors = enq31to60DaysAllSectors;
        this.enq31to60DaysMySector = enq31to60DaysMySector;
        this.enq31to60DaysOtherSectors = enq31to60DaysOtherSectors;
        this.enq61to90DaysAllSectors = enq61to90DaysAllSectors;
        this.enq61to90DaysMySector = enq61to90DaysMySector;
        this.enq61to90DaysOtherSectors = enq61to90DaysOtherSectors;
        this.enq91to180DaysAllSectors = enq91to180DaysAllSectors;
        this.enq91to180DaysMySector = enq91to180DaysMySector;
        this.enq91to180DaysOtherSectors = enq91to180DaysOtherSectors;
        this.enqLast30DaysAllSectors = enqLast30DaysAllSectors;
        this.enqLast30DaysMySector = enqLast30DaysMySector;
        this.enqLast30DaysOtherSectors = enqLast30DaysOtherSectors;
        this.paClosedAccountsAllSectors = paClosedAccountsAllSectors;
        this.paClosedAccountsMySector = paClosedAccountsMySector;
        this.paClosedAccountsOtherSectors = paClosedAccountsOtherSectors;
        this.paClosedAccountsWithDhAllSectors = paClosedAccountsWithDhAllSectors;
        this.paClosedAccountsWithDhMySector = paClosedAccountsWithDhMySector;
        this.paClosedAccountsWithDhOtherSectors = paClosedAccountsWithDhOtherSectors;
        this.insurancePoliciesAllSectors = insurancePoliciesAllSectors;
        this.insurancePoliciesMySector = insurancePoliciesMySector;
        this.insurancePoliciesOtherSectors = insurancePoliciesOtherSectors;
    }
}
