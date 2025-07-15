--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

-- liquibase formatted sql
-- changeset fineract:1
-- MySQL dump 10.13  Distrib 5.1.60, for Win32 (ia32)
--
-- Host: localhost    Database: fineract_default
-- ------------------------------------------------------
-- Server version	5.1.60-community

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES UTF8MB4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


UPDATE stretchy_report SET report_sql = "SELECT DISTINCT
                                           c.external_id                                              AS `Client UID`,
                                           c.display_name                                             AS `Client/Company Name`,
                                           g.display_name                                             AS `Group Name`,
                                           cll.display_name                                           AS `Representative`,
                                           loancountTbl.incremental_count                             AS `Cycle`,
                                           cvg.code_value                                             AS `Gender`,
                                           c.date_of_birth                                            AS `Date of Birth`,
                                           cvs.code_value                                             AS `Strata`,
                                           bi.`Business Sector`                                       AS `Business Sector`,
                                           bi.`Business Sub-Sector`                                   AS `Business Sub-Sector`,
                                           p.name                                                     AS `Loan Product`,
                                           loanPurposeTble.code_value                                 AS `Purpose of the loan`,
                                           l.principal_amount_proposed                                AS `Applied Amount`,
                                           l.submittedon_date                                         AS `Submission Date`,
                                           ld.due_diligence_recommended_amount                         AS `Due Diligence Recommended amount`,
                                           icReviewTbl.IC_Level_One                                   AS `IC Decision Level One`,
                                           ld.ic_review_decision_level_one_on                         AS `IC Decision Level One Date`,
                                           ld.ic_review_decision_level_one_recommended_amount         AS `IC Decision Level One Recommended Amount`,
                                           icReviewTbl.IC_Level_Two                                   AS `IC Decision Level Two`,
                                           ld.ic_review_decision_level_two_on                         AS `IC Decision Level Two Date`,
                                           ld.ic_review_decision_level_two_recommended_amount         AS `IC Decision Level Two Recommended Amount`,
                                           icReviewTbl.IC_Level_Three                                 AS `IC Decision Level Three`,
                                           ld.ic_review_decision_level_three_on                       AS `IC Decision Level Three Date`,
                                           ld.ic_review_decision_level_three_recommended_amount       AS `IC Decision Level Three Recommended Amount`,
                                           icReviewTbl.IC_Level_Four                                  AS `IC Decision Level Four`,
                                           ld.ic_review_decision_level_four_on                        AS `IC Decision Level Four Date`,
                                           ld.ic_review_decision_level_four_recommended_amount        AS `IC Decision Level Four Recommended Amount`,
                                           icReviewTbl.IC_Level_Five                                  AS `IC Decision Level Five`,
                                           ld.ic_review_decision_level_five_on                        AS `IC Decision Level Five Date`,
                                           ld.ic_review_decision_level_five_recommended_amount        AS `IC Decision Level Five Recommended Amount`,
                                           CASE
                                             WHEN l.loan_status_id = 500
                                               OR (l.loan_status_id = 100
                                                   AND (l.loan_decision_state IN (1000,1200) OR l.loan_decision_state IS NULL))
                                             THEN 0
                                             ELSE l.approved_icreview
                                           END                                                        AS `IC Approved Amount`,
                                           l.approvedon_date                                          AS `Approval Date`,
                                           l.repay_every                                             AS `Repayment Frequency`,
                                           CASE l.repayment_period_frequency_enum
                                             WHEN 0 THEN 'Daily'
                                             WHEN 1 THEN 'Weekly'
                                             WHEN 2 THEN 'Monthly'
                                             WHEN 3 THEN 'Yearly'
                                           END                                                        AS `Re-payment Term`,
                                           l.term_frequency                                           AS `Loan Tenure in months`,
                                           ''                                                         AS `IC Approved Amount in Words`,
                                           (instalmentAmntTbl.principal_amount
                                            + IFNULL(instalmentAmntTbl.interest_amount,0)
                                            + IFNULL(instalmentAmntTbl.fee_charges_amount,0)
                                           )                                                           AS `Installment Amount`,
                                           coi.tax_identification_number                              AS `Company Tin Number`,
                                           cvn.code_value                                             AS `Nationality`,
                                           villageTbl.code_value                                      AS `Village`,
                                           addr.physical_address_cell                                 AS `Cell`,
                                           addr.physical_address_sector                               AS `Sector`,
                                           addr.physical_address_district                             AS `District`,
                                           c.mobile_no                                                AS `Mpesa/ Momo number`,
                                           cai.alt_phone_no                                           AS `Fixed Phone Number`,
                                           coi.co_signors                                             AS `Cosigner Names`,
                                           l.account_no                                               AS `Loan ID Number`,
                                           coi.bank_account_number                                    AS `Bank Account Number`,
                                           coi.bank_name                                              AS `Bank Name`,
                                           ccma.UPI_NO                                                AS `UPI Number`,
                                           villageTbl.code_value                                      AS `Collateral Village`,
                                           cellTbl.code_value                                         AS `Collateral Cell`,
                                           sectorTbl.code_value                                       AS `Collateral Sector`,
                                           districtTbl.code_value                                     AS `Collateral District`,
                                           cvp.code_value                                             AS `Province`,
                                           ccma.collateral_owner_first                                AS `Owners Name 1`,
                                           ccma.collateral_owner_second                               AS `Owners Name 2`,
                                           ccma.id_no_of_collateral_owner_first                       AS `Owners ID 1`,
                                           ccma.id_no_of_collateral_owner_second                      AS `Owners ID 2`,
                                           ccma.worth_of_collateral                                   AS `OMV Figures`,
                                           CONCAT(COALESCE(nxtofkintbl.lastname,''), ' ', COALESCE(nxtofkintbl.firstname,'')) AS `Co-signer`,
                                           coi.national_identification_number                         AS `National/Refugee ID`,
                                           CASE
                                             WHEN con.enabled = FALSE THEN loanStatusTable.loanStatus
                                             ELSE CASE
                                               WHEN loanStatusTable.loanStatus = 'Pending Approval'
                                                    AND loanStatusTable.loanDecisionState = 'Pending Approval' THEN 'Pending Approval'
                                               WHEN loanStatusTable.loanStatus = 'Pending Approval'
                                                    AND loanStatusTable.loanDecisionState IS NULL          THEN 'Review Application'
                                               WHEN loanStatusTable.loanStatus = 'Pending Approval'
                                                    AND loanStatusTable.next_loan_ic_review_decision_state = 1900 THEN 'Prepare And Sign Contract'
                                               WHEN loanStatusTable.loanStatus = 'Pending Approval'
                                                    AND loanStatusTable.loan_decision_state = 1000         THEN 'Due Diligence'
                                               WHEN loanStatusTable.loanStatus = 'Pending Approval'
                                                    AND loanStatusTable.loan_decision_state = 1200         THEN 'IC Review Level One'
                                               WHEN loanStatusTable.loanStatus = 'Pending Approval'
                                                    AND loanStatusTable.next_loan_ic_review_decision_state = 1500 THEN 'IC Review Level Two'
                                               WHEN loanStatusTable.loanStatus = 'Pending Approval'
                                                    AND loanStatusTable.next_loan_ic_review_decision_state = 1600 THEN 'IC Review Level Three'
                                               WHEN loanStatusTable.loanStatus = 'Pending Approval'
                                                    AND loanStatusTable.next_loan_ic_review_decision_state = 1700 THEN 'IC Review Level Four'
                                               WHEN loanStatusTable.loanStatus = 'Pending Approval'
                                                    AND loanStatusTable.next_loan_ic_review_decision_state = 1800 THEN 'IC Review Level Five'
                                               ELSE loanStatusTable.loanStatus
                                             END
                                           END                                                        AS `Loan Status`,
                                           CASE WHEN l.loan_status_id = 200 THEN ld.prepare_and_sign_contract_on ELSE NULL END AS `Contract Upload Date`,
                                           CONCAT(gt.firstname, ' ', gt.lastname)                      AS `Guarantor Name`,
                                           gt.mobile_number                                           AS `Guarantor Phone`,
                                           gt.client_id                                               AS `Guarantor Client ID`
                                         FROM m_office o
                                           JOIN m_office ounder
                                             ON ounder.hierarchy LIKE CONCAT(o.hierarchy,'%')
                                            AND ounder.hierarchy LIKE CONCAT('${currentUserHierarchy}','%')
                                           JOIN m_client c
                                             ON c.office_id = ounder.id
                                           JOIN m_loan l
                                             ON l.client_id = c.id
                                           LEFT JOIN m_group_client gc
                                             ON gc.client_id = c.id
                                           LEFT JOIN m_group g
                                             ON g.id = gc.group_id
                                           LEFT JOIN m_client cll
                                             ON cll.id = g.representative_id
                                           LEFT JOIN m_loan_arrears_aging laa
                                             ON laa.loan_id = l.id
                                           JOIN m_product_loan p
                                             ON p.id = l.product_id
                                           LEFT JOIN m_code_value cvg
                                             ON cvg.id = c.gender_cv_id
                                           LEFT JOIN m_client_other_info coi
                                             ON coi.client_id = c.id
                                           LEFT JOIN m_client_additional_info cai
                                             ON cai.client_id = c.id
                                           LEFT JOIN m_client_recruitment_survey crs
                                             ON crs.client_id = c.id
                                           LEFT JOIN m_code_value cvn
                                             ON cvn.id = coi.nationality_cv_id
                                           LEFT JOIN m_code_value cvs
                                             ON cvs.id = coi.strata_cv_id
                                           LEFT JOIN m_business_detail bd
                                             ON bd.client_id = c.id
                                           LEFT JOIN m_code_value cvb
                                             ON cvb.id = bd.business_type_id
                                           LEFT JOIN m_client_collateral_management lcm
                                             ON lcm.client_id = c.id
                                           LEFT JOIN m_client_collateral_management_additional_details ccma
                                             ON ccma.client_collateral_id = lcm.id
                                           LEFT JOIN m_code_value cvp
                                             ON cvp.id = ccma.province_cv_id
                                           LEFT JOIN m_code_value loanPurposeTble
                                             ON loanPurposeTble.id = l.loanpurpose_cv_id
                                           LEFT JOIN m_code_value villageTbl
                                             ON villageTbl.id = ccma.village_cv_id
                                           LEFT JOIN m_code_value districtTbl
                                             ON districtTbl.id = ccma.district_cv_id
                                           LEFT JOIN m_code_value cellTbl
                                             ON cellTbl.id = ccma.cell_cv_id
                                           LEFT JOIN m_code_value sectorTbl
                                             ON sectorTbl.id = ccma.sector_cv_id
                                           LEFT JOIN m_client_address cla
                                             ON cla.client_id = c.id
                                           LEFT JOIN m_address addr
                                             ON addr.id = cla.address_id
                                           LEFT JOIN (
                                             SELECT
                                               ld.loan_id,
                                               CASE
                                                 WHEN ld.loan_decision_state = 1000 THEN 'Review Application'
                                                 WHEN ld.loan_decision_state = 1200 THEN 'Due Diligence'
                                                 WHEN ld.loan_decision_state = 1300 THEN 'Collateral Review'
                                                 WHEN ld.loan_decision_state = 1400 THEN 'IC Review Level One'
                                                 WHEN ld.loan_decision_state = 1500 THEN 'IC Review Level Two'
                                                 WHEN ld.loan_decision_state = 1600 THEN 'IC Review Level Three'
                                                 WHEN ld.loan_decision_state = 1700 THEN 'IC Review Level Four'
                                                 WHEN ld.loan_decision_state = 1800 THEN 'IC Review Level Five'
                                                 WHEN ld.loan_decision_state = 1900 THEN 'Prepare And Sign Contract'
                                               END                                                     AS loanDecisionState,
                                               ld.next_loan_ic_review_decision_state                    AS `Next IC Level`,
                                               IF(ld.is_ic_review_decision_level_one_signed,'Approved',IF(ld.is_reject_ic_review_decision_level_one,'Rejected','')) AS IC_Level_One,
                                               IF(ld.is_ic_review_decision_level_two_signed,'Approved',IF(ld.is_reject_ic_review_decision_level_two,'Rejected','')) AS IC_Level_Two,
                                               IF(ld.is_ic_review_decision_level_three_signed,'Approved',IF(ld.is_reject_ic_review_decision_level_three,'Rejected','')) AS IC_Level_Three,
                                               IF(ld.is_ic_review_decision_level_four_signed,'Approved',IF(ld.is_reject_ic_review_decision_level_four,'Rejected','')) AS IC_Level_Four,
                                               IF(ld.is_ic_review_decision_level_five_signed,'Approved',IF(ld.is_reject_ic_review_decision_level_five,'Rejected','')) AS IC_Level_Five
                                             FROM m_loan_decision ld
                                           ) AS icReviewTbl
                                             ON icReviewTbl.loan_id = l.id
                                           LEFT JOIN m_loan_decision ld
                                             ON ld.loan_id = l.id
                                           LEFT JOIN c_configuration con
                                             ON con.name = 'Add-More-Stages-To-A-Loan-Life-Cycle'
                                           JOIN (
                                             SELECT
                                               l.id                                                   AS loanId,
                                               l.loan_decision_state,
                                               ld.next_loan_ic_review_decision_state,
                                               CASE
                                                 WHEN l.loan_status_id = 100 THEN 'Pending Approval'
                                                 WHEN l.loan_status_id = 200 THEN 'Pending Disbursement'
                                                 WHEN l.loan_status_id = 300 THEN 'Active'
                                                 WHEN l.loan_status_id = 303 THEN 'Transfer In Progress'
                                                 WHEN l.loan_status_id = 304 THEN 'Transfer On Hold'
                                                 WHEN l.loan_status_id = 400 THEN 'Withdrawn By Client'
                                                 WHEN l.loan_status_id = 500 THEN 'Rejected'
                                                 WHEN l.loan_status_id = 600 THEN 'Closed Obligations Met'
                                                 WHEN l.loan_status_id = 601 THEN 'Closed Written Off'
                                                 WHEN l.loan_status_id = 602 THEN 'Closed Reschedule Outstanding Amount'
                                                 WHEN l.loan_status_id = 700 THEN 'Overpaid'
                                               END                                                     AS loanStatus,
                                               CASE
                                                 WHEN l.loan_decision_state = 1000 THEN 'Due Diligence'
                                                 WHEN l.loan_decision_state = 1200 THEN 'IC Review Level One'
                                                 WHEN l.loan_decision_state = 1400 THEN 'IC Review Level Two'
                                                 WHEN l.loan_decision_state = 1500 THEN 'IC Review Level Three'
                                                 WHEN l.loan_decision_state = 1600 THEN 'IC Review Level Four'
                                                 WHEN l.loan_decision_state = 1700 THEN 'IC Review Level Five'
                                                 WHEN l.loan_decision_state = 1800 THEN 'Prepare And Sign Contract'
                                                 WHEN l.loan_decision_state = 1900 THEN 'Pending Approval'
                                               END                                                     AS loanDecisionState
                                             FROM m_loan l
                                             LEFT JOIN m_loan_decision ld
                                               ON ld.loan_id = l.id
                                           ) AS loanStatusTable
                                             ON loanStatusTable.loanId = l.id
                                           LEFT JOIN (
                                             SELECT *
                                             FROM m_loan_repayment_schedule lt
                                             WHERE lt.id IN (
                                               SELECT MIN(id)
                                               FROM m_loan_repayment_schedule rs
                                               GROUP BY rs.loan_id
                                             )
                                           ) AS instalmentAmntTbl
                                             ON instalmentAmntTbl.loan_id = l.id
                                           LEFT JOIN (
                                             SELECT
                                               account_no,
                                               disbursedon_date,
                                               created_on_utc,
                                               submittedon_date,
                                               client_id,
                                               id AS loan_id,
                                               CAST(ROW_NUMBER() OVER (PARTITION BY client_id ORDER BY submittedon_date) AS SIGNED) AS incremental_count
                                             FROM m_loan
                                           ) AS loancountTbl
                                             ON loancountTbl.client_id = l.client_id
                                            AND loancountTbl.loan_id   = l.id
                                           LEFT JOIN (
                                             SELECT *
                                             FROM m_family_members fm
                                             WHERE fm.id IN (
                                               SELECT MIN(id)
                                               FROM m_family_members rs
                                               GROUP BY rs.client_id
                                             )
                                           ) AS nxtofkintbl
                                             ON nxtofkintbl.client_id = c.id
                                           LEFT JOIN `Business Information` bi
                                             ON bi.client_id = c.id
                                           LEFT JOIN m_guarantor gt
                                             ON gt.loan_id = l.id
                                         WHERE
                                           l.loan_status_id NOT IN (300,600,602,700)
                                           AND o.id = ${officeId}
                                           AND (l.product_id = '${loanProductId}' OR '-1' = '${loanProductId}')
                                           AND (IFNULL(l.loan_officer_id, -10) = '${loanOfficerId}' OR '-1' = '${loanOfficerId}')
                                           AND (IFNULL(l.fund_id, -10) = ${fundId} OR -1 = ${fundId})
                                           AND (IFNULL(l.loanpurpose_cv_id, -10) = ${loanPurposeId} OR -1 = ${loanPurposeId})
                                           AND DATE(l.last_modified_on_utc)
                                               BETWEEN DATE('${startDate}') AND DATE('${endDate}')";
