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

UPDATE stretchy_report
SET report_sql = "
SELECT
    DISTINCT l.id,
    l.account_no AS 'Loan Number',
    st.display_name AS 'Loan Officer',
    CASE
        WHEN c.legal_form_enum = 1 THEN CONCAT(c.firstname, ' ', c.lastname)
        WHEN c.legal_form_enum = 2 THEN c.display_name
    END AS 'Client Name',
    c.external_id AS 'Client UUID',
    CASE
        WHEN c.legal_form_enum = 1 THEN 'Individual'
        WHEN c.legal_form_enum = 2 THEN 'Entity'
    END AS 'Client Type',
    p.name AS 'Loan Product',
    loanPurposeTble.code_value AS 'Purpose',
    cvd.code_value AS 'Department',
    cvs.code_value AS 'Strata',
    l.kiva_id AS 'KIVA Loan ID',
    f.name AS 'Funder',
    CASE c.legal_form_enum
        WHEN 1 THEN COALESCE(coi.national_identification_number, 'NA')
        ELSE COALESCE(cid.incorp_no, 'NA')
    END AS 'Client ID',
    c.date_of_birth AS 'Date of Birth',
    c.kiva_id AS 'KIVA Client ID',
    loancountTbl.incremental_count AS 'Cycle',
    cvc.code_value AS 'Cohort',
    CASE
        WHEN c.legal_form_enum = 1 THEN cvg.code_value
        WHEN c.legal_form_enum = 2 THEN cvbog.code_value
    END AS 'Gender',
    cvp.code_value AS 'Province',
    adr.physical_address_sector AS 'Sector',
    adr.physical_address_cell AS 'Cell',
    adr.physical_address_district AS 'District',
    CASE
        WHEN c.legal_form_enum = 1 THEN cvn.code_value
        WHEN c.legal_form_enum = 2 THEN cvbon.code_value
    END AS 'Nationality',
    coi.telephone_no AS 'Telephone',
    cai.alt_phone_no AS 'Mobile No',
    l.submittedon_date AS 'Submission Date ',
    l.approvedon_date AS 'Approval Date',
    l.disbursedon_date AS 'Disbursement Date',
    l.principal_amount_proposed AS 'Applied Amount',
    l.approved_principal AS 'Approved Amount',
    l.principal_disbursed_derived AS 'Disbursed Amount',
    (l.approved_principal - l.principal_disbursed_derived) AS 'Difference',
    currency.name AS 'Currency Type',
    CASE l.repayment_period_frequency_enum
        WHEN 0 THEN 'Daily'
        WHEN 1 THEN 'Weelky'
        WHEN 2 THEN 'Monthly'
        WHEN 3 THEN 'Yearly'
    END AS 'Re-payment Term',
    CASE
        WHEN l.loan_type_enum = 1 THEN 'Individual'
        WHEN l.loan_type_enum = 2 THEN 'Group'
        WHEN l.loan_type_enum = 3 THEN 'JLG'
        WHEN l.loan_type_enum = 4 THEN 'GLIM'
        WHEN l.loan_type_enum = 5 THEN 'GSIM'
    END AS 'Loan Type',
    l.term_frequency AS 'Terms Duration',
    IFNULL(paymentTbl1.Actual_paid, 0) AS 'Actual Payment Amount',
    IFNULL(paymentTbl.princ_paid, 0) AS 'Principal Paid',
    IFNULL(paymentTbl.int_paid, 0) AS 'Interest Paid',
    l.fee_charges_repaid_derived AS 'Insurance fee Paid',
    IFNULL(penaltiesTbl.Pen_outstanding, 0) AS 'Total Late Fees',
    IFNULL(l.total_overpaid_derived, 0) AS 'Excess Amount Paid',
    IFNULL(waiverTbl.int_waived, 0) AS 'Interest Waived',
    CASE
        WHEN l.loan_status_id IN (300, 600, 601, 602, 700)
            THEN ((l.principal_disbursed_derived + l.interest_charged_derived + l.fee_charges_charged_derived + ifnull(penaltiesTbl.Pen_charged, 0))
                - fee_charges_repaid_derived
                - IFNULL(paymentTbl.princ_paid, 0)
                - IFNULL(paymentTbl.int_paid, 0)
                - IFNULL(waiverTbl.int_waived, 0)
                - IFNULL(penaltiesTbl.pen_waived, 0)
                - IFNULL(paymentTbl.pen_paid, 0))
        ELSE 0
    END AS 'Current Balance',
    CASE
        WHEN l.loan_status_id IN (300, 600, 601, 602, 700)
            THEN (l.principal_disbursed_derived - IFNULL(paymentTbl.princ_paid, 0))
        ELSE 0
    END AS 'Principal Balance',
    CASE
        WHEN l.loan_status_id IN (300, 600, 601, 602, 700)
            THEN (l.interest_charged_derived - IFNULL(paymentTbl.int_paid, 0) - IFNULL(waiverTbl.int_waived, 0))
        ELSE 0
    END AS 'Interest Balance',
    0 AS 'Fees Balance',
    (
        CASE
            WHEN (expected_princ - IFNULL(paymentTbl.princ_paid, 0)) > 0 THEN (expected_princ - IFNULL(paymentTbl.princ_paid, 0))
            ELSE 0
        END
        +
        CASE
            WHEN (expected_int - IFNULL(waiverTbl.int_waived, 0) - IFNULL(paymentTbl.int_paid, 0)) > 0 THEN (expected_int - IFNULL(waiverTbl.int_waived, 0) - IFNULL(paymentTbl.int_paid, 0))
            ELSE 0
        END
        +
        CASE
            WHEN (expected_fees - IFNULL(paymentTbl.fee_paid, 0)) > 0 THEN (expected_fees - IFNULL(paymentTbl.fee_paid, 0))
            ELSE 0
        END
    ) AS 'Amount Past Due',
    CASE
        WHEN (expected_princ - IFNULL(paymentTbl.princ_paid, 0)) > 0 THEN (expected_princ - IFNULL(paymentTbl.princ_paid, 0))
        ELSE 0
    END AS 'Principal Past Due',
    CASE
        WHEN (expected_int - IFNULL(waiverTbl.int_waived, 0) - IFNULL(paymentTbl.int_paid, 0)) > 0 THEN (expected_int - IFNULL(waiverTbl.int_waived, 0) - IFNULL(paymentTbl.int_paid, 0))
        ELSE 0
    END AS 'Interest Past Due',
    CASE
        WHEN (expected_fees - IFNULL(paymentTbl.fee_paid, 0)) > 0 THEN (expected_fees - IFNULL(paymentTbl.fee_paid, 0))
        ELSE 0
    END AS 'Fees Past Due',
    IFNULL(nextPaymentTbl.scheduledPrincipalAmount, 0) AS 'Scheduled Principal Amount',
    IFNULL(nextPaymentTbl.scheduledInterestAmount, 0) AS 'Scheduled Interest Amount',
    IFNULL(nextPaymentTbl.scheduledFeesAmount, 0) AS 'Scheduled Fees Amount',
    IFNULL(nextPaymentTbl.scheduledPaymentAmount, 0) AS 'Scheduled Payment Amount',
    IFNULL(lastPaymentTbl.amount, 0) AS 'Last Payment Amount',
    IFNULL(lastPaymentTbl.principal_portion_derived, 0) AS 'Last Principal Amount',
    IFNULL(lastPaymentTbl.interest_portion_derived, 0) AS 'Last Interest Amount',
    IFNULL(lastPaymentTbl.fee_charges_portion_derived, 0) AS 'Last Fees Amount',
    IFNULL(lastPaymentTbl.fee_charges_portion_derived, 0) AS 'Last Late Fees Amount',
    IFNULL(l.total_overpaid_derived, 0) AS 'Last Excess Amount',
    IFNULL(
        CASE
            WHEN max_date = min_date OR max_date > min_date THEN DATEDIFF(DATE('${endDate}'), min_date)
            ELSE DATEDIFF(max_date, min_date)
        END, 0
    ) AS 'Days in Arrears',
    IFNULL(
        CASE
            WHEN max_date >= CURDATE() THEN 0
            ELSE repaymentInstalmentTbl.Instalment
        END, 0
    ) AS 'Installment in Arrears',
    lastPaymentTbl.transaction_date AS 'Last Payment Date',
    CASE
        WHEN (DATEDIFF(NOW(), laa.overdue_since_date_derived) > 0 AND l.maturedon_date < CURDATE()) THEN CURDATE()
        ELSE nextPaymentTbl.nextPaymentDueDate
    END AS 'Next Payment Due',
    l.maturedon_date AS 'Final Payment Date',
    l.closedon_date AS 'Date Closed',
    CASE
        WHEN (con.enabled = FALSE) THEN loanStatusTable.loanStatus
        WHEN con.enabled = TRUE THEN
            CASE
                WHEN loanStatusTable.loanStatus = 'Pending Approval' AND loanStatusTable.loanDecisionState IS NULL THEN loanStatusTable.loanStatus
                WHEN loanStatusTable.loanStatus = 'Pending Approval' AND loanStatusTable.loanDecisionState IS NOT NULL AND loanStatusTable.loanDecisionState != 'Prepare And Sign Contract' THEN loanStatusTable.loanDecisionState
                WHEN loanStatusTable.loanStatus != 'Pending Approval' THEN loanStatusTable.loanStatus
                WHEN loanStatusTable.loanStatus = 'Pending Approval' AND loanStatusTable.loanDecisionState = 'Prepare And Sign Contract' THEN loanStatusTable.loanStatus
            END
    END AS 'Loan Status',
    l.description AS 'Business Description',
    CASE
        WHEN c.legal_form_enum = 1 THEN bi.`Business Sector`
        WHEN c.legal_form_enum = 2 THEN cvebis.code_value
    END AS 'Industry/Sector of Activity',
    CASE
        WHEN c.legal_form_enum = 1 THEN bi.`Business Sub-Sector`
        WHEN c.legal_form_enum = 2 THEN cvebiss.code_value
    END AS 'Business Sub-Sector',
    l.created_on_utc,
    l.last_modified_on_utc
FROM
    m_office o
    JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(o.hierarchy, '%')
        AND ounder.hierarchy LIKE CONCAT('${currentUserHierarchy}', '%')
    JOIN m_client c ON c.office_id = ounder.id
    JOIN m_loan l ON l.client_id = c.id
    LEFT JOIN m_loan_arrears_aging laa ON laa.loan_id = l.id
    JOIN m_product_loan p ON p.id = l.product_id
    JOIN m_appuser u ON u.id = l.created_by
    LEFT JOIN m_staff st ON st.id = l.loan_officer_id
    LEFT JOIN m_currency currency ON currency.code = p.currency_code
    LEFT JOIN m_code_value cvg ON cvg.id = c.gender_cv_id
    LEFT JOIN m_client_other_info coi ON coi.client_id = c.id
    LEFT JOIN m_client_additional_info cai ON cai.client_id = c.id
    LEFT JOIN m_client_recruitment_survey crs ON crs.client_id = c.id
    LEFT JOIN m_code_value cvn ON cvn.id = coi.nationality_cv_id
    LEFT JOIN m_code_value cvs ON cvs.id = coi.strata_cv_id
    LEFT JOIN m_code_value cvd ON cvd.id = l.department_cv_id
    LEFT JOIN m_fund f ON f.id = l.fund_id
    LEFT JOIN m_code_value cvc ON cvc.id = crs.cohort_cv_id
    LEFT JOIN m_business_detail bd ON bd.client_id = c.id
    LEFT JOIN m_code_value cvb ON cvb.id = bd.business_type_id
    LEFT JOIN m_loan_collateral_management lcm ON lcm.loan_id = l.id
    LEFT JOIN m_client_collateral_management_additional_details ccma ON ccma.client_collateral_id = lcm.client_collateral_id
    LEFT JOIN (
        SELECT
            cdr.client_id,
            MIN(addr.physical_address_sector) AS physical_address_sector,
            MIN(addr.physical_address_district) AS physical_address_district,
            MIN(addr.physical_address_cell) AS physical_address_cell,
            MIN(addr.state_province_id) AS state_province_id
        FROM
            m_client_address cdr
            JOIN m_address addr ON addr.id = cdr.address_id
        GROUP BY
            cdr.client_id
    ) adr ON c.id = adr.client_id
    LEFT JOIN m_client_non_person cid ON c.id = cid.client_id
    LEFT JOIN m_code_value cvp ON cvp.id = adr.state_province_id
    LEFT JOIN m_code_value loanPurposeTble ON loanPurposeTble.id = l.loanpurpose_cv_id
    LEFT JOIN (
        SELECT *
        FROM m_loan_transaction lt
        WHERE lt.id IN (
            SELECT MAX(id)
            FROM m_loan_transaction t
            WHERE t.transaction_type_enum = 2
                AND t.is_reversed = 0
                AND (DATE(t.transaction_date) BETWEEN DATE('${startDate}') AND DATE('${endDate}'))
            GROUP BY t.loan_id
        )
    ) lastPaymentTbl ON lastPaymentTbl.loan_id = l.id
    LEFT JOIN (
        SELECT *
        FROM m_loan_repayment_schedule lt
        WHERE lt.id IN (
            SELECT MIN(id)
            FROM m_loan_repayment_schedule rs
            WHERE (DATE(rs.duedate) BETWEEN DATE('${startDate}') AND DATE('${endDate}'))
            GROUP BY rs.loan_id
        )
    ) maxpaydateTbl ON maxpaydateTbl.loan_id = l.id
    LEFT JOIN (
        SELECT
            t.loan_id,
            SUM(IFNULL(t.principal_portion_derived, 0)) AS princ_paid,
            SUM(IFNULL(t.interest_portion_derived, 0)) AS int_paid,
            SUM(IFNULL(t.fee_charges_portion_derived, 0)) AS fee_paid,
            SUM(IFNULL(t.penalty_charges_portion_derived, 0)) AS pen_paid
        FROM m_loan_transaction t
        WHERE t.transaction_type_enum IN (1, 2)
            AND t.is_reversed = 0
            AND (DATE(t.transaction_date) BETWEEN DATE('${startDate}') AND DATE('${endDate}'))
        GROUP BY t.loan_id
    ) paymentTbl ON paymentTbl.loan_id = l.id
    LEFT JOIN (
        SELECT
            t.loan_id,
            SUM(IFNULL(t.amount, 0)) AS Actual_paid
        FROM m_loan_transaction t
        WHERE t.transaction_type_enum IN (2)
            AND t.is_reversed = 0
            AND (DATE(t.transaction_date) BETWEEN DATE('${startDate}') AND DATE('${endDate}'))
        GROUP BY t.loan_id
    ) paymentTbl1 ON paymentTbl1.loan_id = l.id
    LEFT JOIN (
        SELECT
            t.loan_id,
            SUM(IFNULL(amount_outstanding_derived, 0)) AS Pen_outstanding,
            SUM(IFNULL(amount_paid_derived, 0)) AS Pen_paid,
            SUM(IFNULL(amount_waived_derived, 0)) AS pen_waived,
            SUM(IFNULL(amount, 0)) AS Pen_charged
        FROM m_loan_charge t
        WHERE (DATE(t.due_for_collection_as_of_date) BETWEEN DATE('${startDate}') AND DATE('${endDate}'))
        GROUP BY t.loan_id
    ) penaltiesTbl ON penaltiesTbl.loan_id = l.id
    LEFT JOIN (
        SELECT
            t.loan_id,
            SUM(IFNULL(t.interest_portion_derived, 0)) AS int_waived,
            SUM(IFNULL(t.fee_charges_portion_derived, 0)) AS fee_waived,
            SUM(IFNULL(t.penalty_charges_portion_derived, 0)) AS pen_waived
        FROM m_loan_transaction t
        WHERE t.transaction_type_enum = 4
            AND t.is_reversed = 0
            AND (DATE(t.transaction_date) BETWEEN DATE('${startDate}') AND DATE('${endDate}'))
        GROUP BY t.loan_id
    ) waiverTbl ON waiverTbl.loan_id = l.id
    LEFT JOIN m_loan_decision ld ON ld.loan_id = l.id
    LEFT JOIN c_configuration con ON con.name = 'Add-More-Stages-To-A-Loan-Life-Cycle'
    LEFT JOIN (
        SELECT
            l.id AS loanId,
            CASE
                WHEN l.loan_status_id = 100 THEN 'Pending Approval'
                WHEN l.loan_status_id = 200 THEN 'Approval'
                WHEN l.loan_status_id = 300 THEN 'Active'
                WHEN l.loan_status_id = 303 THEN 'Transfer In Progress'
                WHEN l.loan_status_id = 304 THEN 'Transfer On Hold'
                WHEN l.loan_status_id = 400 THEN 'Withdrawn By Client'
                WHEN l.loan_status_id = 500 THEN 'Rejected'
                WHEN l.loan_status_id = 600 THEN 'Closed Obligations Met'
                WHEN l.loan_status_id = 601 THEN 'Closed Written Off'
                WHEN l.loan_status_id = 602 THEN 'Closed Reschedule Outstanding Amount'
                WHEN l.loan_status_id = 700 THEN 'Overpaid'
            END AS loanStatus,
            CASE
                WHEN l.loan_decision_state = 1000 THEN 'Review Application'
                WHEN l.loan_decision_state = 1200 THEN 'Due Diligence'
                WHEN l.loan_decision_state = 1300 THEN 'Collateral Review'
                WHEN l.loan_decision_state = 1400 THEN 'IC Review Level One'
                WHEN l.loan_decision_state = 1500 THEN 'IC Review Level Two'
                WHEN l.loan_decision_state = 1600 THEN 'IC Review Level Three'
                WHEN l.loan_decision_state = 1700 THEN 'IC Review Level Four'
                WHEN l.loan_decision_state = 1800 THEN 'IC Review Level Five'
                WHEN l.loan_decision_state = 1900 THEN 'Prepare And Sign Contract'
            END AS loanDecisionState
        FROM m_loan l
        LEFT JOIN m_loan_decision ld ON ld.loan_id = l.id
    ) AS loanStatusTable ON loanStatusTable.loanId = l.id
    LEFT JOIN (
        SELECT
            COUNT(*) AS installemntsCount,
            lrs.loan_id
        FROM m_loan_repayment_schedule lrs
        WHERE lrs.completed_derived = FALSE
            AND lrs.duedate < NOW()
        GROUP BY lrs.loan_id, lrs.id
    ) installmentArrears ON installmentArrears.loan_id = l.id
    LEFT JOIN (
        SELECT
            SUM(lrs.principal_amount) AS expected_princ,
            SUM(lrs.interest_amount) AS expected_int,
            IFNULL(SUM(lrs.fee_charges_amount), 0) AS expected_fees,
            lrs.loan_id
        FROM m_loan_repayment_schedule lrs
        WHERE (DATE(lrs.duedate) BETWEEN DATE('${startDate}') AND DATE('${endDate}'))
        GROUP BY lrs.loan_id
    ) expectedrepaymentTbl ON expectedrepaymentTbl.loan_id = l.id
    LEFT JOIN (
        SELECT
            COUNT(*) AS Instalment,
            lrs.loan_id,
            MAX(lrs.duedate) AS max_date,
            MIN(lrs.duedate) AS min_date
        FROM m_loan_repayment_schedule lrs
        WHERE (DATE(lrs.duedate) BETWEEN DATE('${startDate}') AND DATE('${endDate}'))
            AND (
                (lrs.completed_derived = FALSE AND lrs.obligations_met_on_date IS NULL)
                OR lrs.obligations_met_on_date > DATE('${endDate}')
            )
        GROUP BY lrs.loan_id
    ) repaymentInstalmentTbl ON repaymentInstalmentTbl.loan_id = l.id
    LEFT JOIN (
        SELECT
            lrs.duedate AS nextPaymentDueDate,
            lrs.loan_id,
            IFNULL(lrs.principal_amount, 0) AS scheduledPrincipalAmount,
            IFNULL(lrs.interest_amount, 0) AS scheduledInterestAmount,
            IFNULL(lrs.fee_charges_amount, 0) AS scheduledFeesAmount,
            IFNULL(lrs.principal_amount, 0) + IFNULL(lrs.interest_amount, 0) AS scheduledPaymentAmount
        FROM (
            SELECT
                lrs.*,
                ROW_NUMBER() OVER (PARTITION BY lrs.loan_id ORDER BY lrs.installment ASC) AS row_num
            FROM m_loan_repayment_schedule lrs
            WHERE lrs.completed_derived = FALSE
                AND lrs.obligations_met_on_date IS NULL
                AND lrs.duedate >= CURDATE()
            GROUP BY lrs.loan_id, lrs.installment, lrs.id
            ORDER BY lrs.installment ASC
        ) lrs
        WHERE lrs.row_num = 1
    ) AS nextPaymentTbl ON nextPaymentTbl.loan_id = l.id
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
    ) loancountTbl ON loancountTbl.client_id = l.client_id AND loancountTbl.loan_id = l.id
    LEFT JOIN `Business Information` bi ON c.id = bi.client_id
    LEFT JOIN `Entity Business Information` ebi ON c.id = ebi.client_id
    LEFT JOIN m_code_value cvebis ON ebi.`BusinessSectors_cd_Business Sector` = cvebis.id
    LEFT JOIN m_code_value cvebiss ON ebi.`BusinessSubSectors_cd_Business Sub-Sector` = cvebiss.id
    LEFT JOIN `Business Owner Information` boi ON c.id = boi.client_id
    LEFT JOIN m_code_value cvbog ON boi.`Gender_cd_Gender` = cvbog.id
    LEFT JOIN m_code_value cvbon ON boi.`COUNTRY_cd_Nationality` = cvbon.id
WHERE
    o.id = ${officeId}
    AND (l.product_id = '${loanProductId}' OR '-1' = '${loanProductId}')
    AND (IFNULL(l.loan_officer_id, -10) = '${loanOfficerId}' OR '-1' = '${loanOfficerId}')
    AND (IFNULL(l.fund_id, -10) = ${fundId} OR -1 = ${fundId})
    AND (IFNULL(l.loanpurpose_cv_id, -10) = ${loanPurposeId} OR -1 = ${loanPurposeId})
    AND (l.currency_code = '${currencyId}' OR '-1' = '${currencyId}')
    AND (DATE(l.disbursedon_date) BETWEEN DATE('${startDate}') AND DATE('${endDate}'))
    AND (${loanStatusId} = -1 OR l.loan_status_id = ${loanStatusId})
"
WHERE id = (
    SELECT tbl.id
    FROM (
        SELECT sr.id
        FROM stretchy_report sr
        WHERE sr.report_name = 'Portfolio Management'
    ) AS tbl
);