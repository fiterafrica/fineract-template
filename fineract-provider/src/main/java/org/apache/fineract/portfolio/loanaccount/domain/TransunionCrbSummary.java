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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.loanaccount.data.SummaryData;

@Data
@Entity
@Table(name = "m_transunion_crb_summary")
public class TransunionCrbSummary extends AbstractPersistableCustom {

    private static final long serialVersionUID = 9181640245194392646L;
    @ManyToOne
    @JoinColumn(name = "header_id", nullable = true)
    private TransunionCrbHeader headerId;
    @Column(name = "bc_all_sectors")
    private Integer bcAllSectors;
    @Column(name = "bc_my_sector")
    private Integer bcMySector;
    @Column(name = "bc_other_sectors")
    private Integer bcOtherSectors;
    @Column(name = "bc_180_all_sectors")
    private Integer bc180AllSectors;
    @Column(name = "bc_180_my_sector")
    private Integer bc180MySector;
    @Column(name = "bc_180_other_sectors")
    private Integer bc180OtherSectors;
    @Column(name = "bc_90_all_sectors")
    private Integer bc90AllSectors;
    @Column(name = "bc_90_my_sector")
    private Integer bc90MySector;
    @Column(name = "bc_90_other_sectors")
    private Integer bc90OtherSectors;
    @Column(name = "bc_365_all_sectors")
    private Integer bc365AllSectors;
    @Column(name = "bc_365_my_sector")
    private Integer bc365MySector;
    @Column(name = "bc_365_other_sectors")
    private Integer bc365OtherSectors;
    @Column(name = "fc_all_sectors")
    private Integer fcAllSectors;
    @Column(name = "fc_my_sector")
    private Integer fcMySector;
    @Column(name = "fc_other_sectors")
    private Integer fcOtherSectors;
    @Column(name = "last_bounced_cheque_date")
    private String lastBouncedChequeDate;
    @Column(name = "last_credit_application_date")
    private String lastCreditApplicationDate;
    @Column(name = "last_fraud_date")
    private String lastFraudDate;
    @Column(name = "last_npa_listing_date")
    private String lastNPAListingDate;
    @Column(name = "last_pa_listing_date")
    private String lastPAListingDate;
    @Column(name = "last_insurance_policy_date")
    private String lastInsurancePolicyDate;
    @Column(name = "npa_accounts_all_sectors")
    private Integer npaAccountsAllSectors;
    @Column(name = "npa_accounts_my_sector")
    private Integer npaAccountsMySector;
    @Column(name = "npa_accounts_other_sectors")
    private Integer npaAccountsOtherSectors;
    @Column(name = "open_accounts_all_sectors")
    private Integer openAccountsAllSectors;
    @Column(name = "open_accounts_my_sector")
    private Integer openAccountsMySector;
    @Column(name = "open_accounts_other_sectors")
    private Integer openAccountsOtherSectors;
    @Column(name = "pa_accounts_all_sectors")
    private Integer paAccountsAllSectors;
    @Column(name = "pa_accounts_my_sector")
    private Integer paAccountsMySector;
    @Column(name = "pa_accounts_other_sectors")
    private Integer paAccountsOtherSectors;
    @Column(name = "pa_accounts_with_dh_all_sectors")
    private Integer paAccountsWithDhAllSectors;
    @Column(name = "pa_accounts_with_dh_my_sector")
    private Integer paAccountsWithDhMySector;
    @Column(name = "pa_accounts_with_dh_other_sectors")
    private Integer paAccountsWithDhOtherSectors;
    @Column(name = "closed_accounts_all_sectors")
    private Integer closedAccountsAllSectors;
    @Column(name = "closed_accounts_my_sector")
    private Integer closedAccountsMySector;
    @Column(name = "closed_accounts_other_sectors")
    private Integer closedAccountsOtherSectors;
    @Column(name = "ca_all_sectors")
    private Integer caAllSectors;
    @Column(name = "ca_my_sector")
    private Integer caMySector;
    @Column(name = "ca_other_sectors")
    private Integer caOtherSectors;
    @Column(name = "ch_all_sectors")
    private Integer chAllSectors;
    @Column(name = "ch_my_sector")
    private Integer chMySector;
    @Column(name = "ch_other_sectors")
    private Integer chOtherSectors;
    @Column(name = "enq_31_to_60_days_all_sectors")
    private Integer enq31to60DaysAllSectors;
    @Column(name = "enq_31_to_60_days_my_sector")
    private Integer enq31to60DaysMySector;
    @Column(name = "enq_31_to_60_days_other_sectors")
    private Integer enq31to60DaysOtherSectors;
    @Column(name = "enq_61_to_90_days_all_sectors")
    private Integer enq61to90DaysAllSectors;
    @Column(name = "enq_61_to_90_days_my_sector")
    private Integer enq61to90DaysMySector;
    @Column(name = "enq_61_to_90_days_other_sectors")
    private Integer enq61to90DaysOtherSectors;
    @Column(name = "enq_91_to_180_days_all_sectors")
    private Integer enq91to180DaysAllSectors;
    @Column(name = "enq_91_to_180_days_my_sector")
    private Integer enq91to180DaysMySector;
    @Column(name = "enq_91_to_180_days_other_sectors")
    private Integer enq91to180DaysOtherSectors;
    @Column(name = "enq_last_30_days_all_sectors")
    private Integer enqLast30DaysAllSectors;
    @Column(name = "enq_last_30_days_my_sector")
    private Integer enqLast30DaysMySector;
    @Column(name = "enq_last_30_days_other_sectors")
    private Integer enqLast30DaysOtherSectors;
    @Column(name = "pa_closed_accounts_all_sectors")
    private Integer paClosedAccountsAllSectors;
    @Column(name = "pa_closed_accounts_my_sector")
    private Integer paClosedAccountsMySector;
    @Column(name = "pa_closed_accounts_other_sectors")
    private Integer paClosedAccountsOtherSectors;
    @Column(name = "pa_closed_accounts_with_dh_all_sectors")
    private Integer paClosedAccountsWithDhAllSectors;
    @Column(name = "pa_closed_accounts_with_dh_my_sector")
    private Integer paClosedAccountsWithDhMySector;
    @Column(name = "pa_closed_accounts_with_dh_other_sectors")
    private Integer paClosedAccountsWithDhOtherSectors;
    @Column(name = "insurance_policies_all_sectors")
    private Integer insurancePoliciesAllSectors;
    @Column(name = "insurance_policies_my_sector")
    private Integer insurancePoliciesMySector;
    @Column(name = "insurance_policies_other_sectors")
    private Integer insurancePoliciesOtherSectors;

    public TransunionCrbSummary() {}

    public TransunionCrbSummary(TransunionCrbHeader headerId, SummaryData summary) {
        this.headerId = headerId;
        this.bcAllSectors = summary.getBouncedCheques().getAllSectors();
        this.bcMySector = summary.getBouncedCheques().getMySector();
        this.bcOtherSectors = summary.getBouncedCheques().getOtherSectors();
        this.bc180AllSectors = summary.getBouncedCheques180Days().getAllSectors();
        this.bc180MySector = summary.getBouncedCheques180Days().getMySector();
        this.bc180OtherSectors = summary.getBouncedCheques180Days().getOtherSectors();
        this.bc90AllSectors = summary.getBouncedCheques90Days().getAllSectors();
        this.bc90MySector = summary.getBouncedCheques90Days().getMySector();
        this.bc90OtherSectors = summary.getBouncedCheques90Days().getOtherSectors();
        this.bc365AllSectors = summary.getBouncedCheques365Days().getAllSectors();
        this.bc365MySector = summary.getBouncedCheques365Days().getMySector();
        this.bc365OtherSectors = summary.getBouncedCheques365Days().getOtherSectors();
        this.fcAllSectors = summary.getFraudulentCases().getAllSectors();
        this.fcMySector = summary.getFraudulentCases().getMySector();
        this.fcOtherSectors = summary.getFraudulentCases().getOtherSectors();
        this.lastBouncedChequeDate = summary.getLastBouncedChequeDate();
        this.lastCreditApplicationDate = summary.getLastCreditApplicationDate();
        this.lastFraudDate = summary.getLastFraudDate();
        this.lastNPAListingDate = summary.getLastNPAListingDate();
        this.lastPAListingDate = summary.getLastPAListingDate();
        this.lastInsurancePolicyDate = summary.getLastInsurancePolicyDate();
        this.npaAccountsAllSectors = summary.getNpaAccounts().getAllSectors();
        this.npaAccountsMySector = summary.getNpaAccounts().getMySector();
        this.npaAccountsOtherSectors = summary.getNpaAccounts().getOtherSectors();
        this.openAccountsAllSectors = summary.getOpenAccounts().getAllSectors();
        this.openAccountsMySector = summary.getOpenAccounts().getMySector();
        this.openAccountsOtherSectors = summary.getOpenAccounts().getOtherSectors();
        this.paAccountsAllSectors = summary.getPaAccounts().getAllSectors();
        this.paAccountsMySector = summary.getPaAccounts().getMySector();
        this.paAccountsOtherSectors = summary.getPaAccounts().getOtherSectors();
        this.paAccountsWithDhAllSectors = summary.getPaAccountsWithDh().getAllSectors();
        this.paAccountsWithDhMySector = summary.getPaAccountsWithDh().getMySector();
        this.paAccountsWithDhOtherSectors = summary.getPaAccountsWithDh().getOtherSectors();
        this.closedAccountsAllSectors = summary.getClosedAccounts().getAllSectors();
        this.closedAccountsMySector = summary.getClosedAccounts().getMySector();
        this.closedAccountsOtherSectors = summary.getClosedAccounts().getOtherSectors();
        this.caAllSectors = summary.getCreditApplications().getAllSectors();
        this.caMySector = summary.getCreditApplications().getMySector();
        this.caOtherSectors = summary.getCreditApplications().getOtherSectors();
        this.chAllSectors = summary.getCreditHistory().getAllSectors();
        this.chMySector = summary.getCreditHistory().getMySector();
        this.chOtherSectors = summary.getCreditHistory().getOtherSectors();
        this.enq31to60DaysAllSectors = summary.getEnquiries31to60Days().getAllSectors();
        this.enq31to60DaysMySector = summary.getEnquiries31to60Days().getMySector();
        this.enq31to60DaysOtherSectors = summary.getEnquiries31to60Days().getOtherSectors();
        this.enq61to90DaysAllSectors = summary.getEnquiries61to90Days().getAllSectors();
        this.enq61to90DaysMySector = summary.getEnquiries61to90Days().getMySector();
        this.enq61to90DaysOtherSectors = summary.getEnquiries61to90Days().getOtherSectors();
        this.enq91to180DaysAllSectors = summary.getEnquiries91Days().getAllSectors();
        this.enq91to180DaysMySector = summary.getEnquiries91Days().getMySector();
        this.enq91to180DaysOtherSectors = summary.getEnquiries91Days().getOtherSectors();
        this.enqLast30DaysAllSectors = summary.getEnquiriesLast30Days().getAllSectors();
        this.enqLast30DaysMySector = summary.getEnquiriesLast30Days().getMySector();
        this.enqLast30DaysOtherSectors = summary.getEnquiriesLast30Days().getOtherSectors();
        this.paClosedAccountsAllSectors = summary.getPaClosedAccounts().getAllSectors();
        this.paClosedAccountsMySector = summary.getPaClosedAccounts().getMySector();
        this.paClosedAccountsOtherSectors = summary.getPaClosedAccounts().getOtherSectors();
        this.paClosedAccountsWithDhAllSectors = summary.getPaClosedAccountsWithDh().getAllSectors();
        this.paClosedAccountsWithDhMySector = summary.getPaClosedAccountsWithDh().getMySector();
        this.paClosedAccountsWithDhOtherSectors = summary.getPaClosedAccountsWithDh().getOtherSectors();
        this.insurancePoliciesAllSectors = summary.getInsurancePolicies().getAllSectors();
        this.insurancePoliciesMySector = summary.getInsurancePolicies().getMySector();
        this.insurancePoliciesOtherSectors = summary.getInsurancePolicies().getOtherSectors();
    }
}
