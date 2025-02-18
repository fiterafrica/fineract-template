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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.database.DatabaseTypeResolver;
import org.apache.fineract.portfolio.loanaccount.data.TransUnionRwandaConsumerCreditData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransUnionCrbPostConsumerCreditReadPlatformServiceImpl implements TransUnionCrbPostConsumerCreditReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseTypeResolver databaseTypeResolver;

    @Override
    public Collection<TransUnionRwandaConsumerCreditData> retrieveAllConsumerCredits() {
        final ConsumerCreditMapper mapper = new ConsumerCreditMapper();
        final String sql = mapper.schema() + " order by l.id ";
        return this.jdbcTemplate.query(sql, mapper, new Object[] {});
    }

    private final class ConsumerCreditMapper implements RowMapper<TransUnionRwandaConsumerCreditData> {

        public String schema() {
            final StringBuilder sql = new StringBuilder();

            sql.append("  WITH RankedAddresses AS ( " + "    SELECT client_id, " + "           address_id, "
                    + "           ROW_NUMBER() OVER (PARTITION BY client_id ORDER BY address_id DESC) AS row_num "
                    + "    FROM m_client_address " + "  ) "
                    + "  SELECT l.id                                                                              AS loanId, "
                    + "       l.account_no                                                                      AS accountNumber, "
                    + "       l.loan_status_id                                                                  AS loanStatus, "
                    + "       l.currency_code                                                                   AS currencyType, "
                    + "       country_cv.code_value                                                             AS country, "
                    + "       mc.firstname                                                                      AS surName, ");
            if (databaseTypeResolver.isMySQL()) {
                sql.append(
                        "      COALESCE(DATEDIFF(NOW(), mlaa.overdue_since_date_derived)  ,0)                           AS daysInArrears, ");
            } else {
                sql.append(
                        "       COALESCE(CAST(EXTRACT(DAY FROM  (now()::TIMESTAMP - mlaa.overdue_since_date_derived::TIMESTAMP)) AS INTEGER),0)  AS  daysInArrears, ");
            }
            sql.append("       mc.firstname                                                                      AS foreName1, "
                    + "       mc.middlename                                                                     AS foreName2, "
                    + "       mc.lastname                                                                       AS foreName3, "
                    + "       l.principal_amount                                                           AS openingBalance, "
                    + "       CASE " + "           WHEN l.repayment_period_frequency_enum = 0 THEN 'DLY' "
                    + "           WHEN l.repayment_period_frequency_enum = 1 THEN 'WKY' "
                    + "           WHEN l.repayment_period_frequency_enum = 2 THEN 'MTH' "
                    + "           WHEN l.repayment_period_frequency_enum = 3 THEN 'ANN' " + "           ELSE 'IRR' "
                    + "           END                                                                           AS accountRepaymentTerm, "
                    + "       l.total_outstanding_derived                                                       AS currentBalance, "
                    + "       IF(l.loan_type_enum = 1, 'O', 'G')                                                AS accountOwner, "
                    + "       CASE " + "           WHEN l.repayment_period_frequency_enum = 1 THEN 'W' "
                    + "           WHEN l.repayment_period_frequency_enum = 2 THEN 'M' "
                    + "           WHEN l.repayment_period_frequency_enum = 3 THEN 'A' "
                    + "           END                                                                           AS incomeFrequency, "
                    + "       nextPaymentTbl.scheduledPaymentAmount                        AS scheduledPaymentAmount, "
                    + "       mc.mobile_no                                                                      AS mobileTelephone, "
                    + "       (l.principal_repaid_derived + l.interest_repaid_derived + l.fee_charges_repaid_derived + "
                    + "        l.penalty_charges_repaid_derived)                                                AS actualPaymentAmount, "
                    + "       l.disbursedon_date                                                                AS dateAccountOpened, "
                    + "       l.nominal_interest_rate_per_period                                                AS interestRateAtDisbursement, "
                    + "       info.number_of_dependents                                                         AS noOfDependants, "
                    + "       nationality_cv.code_value                                                         AS nationality, "
                    + "       UPPER(title_cv.code_value)                                                        AS salutation, "
                    + "       ra.postal_code                                                                    AS postalCode, "
                    + "       province_cv.code_value                                                            AS physicalAddressProvince, "
                    + "       CASE " + "           WHEN marital_cv.code_value = 'Single' THEN 'S' "
                    + "           WHEN marital_cv.code_value = 'Married' THEN 'M' "
                    + "           WHEN marital_cv.code_value = 'Divorced' THEN 'D' "
                    + "           WHEN marital_cv.code_value = 'Widowed' THEN 'W' " + "           ELSE 'O' "
                    + "           END                                                                           AS maritalStatus, "
                    + "       ra.postal_code                                                                    AS postalAddressNumber, "
                    + "       l.approvedon_date                                                                 AS approvalDate, "
                    + "       CASE " + "           WHEN gender_cv.code_value = 'Male' THEN 'M' "
                    + "           WHEN gender_cv.code_value = 'Female' THEN 'F' "
                    + "           END                                                                           AS gender, "
                    + "       first_payment.firstPaymentDate                                                    AS firstPaymentDate, "
                    + "       l.closedon_date                                                                   AS dateClosed, "
                    + "       CASE " + "           WHEN l.loan_status_id = 300 THEN 'A' "
                    + "           WHEN l.loan_status_id = 600 THEN 'C' " + "           WHEN l.loan_status_id = 601 THEN 'W' "
                    + "           WHEN l.loan_status_id = 700 THEN 'X' "
                    + "           END                                                                           AS accountStatus, "
                    + "       l.number_of_repayments                                                            AS termsDuration, "
                    + "       l.last_repayment_date                                                             AS lastPaymentDate, "
                    + "       mc.date_of_birth                                                                  AS dateOfBirth, "
                    + "       l.maturedon_date                                                                  AS finalPaymentDate, "
                    + "       mlaa.principal_overdue_derived                                                    AS amountPastDue, "
                    + "       40                                                                                AS category, "
                    + "       'Other personal service activities n.e.c.'                                        AS sectorOfActivity, "
                    + "       'I'                                                                               AS accountType, "
                    + "       ra.physical_address_district                                                      AS physicalAddressDistrict, "
                    + "       ''                                                                                AS groupName, ");
            if (databaseTypeResolver.isMySQL()) {
                sql.append(" CASE " + "    WHEN DATEDIFF(NOW(), mlaa.overdue_since_date_derived) <= 90   THEN 'C' "
                        + "    WHEN l.loan_status_id IN(600,601,700) THEN 'C' " + "    ELSE 'D' "
                        + "    END        AS currentBalanceIndicator, ");
            } else {
                sql.append(" CASE "
                        + "    WHEN EXTRACT(DAY FROM (now()::TIMESTAMP - mlaa.overdue_since_date_derived::TIMESTAMP))  <= 90   THEN 'C' "
                        + "    WHEN l.loan_status_id IN(600,601,700) THEN 'C' " + "    ELSE 'D' "
                        + "    END        AS currentBalanceIndicator, ");
            }

            sql.append("       ra.physical_address_sector                                                        AS physicalAddressSector, "
                    + "       0                                                                                 AS numberOfJointLoanParticipants, "
                    + "       ra.physical_address_cell                                                          AS physicalAddressCell, "
                    + "       13                                                                                AS nature, "
                    + "       other_info.national_identification_number                                         AS nationalId, "
                    + "       other_info.passport_number                                                        AS passportNumber, ");
            if (databaseTypeResolver.isMySQL()) {
                sql.append("       CASE " + "           WHEN mlaa.overdue_since_date_derived IS NULL OR "
                        + "                DATEDIFF(NOW(), mlaa.overdue_since_date_derived) < 30 THEN 1 "
                        + "           WHEN DATEDIFF(NOW(), mlaa.overdue_since_date_derived) BETWEEN 31 AND 90 " + "               THEN 2 "
                        + "          WHEN DATEDIFF(NOW(), mlaa.overdue_since_date_derived) BETWEEN 91 AND 180 " + "               THEN 3 "
                        + "            WHEN DATEDIFF(NOW(), mlaa.overdue_since_date_derived) BETWEEN 181 AND 365 "
                        + "               THEN 4 " + "           WHEN DATEDIFF(NOW(), mlaa.overdue_since_date_derived) BETWEEN 366 AND 719 "
                        + "               THEN 5 " + "           WHEN DATEDIFF(NOW(), mlaa.overdue_since_date_derived) > 720 THEN 6 "
                        + "          END                                                                                            AS classification, ");
            } else {
                sql.append("       CASE " + "           WHEN mlaa.overdue_since_date_derived IS NULL OR "
                        + "                EXTRACT(DAY FROM (now()::TIMESTAMP - mlaa.overdue_since_date_derived::TIMESTAMP)) < 30 THEN 1 "
                        + "           WHEN EXTRACT(DAY FROM (now()::TIMESTAMP - mlaa.overdue_since_date_derived::TIMESTAMP)) BETWEEN 31 AND 90 "
                        + "               THEN 2 "
                        + "          WHEN EXTRACT(DAY FROM (now()::TIMESTAMP - mlaa.overdue_since_date_derived::TIMESTAMP)) BETWEEN 91 AND 180 "
                        + "               THEN 3 "
                        + "            WHEN EXTRACT(DAY FROM (now()::TIMESTAMP - mlaa.overdue_since_date_derived::TIMESTAMP)) BETWEEN 181 AND 365 "
                        + "               THEN 4 "
                        + "           WHEN EXTRACT(DAY FROM (now()::TIMESTAMP - mlaa.overdue_since_date_derived::TIMESTAMP)) BETWEEN 366 AND 719 "
                        + "               THEN 5 "
                        + "           WHEN EXTRACT(DAY FROM (now()::TIMESTAMP - mlaa.overdue_since_date_derived::TIMESTAMP)) > 720 THEN 6 "
                        + "          END                                                                                            AS classification, ");
            }
            sql.append("      ''                                                                                AS emailAddress, "
                    + "       'T'                                                                               AS residenceType, "
                    + "        l.total_outstanding_derived                           AS availableCredit, "
                    + "       0                                                                                 AS income, "
                    + "       ''                                                                                AS homeTelephone, "
                    + "       ''                                                                                AS workTelephone, "
                    + "      now()                                                          AS dateAccountUpdated, "
                    + "       r.installments_in_arrears                                                         AS installmentsInArrears "
                    + "  FROM m_loan l " + "         INNER JOIN m_client mc ON l.client_id = mc.id "
                    + "         LEFT JOIN m_client_recruitment_survey mcrs ON mc.id = mcrs.client_id "
                    + "         LEFT JOIN m_code_value country_cv ON mcrs.country_cv_id = country_cv.id "
                    + "         LEFT JOIN m_loan_arrears_aging mlaa ON l.id = mlaa.loan_id "
                    + "         LEFT JOIN m_client_other_info info ON mc.id = info.client_id "
                    + "         LEFT JOIN m_code_value nationality_cv ON info.nationality_cv_id = nationality_cv.id "
                    + "         LEFT JOIN m_client_additional_info ad_info ON mc.id = ad_info.client_id "
                    + "         LEFT JOIN m_client_other_info other_info ON mc.id = other_info.client_id "
                    + "         LEFT JOIN m_code_value marital_cv ON ad_info.marital_status = marital_cv.id "
                    + "         LEFT JOIN m_code_value gender_cv ON mc.gender_cv_id = gender_cv.id "
                    + "         LEFT JOIN m_code_value title_cv ON ad_info.title = title_cv.id " + " " + "         LEFT JOIN ( "
                    + "    SELECT loan_id, " + "           transaction_date AS firstPaymentDate " + "    FROM ( "
                    + "             SELECT loan_id, " + "                    transaction_date, "
                    + "                    ROW_NUMBER() OVER (PARTITION BY loan_id ORDER BY transaction_date) AS row_num "
                    + "             FROM m_loan_transaction " + "             WHERE transaction_type_enum = 2 "
                    + "         ) ranked_transactions " + "    WHERE row_num = 1 " + " ) AS first_payment ON l.id = first_payment.loan_id "
                    + "         LEFT JOIN ( " + "    SELECT client_id, " + "           MAX(address_id) AS last_address_id "
                    + "    FROM m_client_address " + "    GROUP BY client_id "
                    + " ) AS last_client_address ON mc.id = last_client_address.client_id "
                    + "         LEFT JOIN m_address ra ON last_client_address.last_address_id = ra.id "
                    + "         LEFT JOIN (SELECT loan_id, " + "                           COUNT(*) AS installments_in_arrears "
                    + "                    FROM m_loan_repayment_schedule " + "                    WHERE duedate <= CURRENT_DATE "
                    + "                      AND completed_derived = FALSE " + "                      AND obligations_met_on_date IS NULL "
                    + "                    GROUP BY loan_id) AS r ON l.id = r.loan_id "
                    + "         LEFT JOIN m_code_value province_cv ON ra.state_province_id = province_cv.id "
                    + "             LEFT JOIN ( SELECT lrs.duedate                                                      AS nextPaymentDueDate, "
                    + "                                                    lrs.loan_id, "
                    + "                                                    IFNULL(lrs.principal_amount, 0)                                  AS scheduledPrincipalAmount, "
                    + "                                                    IFNULL(lrs.interest_amount, 0)                                   AS scheduledInterestAmount, "
                    + "                                                    IFNULL(lrs.fee_charges_amount, 0)                                AS scheduledFeesAmount, "
                    + "                                                    IFNULL(lrs.principal_amount, 0) + IFNULL(lrs.interest_amount, 0) AS scheduledPaymentAmount "
                    + "                                             FROM ( "
                    + "                                                      SELECT lrs.*,"
                    + "                                                                          ROW_NUMBER() OVER (PARTITION BY lrs.loan_id ORDER BY lrs.installment ASC) AS row_num "
                    + "                                                                   FROM m_loan_repayment_schedule lrs "
                    + "                                                                   WHERE lrs.completed_derived = false AND lrs.obligations_met_on_date IS NULL "
                    + "                                                                   GROUP BY lrs.loan_id,lrs.installment,lrs.id ORDER BY lrs.installment ASC "
                    + "                                                  ) lrs   WHERE lrs.row_num = 1 "
                    + "                                         ) AS nextPaymentTbl on nextPaymentTbl.loan_id = l.id"
                    + "  WHERE l.loan_status_id IN (300, 600, 601, 700) " + "  AND l.currency_code = 'RWF' "
                    + "  AND first_payment.firstPaymentDate IS NOT NULL " + "  AND l.last_repayment_date IS NOT NULL "
                    + "  AND mc.legal_form_enum = 1 " // 1 = individual 2= entity/corporate
                    + "  AND (l.stop_consumer_credit_upload_to_trans_union IS NULL OR l.stop_consumer_credit_upload_to_trans_union = false) ");
            return sql.toString();
        }

        @Override
        public TransUnionRwandaConsumerCreditData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
                throws SQLException {
            final Integer loanId = rs.getInt("loanId");
            final String accountNumber = rs.getString("accountNumber");
            final Integer loanStatus = rs.getInt("loanStatus");
            final String currencyType = rs.getString("currencyType");
            final String country = rs.getString("country");
            final String surName = rs.getString("surName");
            final Integer daysInArrears = rs.getInt("daysInArrears");
            final String foreName1 = rs.getString("foreName1");
            final String foreName2 = rs.getString("foreName2");
            final String foreName3 = rs.getString("foreName3");
            final Integer openingBalance = rs.getInt("openingBalance");
            final String accountRepaymentTerm = rs.getString("accountRepaymentTerm");
            final Integer currentBalance = rs.getInt("currentBalance");
            final String accountOwner = rs.getString("accountOwner");
            final String incomeFrequency = rs.getString("incomeFrequency");
            final Integer scheduledPaymentAmount = rs.getInt("scheduledPaymentAmount");

            final String phoneNumber = rs.getString("mobileTelephone");
            String mobileTelephone = (phoneNumber != null) ? phoneNumber.replace(" ", "") : null; // Remove Spaces from
                                                                                                  // phone Number
                                                                                                  // because CRB
                                                                                                  // TransUnion does not
                                                                                                  // accept spaces in
                                                                                                  // phone number.
                                                                                                  // FSI-173

            final Integer actualPaymentAmount = rs.getInt("actualPaymentAmount");
            final LocalDate dateAccountOpened = JdbcSupport.getLocalDate(rs, "dateAccountOpened");
            final Double interestRateAtDisbursement = rs.getDouble("interestRateAtDisbursement");
            final Integer noOfDependants = rs.getInt("noOfDependants");
            final String nationality = rs.getString("nationality");
            final String salutation = rs.getString("salutation");
            final String postalCode = rs.getString("postalCode");
            final String physicalAddressProvince = rs.getString("physicalAddressProvince");
            final String maritalStatus = rs.getString("maritalStatus");
            final String postalAddressNumber = rs.getString("postalAddressNumber");
            final LocalDate approvalDate = JdbcSupport.getLocalDate(rs, "approvalDate");
            final String gender = rs.getString("gender");
            final LocalDate firstPaymentDate = JdbcSupport.getLocalDate(rs, "firstPaymentDate");
            final LocalDate dateClosed = JdbcSupport.getLocalDate(rs, "dateClosed");
            final String accountStatus = rs.getString("accountStatus");
            final Integer termsDuration = rs.getInt("termsDuration");
            final LocalDate lastPaymentDate = JdbcSupport.getLocalDate(rs, "lastPaymentDate");
            final LocalDate dateOfBirth = JdbcSupport.getLocalDate(rs, "dateOfBirth");
            final LocalDate finalPaymentDate = JdbcSupport.getLocalDate(rs, "finalPaymentDate");
            final Integer amountPastDue = rs.getInt("amountPastDue");
            final Integer category = rs.getInt("category");
            final String sectorOfActivity = rs.getString("sectorOfActivity");
            final String accountType = rs.getString("accountType");
            final String physicalAddressDistrict = rs.getString("physicalAddressDistrict");
            final String groupName = rs.getString("groupName");
            final String currentBalanceIndicator = rs.getString("currentBalanceIndicator");
            final String physicalAddressSector = rs.getString("physicalAddressSector");
            final Integer numberOfJointLoanParticipants = rs.getInt("numberOfJointLoanParticipants");
            final String physicalAddressCell = rs.getString("physicalAddressCell");
            final String nationalId = rs.getString("nationalId");
            final Integer nature = rs.getInt("nature");
            final Integer installmentsInArrears = rs.getInt("installmentsInArrears");
            final Integer classification = rs.getInt("classification");
            final String emailAddress = rs.getString("emailAddress");
            final String residenceType = rs.getString("residenceType");
            final Integer availableCredit = rs.getInt("availableCredit");
            final Integer income = rs.getInt("income");
            final String homeTelephone = rs.getString("homeTelephone");
            final String workTelephone = rs.getString("workTelephone");
            final String passportNumber = rs.getString("passportNumber");
            final LocalDate dateAccountUpdated = JdbcSupport.getLocalDate(rs, "dateAccountUpdated");

            TransUnionRwandaConsumerCreditData trans = new TransUnionRwandaConsumerCreditData();
            trans.setLoanId(loanId);
            trans.setLoanStatus(loanStatus);
            trans.setCurrencyType(currencyType);
            trans.setCountry(country);
            trans.setSurName(surName);
            trans.setDaysInArrears(daysInArrears);
            trans.setForeName1(foreName1);
            trans.setForeName2(foreName2);
            trans.setForeName3(foreName3);
            trans.setOpeningBalance(openingBalance);
            trans.setAccountRepaymentTerm(accountRepaymentTerm);
            trans.setCurrentBalance(currentBalance);
            trans.setAccountOwner(accountOwner);
            trans.setIncomeFrequency(incomeFrequency);
            trans.setScheduledPaymentAmount(scheduledPaymentAmount);
            trans.setMobileTelephone(mobileTelephone);
            trans.setActualPaymentAmount(actualPaymentAmount);
            trans.setDateAccountOpened(DateUtils.convertLocalDateToLong(dateAccountOpened));
            trans.setInterestRateAtDisbursement(interestRateAtDisbursement);
            trans.setNoOfDependants(noOfDependants);
            trans.setNationality(nationality);
            trans.setSalutation(salutation);
            trans.setPostalCode(postalCode);
            trans.setPhysicalAddressProvince(physicalAddressProvince);
            trans.setMaritalStatus(maritalStatus);
            trans.setPostalAddressNumber(postalAddressNumber);
            trans.setApprovalDate(DateUtils.convertLocalDateToLong(approvalDate));
            trans.setGender(gender);
            trans.setFirstPaymentDate(DateUtils.convertLocalDateToLong(firstPaymentDate));
            trans.setDateClosed(DateUtils.convertLocalDateToLong(dateClosed));
            trans.setAccountStatus(accountStatus);
            trans.setTermsDuration(termsDuration);
            trans.setLastPaymentDate(DateUtils.convertLocalDateToLong(lastPaymentDate));
            trans.setDateOfBirth(DateUtils.convertLocalDateToLong(dateOfBirth));
            trans.setFinalPaymentDate(DateUtils.convertLocalDateToLong(finalPaymentDate));
            trans.setAmountPastDue(amountPastDue);
            trans.setCategory(category);
            trans.setSectorOfActivity(sectorOfActivity);
            trans.setAccountType(accountType);
            trans.setPhysicalAddressDistrict(physicalAddressDistrict);
            trans.setGroupName(groupName);
            trans.setCurrentBalanceIndicator(currentBalanceIndicator);
            trans.setPhysicalAddressSector(physicalAddressSector);
            trans.setNumberOfJointLoanParticipants(numberOfJointLoanParticipants);
            trans.setPhysicalAddressCell(physicalAddressCell);
            trans.setNature(nature);
            trans.setNationalId(nationalId);
            trans.setInstallmentsInArrears(installmentsInArrears);
            trans.setAccountNumber(accountNumber);
            trans.setEmailAddress(emailAddress);
            trans.setClassification(classification);
            trans.setResidenceType(residenceType);
            trans.setAvailableCredit(availableCredit);
            trans.setIncome(income);
            trans.setHomeTelephone(homeTelephone);
            trans.setWorkTelephone(workTelephone);
            trans.setDateAccountUpdated(DateUtils.convertLocalDateToLong(dateAccountUpdated));
            trans.setPassportNumber(passportNumber);

            return trans;

        }
    }
}
