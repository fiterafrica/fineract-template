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
package org.apache.fineract.portfolio.loanaccount.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaConsumerVerificationData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaCorporateVerificationData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaCrbAccountReportData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaCrbReportData;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaCrbSummaryReportData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransUnionCrbConsumerVerificationReadPlatformServiceImpl implements TransUnionCrbConsumerVerificationReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public TransUnionRwandaConsumerVerificationData retrieveConsumer(Long clientId) {
        final ConsumerCreditMapper mapper = new ConsumerCreditMapper();
        final String sql = "SELECT " + mapper.schema() + " order by cl.id ";
        return this.jdbcTemplate.queryForObject(sql, mapper, new Object[] { clientId });
    }

    @Override
    public TransUnionRwandaCorporateVerificationData retrieveCorporate(Long clientId) {
        final CorporateCreditMapper mapper = new CorporateCreditMapper();
        final String sql = "SELECT " + mapper.schema() + " order by cl.id ";
        return this.jdbcTemplate.queryForObject(sql, mapper, new Object[] { clientId });
    }

    @Override
    public TransUnionRwandaCrbReportData fetchCrbReportForTransUnion(Integer loanId) {
        final ClientCrbReportMapper mapper = new ClientCrbReportMapper();
        final String sql = "SELECT " + mapper.schema() + " order by h.id DESC LIMIT 1 ";
        return this.jdbcTemplate.queryForObject(sql, mapper, new Object[] { loanId });
    }

    @Override
    public TransUnionRwandaCrbSummaryReportData fetchCrbReportSummaryTransUnion(Integer headerId) {
        final SummaryCrbReportMapper mapper = new SummaryCrbReportMapper();
        final String sql = "SELECT " + mapper.schema() + " order by summary.id DESC LIMIT 1 ";
        return this.jdbcTemplate.queryForObject(sql, mapper, new Object[] { headerId });
    }

    @Override
    public List<TransUnionRwandaCrbAccountReportData> fetchCrbReportAccountTransUnion(Integer headerId) {
        final AccountCrbReportMapper mapper = new AccountCrbReportMapper();
        final String sql = "SELECT " + mapper.schema() + " order by ac.id DESC ";
        return this.jdbcTemplate.query(sql, mapper, headerId);
    }

    private static final class ConsumerCreditMapper implements RowMapper<TransUnionRwandaConsumerVerificationData> {

        public String schema() {
            final StringBuilder sql = new StringBuilder();

            sql.append(" cl.id                                     as id, " + "       cl.firstname                              AS name1, "
                    + "       cl.lastname                               AS name2, "
                    + "       cl.middlename                             AS name3, "
                    + "       other_info.national_identification_number AS nationalID, "
                    + "       other_info.passport_number                AS passportNo " + " FROM m_client cl "
                    + " LEFT JOIN m_client_other_info other_info on cl.id = other_info.client_id "
                    + " WHERE cl.id = ? AND cl.status_enum = 300 AND cl.legal_form_enum = 1 ");
            return sql.toString();
        }

        @Override
        public TransUnionRwandaConsumerVerificationData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
                throws SQLException {
            final Integer id = rs.getInt("id");
            final String name1 = rs.getString("name1");
            final String name2 = rs.getString("name2");
            final String name3 = rs.getString("name3");
            final String nationalID = rs.getString("nationalID");
            final String passportNo = rs.getString("passportNo");

            return new TransUnionRwandaConsumerVerificationData(id, name1, name2, name3, nationalID, passportNo);

        }
    }

    private static final class CorporateCreditMapper implements RowMapper<TransUnionRwandaCorporateVerificationData> {

        public String schema() {
            final StringBuilder sql = new StringBuilder();

            sql.append(" cl.id                                     as id, " + " cl.fullname                              AS companyName, "
                    + " mcnp.incorp_no                           AS companyRegNo " + " FROM m_client cl "
                    + " INNER JOIN  m_client_non_person mcnp on cl.id = mcnp.client_id "
                    + " WHERE cl.id = ? AND cl.status_enum = 300 AND cl.legal_form_enum = 2");
            return sql.toString();
        }

        @Override
        public TransUnionRwandaCorporateVerificationData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
                throws SQLException {
            final Integer id = rs.getInt("id");
            final String companyName = rs.getString("companyName");
            final String companyRegNo = rs.getString("companyRegNo");

            return new TransUnionRwandaCorporateVerificationData(id, companyName, companyRegNo);

        }
    }

    private static final class ClientCrbReportMapper implements RowMapper<TransUnionRwandaCrbReportData> {

        public String schema() {
            final StringBuilder sql = new StringBuilder();

            sql.append("    h.id                      AS id, " + "       h.crb_name                AS crbName, "
                    + "       h.pdf_id                  AS pdfId, " + "       h.product_display_name    AS productDisplayName, "
                    + "       h.report_date             AS reportDate, " + "       h.report_type             AS reportType, "
                    + "       h.request_no              AS requestNo, " + "       h.requester               AS requester, "
                    + "       h.created_on_utc          AS createdOnUtc, " + "       mtcpp.crn                 AS personalCrn, "
                    + "       mtcpp.date_of_birth       AS dateOfBirth, " + "       mtcpp.full_name           AS fullName, "
                    + "       mtcpp.gender              AS gender, " + "       mtcpp.health_insurance_no AS healthInsuranceNo, "
                    + "       mtcpp.marital_status      AS maritalStatus, " + "       mtcpp.national_id         AS nationalId, "
                    + "       mtcpp.other_names         AS otherNames, " + "       mtcpp.salutation          AS salutation, "
                    + "       mtcpp.surname             AS surname, " + "       mtccp.crn                 AS corporateCrn, "
                    + "       mtccp.company_reg_no      AS companyRegNo, " + "       mtccp.company_name        AS companyName, "
                    + "       mtcso.grade               AS grade, " + "       mtcso.positive_score      AS positiveScore, "
                    + "       mtcso.possibility         AS possibility, " + "       mtcso.reason_code_aarc1   AS reasonCodeAarc1, "
                    + "       mtcso.reason_code_aarc2   AS reasonCodeAarc2, " + "       mtcso.reason_code_aarc3   AS reasonCodeAarc3, "
                    + "       mtcso.reason_code_aarc4   AS reasonCodeAarc4, " + "       c.legal_form_enum         AS clientType "
                    + " FROM m_transunion_crb_header h " + "         INNER JOIN m_loan l on h.loan_id = l.id "
                    + "         INNER JOIN m_client c on h.client_id = c.id "
                    + "         LEFT JOIN m_transunion_crb_corporate_profile mtccp on h.id = mtccp.header_id "
                    + "         LEFT JOIN m_transunion_crb_personal_profile mtcpp on h.id = mtcpp.header_id "
                    + "         LEFT JOIN m_transunion_crb_score_output mtcso on h.id = mtcso.header_id " + " WHERE h.loan_id = ? ");
            return sql.toString();
        }

        @Override
        public TransUnionRwandaCrbReportData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {
            final Integer id = rs.getInt("id");
            final String crbName = rs.getString("crbName");
            final String pdfId = rs.getString("pdfId");
            final String productDisplayName = rs.getString("productDisplayName");
            final String reportDate = rs.getString("reportDate");
            final String reportType = rs.getString("reportType");
            final String requestNo = rs.getString("requestNo");
            final String requester = rs.getString("requester");
            final LocalDate createdOnDate = JdbcSupport.getLocalDate(rs, "createdOnUtc");
            final String personalCrn = rs.getString("personalCrn");
            final String dateOfBirth = rs.getString("dateOfBirth");
            final String fullName = rs.getString("fullName");
            final String gender = rs.getString("gender");
            final String healthInsuranceNo = rs.getString("healthInsuranceNo");
            final String maritalStatus = rs.getString("maritalStatus");
            final String nationalId = rs.getString("nationalId");
            final String otherNames = rs.getString("otherNames");
            final String salutation = rs.getString("salutation");
            final String surname = rs.getString("surname");
            final String corporateCrn = rs.getString("corporateCrn");
            final String companyRegNo = rs.getString("companyRegNo");
            final String companyName = rs.getString("companyName");
            final String grade = rs.getString("grade");
            final String positiveScore = rs.getString("positiveScore");
            final String possibility = rs.getString("possibility");
            final String reasonCodeAarc1 = rs.getString("reasonCodeAarc1");
            final String reasonCodeAarc2 = rs.getString("reasonCodeAarc2");
            final String reasonCodeAarc3 = rs.getString("reasonCodeAarc3");
            final String reasonCodeAarc4 = rs.getString("reasonCodeAarc4");
            final Integer clientType = rs.getInt("clientType");

            return new TransUnionRwandaCrbReportData(id, crbName, pdfId, productDisplayName, reportDate, reportType, requestNo, requester,
                    createdOnDate, personalCrn, dateOfBirth, fullName, gender, healthInsuranceNo, maritalStatus, nationalId, otherNames,
                    salutation, surname, corporateCrn, companyRegNo, companyName, grade, positiveScore, possibility, reasonCodeAarc1,
                    reasonCodeAarc2, reasonCodeAarc3, reasonCodeAarc4, clientType);

        }
    }

    private static final class SummaryCrbReportMapper implements RowMapper<TransUnionRwandaCrbSummaryReportData> {

        public String schema() {
            final StringBuilder sql = new StringBuilder();

            sql.append("    h.id                      AS id, " + "       summary.bc_all_sectors AS bcAllSectors, "
                    + "       summary.bc_my_sector AS bcMySector, " + "       summary.bc_other_sectors AS bcOtherSectors, "
                    + "       summary.bc_180_all_sectors AS bc180AllSectors, " + "       summary.bc_180_my_sector AS bc180MySector, "
                    + "       summary.bc_180_other_sectors AS bc180OtherSectors, " + "       summary.bc_90_all_sectors AS bc90AllSectors, "
                    + "       summary.bc_90_my_sector AS bc90MySector, " + "       summary.bc_90_other_sectors AS bc90OtherSectors, "
                    + "       summary.bc_365_all_sectors AS bc365AllSectors, " + "       summary.bc_365_my_sector AS bc365MySector, "
                    + "       summary.bc_365_other_sectors AS bc365OtherSectors, " + "       summary.fc_all_sectors AS fcAllSectors, "
                    + "       summary.fc_my_sector AS fcMySector, " + "       summary.fc_other_sectors AS fcOtherSectors, "
                    + "       summary.last_bounced_cheque_date AS lastBouncedChequeDate, "
                    + "       summary.last_credit_application_date AS lastCreditApplicationDate, "
                    + "       summary.last_fraud_date AS lastFraudDate, " + "       summary.last_npa_listing_date AS lastNPAListingDate, "
                    + "       summary.last_pa_listing_date AS lastPAListingDate, "
                    + "       summary.last_insurance_policy_date AS lastInsurancePolicyDate, "
                    + "       summary.npa_accounts_all_sectors AS npaAccountsAllSectors, "
                    + "       summary.npa_accounts_my_sector AS npaAccountsMySector, "
                    + "       summary.npa_accounts_other_sectors AS npaAccountsOtherSectors, "
                    + "       summary.open_accounts_all_sectors AS openAccountsAllSectors, "
                    + "       summary.open_accounts_my_sector AS openAccountsMySector, "
                    + "       summary.open_accounts_other_sectors AS openAccountsOtherSectors, "
                    + "       summary.pa_accounts_all_sectors AS paAccountsAllSectors, "
                    + "       summary.pa_accounts_my_sector AS paAccountsMySector, "
                    + "       summary.pa_accounts_other_sectors AS paAccountsOtherSectors, "
                    + "       summary.pa_accounts_with_dh_all_sectors AS paAccountsWithDhAllSectors, "
                    + "       summary.pa_accounts_with_dh_my_sector AS paAccountsWithDhMySector, "
                    + "       summary.pa_accounts_with_dh_other_sectors AS paAccountsWithDhOtherSectors, "
                    + "       summary.closed_accounts_all_sectors AS closedAccountsAllSectors, "
                    + "       summary.closed_accounts_my_sector AS closedAccountsMySector, "
                    + "       summary.closed_accounts_other_sectors AS closedAccountsOtherSectors, "
                    + "       summary.ca_all_sectors AS caAllSectors, " + "       summary.ca_my_sector AS caMySector, "
                    + "       summary.ca_other_sectors AS caOtherSectors, " + "       summary.ch_all_sectors AS chAllSectors, "
                    + "       summary.ch_my_sector AS chMySector, " + "       summary.ch_other_sectors AS chOtherSectors, "
                    + "       summary.enq_31_to_60_days_all_sectors AS enq31to60DaysAllSectors, "
                    + "       summary.enq_31_to_60_days_my_sector AS enq31to60DaysMySector, "
                    + "       summary.enq_31_to_60_days_other_sectors AS enq31to60DaysOtherSectors, "
                    + "       summary.enq_61_to_90_days_all_sectors AS enq61to90DaysAllSectors, "
                    + "       summary.enq_61_to_90_days_my_sector AS enq61to90DaysMySector, "
                    + "       summary.enq_61_to_90_days_other_sectors AS enq61to90DaysOtherSectors, "
                    + "       summary.enq_91_to_180_days_all_sectors AS enq91to180DaysAllSectors, "
                    + "       summary.enq_91_to_180_days_my_sector AS enq91to180DaysMySector, "
                    + "       summary.enq_91_to_180_days_other_sectors AS enq91to180DaysOtherSectors, "
                    + "       summary.enq_last_30_days_all_sectors AS enqLast30DaysAllSectors, "
                    + "       summary.enq_last_30_days_my_sector AS enqLast30DaysMySector, "
                    + "       summary.enq_last_30_days_other_sectors AS enqLast30DaysOtherSectors, "
                    + "       summary.pa_closed_accounts_all_sectors AS paClosedAccountsAllSectors, "
                    + "       summary.pa_closed_accounts_my_sector AS paClosedAccountsMySector, "
                    + "       summary.pa_closed_accounts_other_sectors AS paClosedAccountsOtherSectors, "
                    + "       summary.pa_closed_accounts_with_dh_all_sectors AS paClosedAccountsWithDhAllSectors, "
                    + "       summary.pa_closed_accounts_with_dh_my_sector AS paClosedAccountsWithDhMySector, "
                    + "       summary.pa_closed_accounts_with_dh_other_sectors AS paClosedAccountsWithDhOtherSectors, "
                    + "       summary.insurance_policies_my_sector AS insurancePoliciesMySector, "
                    + "       summary.insurance_policies_all_sectors AS insurancePoliciesAllSectors, "
                    + "       summary.insurance_policies_my_sector AS insurancePoliciesOtherSectors "
                    + "  FROM m_transunion_crb_summary  summary " + "  INNER JOIN m_transunion_crb_header h on summary.header_id = h.id "
                    + " WHERE h.id =  ? ");
            return sql.toString();
        }

        @Override
        public TransUnionRwandaCrbSummaryReportData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
                throws SQLException {
            final Integer id = rs.getInt("id");
            final Integer bcAllSectors = rs.getInt("bcAllSectors");
            final Integer bcMySector = rs.getInt("bcMySector");
            final Integer bcOtherSectors = rs.getInt("bcOtherSectors");
            final Integer bc180AllSectors = rs.getInt("bc180AllSectors");
            final Integer bc180MySector = rs.getInt("bc180MySector");
            final Integer bc180OtherSectors = rs.getInt("bc180OtherSectors");
            final Integer bc90AllSectors = rs.getInt("bc90AllSectors");
            final Integer bc90MySector = rs.getInt("bc90MySector");
            final Integer bc90OtherSectors = rs.getInt("bc90OtherSectors");
            final Integer bc365AllSectors = rs.getInt("bc365AllSectors");
            final Integer bc365MySector = rs.getInt("bc365MySector");
            final Integer bc365OtherSectors = rs.getInt("bc365OtherSectors");
            final Integer fcAllSectors = rs.getInt("fcAllSectors");
            final Integer fcMySector = rs.getInt("fcMySector");
            final Integer fcOtherSectors = rs.getInt("fcOtherSectors");

            final String lastBouncedChequeDate = rs.getString("lastBouncedChequeDate");
            final String lastCreditApplicationDate = rs.getString("lastCreditApplicationDate");
            final String lastFraudDate = rs.getString("lastFraudDate");
            final String lastNPAListingDate = rs.getString("lastNPAListingDate");
            final String lastPAListingDate = rs.getString("lastPAListingDate");
            final String lastInsurancePolicyDate = rs.getString("lastInsurancePolicyDate");

            final Integer npaAccountsAllSectors = rs.getInt("npaAccountsAllSectors");
            final Integer npaAccountsMySector = rs.getInt("npaAccountsMySector");
            final Integer npaAccountsOtherSectors = rs.getInt("npaAccountsOtherSectors");
            final Integer openAccountsAllSectors = rs.getInt("openAccountsAllSectors");
            final Integer openAccountsMySector = rs.getInt("openAccountsMySector");
            final Integer openAccountsOtherSectors = rs.getInt("openAccountsOtherSectors");
            final Integer paAccountsAllSectors = rs.getInt("paAccountsAllSectors");
            final Integer paAccountsMySector = rs.getInt("paAccountsMySector");
            final Integer paAccountsOtherSectors = rs.getInt("paAccountsOtherSectors");
            final Integer paAccountsWithDhAllSectors = rs.getInt("paAccountsWithDhAllSectors");
            final Integer paAccountsWithDhMySector = rs.getInt("paAccountsWithDhMySector");
            final Integer paAccountsWithDhOtherSectors = rs.getInt("paAccountsWithDhOtherSectors");
            final Integer closedAccountsAllSectors = rs.getInt("closedAccountsAllSectors");
            final Integer closedAccountsMySector = rs.getInt("closedAccountsMySector");
            final Integer closedAccountsOtherSectors = rs.getInt("closedAccountsOtherSectors");
            final Integer caAllSectors = rs.getInt("caAllSectors");
            final Integer caMySector = rs.getInt("caMySector");
            final Integer caOtherSectors = rs.getInt("caOtherSectors");
            final Integer chAllSectors = rs.getInt("chAllSectors");
            final Integer chMySector = rs.getInt("chMySector");
            final Integer chOtherSectors = rs.getInt("chOtherSectors");
            final Integer enq31to60DaysAllSectors = rs.getInt("enq31to60DaysAllSectors");
            final Integer enq31to60DaysMySector = rs.getInt("enq31to60DaysMySector");
            final Integer enq31to60DaysOtherSectors = rs.getInt("enq31to60DaysOtherSectors");
            final Integer enq61to90DaysAllSectors = rs.getInt("enq61to90DaysAllSectors");
            final Integer enq61to90DaysMySector = rs.getInt("enq61to90DaysMySector");
            final Integer enq61to90DaysOtherSectors = rs.getInt("enq61to90DaysOtherSectors");
            final Integer enq91to180DaysAllSectors = rs.getInt("enq91to180DaysAllSectors");
            final Integer enq91to180DaysMySector = rs.getInt("enq91to180DaysMySector");
            final Integer enq91to180DaysOtherSectors = rs.getInt("enq91to180DaysOtherSectors");
            final Integer enqLast30DaysAllSectors = rs.getInt("enqLast30DaysAllSectors");
            final Integer enqLast30DaysMySector = rs.getInt("enqLast30DaysMySector");
            final Integer enqLast30DaysOtherSectors = rs.getInt("enqLast30DaysOtherSectors");
            final Integer paClosedAccountsAllSectors = rs.getInt("paClosedAccountsAllSectors");
            final Integer paClosedAccountsMySector = rs.getInt("paClosedAccountsMySector");
            final Integer paClosedAccountsOtherSectors = rs.getInt("paClosedAccountsOtherSectors");
            final Integer paClosedAccountsWithDhAllSectors = rs.getInt("paClosedAccountsWithDhAllSectors");
            final Integer paClosedAccountsWithDhMySector = rs.getInt("paClosedAccountsWithDhMySector");
            final Integer paClosedAccountsWithDhOtherSectors = rs.getInt("paClosedAccountsWithDhOtherSectors");
            final Integer insurancePoliciesAllSectors = rs.getInt("insurancePoliciesAllSectors");
            final Integer insurancePoliciesMySector = rs.getInt("insurancePoliciesMySector");
            final Integer insurancePoliciesOtherSectors = rs.getInt("insurancePoliciesOtherSectors");

            return new TransUnionRwandaCrbSummaryReportData(id, bcAllSectors, bcMySector, bcOtherSectors, bc180AllSectors, bc180MySector,
                    bc180OtherSectors, bc90AllSectors, bc90MySector, bc90OtherSectors, bc365AllSectors, bc365MySector, bc365OtherSectors,
                    fcAllSectors, fcMySector, fcOtherSectors, lastBouncedChequeDate, lastCreditApplicationDate, lastFraudDate,
                    lastNPAListingDate, lastPAListingDate, lastInsurancePolicyDate, npaAccountsAllSectors, npaAccountsMySector,
                    npaAccountsOtherSectors, openAccountsAllSectors, openAccountsMySector, openAccountsOtherSectors, paAccountsAllSectors,
                    paAccountsMySector, paAccountsOtherSectors, paAccountsWithDhAllSectors, paAccountsWithDhMySector,
                    paAccountsWithDhOtherSectors, closedAccountsAllSectors, closedAccountsMySector, closedAccountsOtherSectors,
                    caAllSectors, caMySector, caOtherSectors, chAllSectors, chMySector, chOtherSectors, enq31to60DaysAllSectors,
                    enq31to60DaysMySector, enq31to60DaysOtherSectors, enq61to90DaysAllSectors, enq61to90DaysMySector,
                    enq61to90DaysOtherSectors, enq91to180DaysAllSectors, enq91to180DaysMySector, enq91to180DaysOtherSectors,
                    enqLast30DaysAllSectors, enqLast30DaysMySector, enqLast30DaysOtherSectors, paClosedAccountsAllSectors,
                    paClosedAccountsMySector, paClosedAccountsOtherSectors, paClosedAccountsWithDhAllSectors,
                    paClosedAccountsWithDhMySector, paClosedAccountsWithDhOtherSectors, insurancePoliciesAllSectors,
                    insurancePoliciesMySector, insurancePoliciesOtherSectors);

        }
    }

    private static final class AccountCrbReportMapper implements RowMapper<TransUnionRwandaCrbAccountReportData> {

        public String schema() {
            final StringBuilder sql = new StringBuilder();

            sql.append(" h.id                        AS id,  " + "       ac.account_no               AS accountNo,  "
                    + "       ac.account_opening_date     AS accountOpeningDate,  "
                    + "       ac.account_owner            AS accountOwner,  " + "       ac.account_status           AS accountStatus,  "
                    + "       ac.account_type             AS accountType,  " + "       ac.arrear_amount            AS arrearAmount,  "
                    + "       ac.arrear_days              AS arrearDays,  " + "       ac.balance_amount           AS balanceAmount,  "
                    + "       ac.currency                 AS currency,  " + "       ac.disputed                 AS disputed,  "
                    + "       ac.is_my_account            AS isMyAccount,  " + "       ac.last_payment_date        AS lastPaymentDate,  "
                    + "       ac.listing_date             AS listingDate,  " + "       ac.principal_amount         AS principalAmount,  "
                    + "       ac.repayment_duration       AS repaymentDuration,  "
                    + "       ac.repayment_term           AS repaymentTerm,  "
                    + "       ac.scheduled_payment_amount AS scheduledPaymentAmount,  "
                    + "       ac.trade_sector             AS tradeSector,  " + "       ac.worst_arrear             AS worstArrear  "
                    + " FROM m_transunion_crb_account ac  " + " INNER JOIN m_transunion_crb_header h on ac.header_id = h.id  "
                    + " WHERE h.id =   ? ");
            return sql.toString();
        }

        @Override
        public TransUnionRwandaCrbAccountReportData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
                throws SQLException {
            final Integer id = rs.getInt("id");
            final String accountNo = rs.getString("accountNo");
            final String accountOpeningDate = rs.getString("accountOpeningDate");
            final String accountOwner = rs.getString("accountOwner");
            final String accountStatus = rs.getString("accountStatus");
            final String accountType = rs.getString("accountType");
            final BigDecimal arrearAmount = rs.getBigDecimal("arrearAmount");
            final Integer arrearDays = rs.getInt("arrearDays");
            final BigDecimal balanceAmount = rs.getBigDecimal("balanceAmount");
            final String currency = rs.getString("currency");
            final Boolean disputed = rs.getBoolean("disputed");
            final Boolean isMyAccount = rs.getBoolean("isMyAccount");
            final String lastPaymentDate = rs.getString("lastPaymentDate");
            final String listingDate = rs.getString("listingDate");
            final BigDecimal principalAmount = rs.getBigDecimal("principalAmount");
            final Integer repaymentDuration = rs.getInt("repaymentDuration");
            final String repaymentTerm = rs.getString("repaymentTerm");
            final BigDecimal scheduledPaymentAmount = rs.getBigDecimal("scheduledPaymentAmount");
            final String tradeSector = rs.getString("tradeSector");
            final Integer worstArrear = rs.getInt("worstArrear");

            return new TransUnionRwandaCrbAccountReportData(id, accountNo, accountOpeningDate, accountOwner, accountStatus, accountType,
                    arrearAmount, arrearDays, balanceAmount, currency, disputed, isMyAccount, lastPaymentDate, listingDate, principalAmount,
                    repaymentDuration, repaymentTerm, scheduledPaymentAmount, tradeSector, worstArrear);

        }
    }
}
