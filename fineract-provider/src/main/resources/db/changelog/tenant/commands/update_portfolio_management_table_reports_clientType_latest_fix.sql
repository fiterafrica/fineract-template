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

UPDATE stretchy_report SET report_sql = "    select
                                                 DISTINCT l.id,
                                                 l.account_no as 'Loan Number',
                                                 st.display_name as 'Loan Officer',
                                                 case
                                                  when c.legal_form_enum = 1 then concat(c.firstname, ' ', c.lastname)
                                                  when c.legal_form_enum = 2 then c.display_name
                                                  end                                                       as 'Client Name',
                                                 c.external_id as 'Client UUID',
                                                  case
                                                    when c.legal_form_enum = 1 then 'Individual'
                                                    when c.legal_form_enum = 2 then 'Entity'
                                                    end                                                         as 'Client Type',
                                                 p.name as 'Loan Product',
                                                 loanPurposeTble.code_value as 'Purpose',
                                                 cvd.code_value as 'Department',
                                                 cvs.code_value as 'Strata',
                                                 l.kiva_id as 'KIVA Loan ID',
                                                 f.name as 'Funder',
                                                 case
                                                     c.legal_form_enum when 1 then coalesce(coi.national_identification_number, 'NA')
                                                     else coalesce(cid.incorp_no, 'NA')
                                                 end as 'Client ID',
                                                 c.date_of_birth as 'Date of Birth',
                                                 c.kiva_id as 'KIVA Client ID',
                                                 loancountTbl.incremental_count as 'Cycle',
                                                 cvc.code_value as 'Cohort',
                                                 cvg.code_value as 'Gender',
                                                 cvp.code_value as 'Province',
                                                 adr.physical_address_sector as 'Sector',
                                                 adr.physical_address_cell as 'Cell' ,
                                                 adr.physical_address_district as 'District',
                                                 cvn.code_value as 'Nationality',
                                                 coi.telephone_no as 'Telephone',
                                                 cai.alt_phone_no as 'Mobile No',
                                                 l.submittedon_date as 'Submission Date ',
                                                 l.approvedon_date as 'Approval Date',
                                                 l.disbursedon_date as 'Disbursement Date',
                                                 l.principal_amount_proposed as 'Applied Amount',
                                                 l.approved_principal as 'Approved Amount',
                                                 l.principal_disbursed_derived as 'Disbursed Amount',
                                                 (l.approved_principal - l.principal_disbursed_derived) as 'Difference',
                                                 currency.name as 'Currency Type',
                                                 case
                                                     l.repayment_period_frequency_enum when 0 then 'Daily'
                                                     when 1 then 'Weelky'
                                                     when 2 then 'Monthly'
                                                     when 3 then 'Yearly'
                                                 end as 'Re-payment Term',
                                                 case
                                                     when l.loan_type_enum = 1 then 'Individual'
                                                     when l.loan_type_enum = 2 then 'Group'
                                                     when l.loan_type_enum = 3 then 'JLG'
                                                     when l.loan_type_enum = 4 then 'GLIM'
                                                     when l.loan_type_enum = 5 then 'GSIM'
                                                 end as 'Loan Type',
                                                 l.term_frequency as 'Terms Duration',
                                                 ifnull(paymentTbl1.Actual_paid, 0) as 'Actual Payment Amount',
                                                 ifnull(paymentTbl.princ_paid, 0)as 'Principal Paid' ,
                                                 ifnull(paymentTbl.int_paid, 0) as 'Interest Paid',
                                                 l.fee_charges_repaid_derived as 'Insurance fee Paid',
                                                 ifnull(penaltiesTbl.Pen_outstanding, 0) as 'Total Late Fees',
                                                 ifnull(l.total_overpaid_derived, 0) as 'Excess Amount Paid',
                                                 ifnull(waiverTbl.int_waived, 0) as 'Interest Waived' ,
                                                case
                                                     when l.loan_status_id in (300, 600, 601, 602, 700) then( l.total_expected_repayment_derived -fee_charges_repaid_derived - ifnull(paymentTbl.princ_paid, 0)- ifnull(paymentTbl.int_paid, 0)-ifnull(waiverTbl.int_waived, 0)-ifnull(penaltiesTbl.pen_waived, 0)- ifnull(penaltiesTbl.Pen_paid, 0))
                                                     else 0
                                                 end as 'Current Balance',
                                                 case
                                                     when l.loan_status_id in(300, 600, 601, 602, 700) then (l.principal_disbursed_derived- ifnull(paymentTbl.princ_paid, 0))
                                                     else 0
                                                 end as 'Principal Balance',
                                                 case
                                                     when l.loan_status_id in (300, 600, 601, 602, 700) then (l.interest_charged_derived - ifnull(paymentTbl.int_paid, 0)-ifnull(waiverTbl.int_waived, 0))
                                                     else 0
                                                 end as 'Interest Balance',
                                                 0 as 'Fees Balance',
                                                 (case
                                                     when (expected_princ - ifnull(paymentTbl.princ_paid, 0)) > 0 then (expected_princ - ifnull(paymentTbl.princ_paid, 0))
                                                     when (expected_princ - ifnull(paymentTbl.princ_paid, 0)) <= 0 then 0
                                                     else 0
                                                 end) + (case
                                                     when (expected_int- ifnull(waiverTbl.int_waived, 0)- ifnull(paymentTbl.int_paid, 0)) > 0 then (expected_int - ifnull(waiverTbl.int_waived, 0) - ifnull(paymentTbl.int_paid, 0))
                                                     when (expected_int -ifnull(waiverTbl.int_waived, 0) - ifnull(paymentTbl.int_paid, 0)) <= 0 then 0
                                                     else 0
                                                 end) + (case
                                                     when (expected_fees - ifnull(paymentTbl.fee_paid, 0)) > 0 then (expected_fees - ifnull(paymentTbl.fee_paid, 0))
                                                     when (expected_fees - ifnull(paymentTbl.fee_paid, 0)) <= 0 then 0
                                                     else 0
                                                 end) as 'Amount Past Due',
                                                 case
                                                     when (expected_princ - ifnull(paymentTbl.princ_paid, 0)) > 0 then (expected_princ - ifnull(paymentTbl.princ_paid, 0))
                                                     when (expected_princ - ifnull(paymentTbl.princ_paid, 0)) <= 0 then 0
                                                     else 0
                                                 end as 'Principal Past Due',
                                                 case
                                                     when (expected_int- ifnull(waiverTbl.int_waived, 0)- ifnull(paymentTbl.int_paid, 0)) > 0 then (expected_int - ifnull(waiverTbl.int_waived, 0) - ifnull(paymentTbl.int_paid, 0))
                                                     when (expected_int -ifnull(waiverTbl.int_waived, 0) - ifnull(paymentTbl.int_paid, 0)) <= 0 then 0
                                                     else 0
                                                 end as 'Interest Past Due',
                                                 case
                                                     when (expected_fees - ifnull(paymentTbl.fee_paid, 0)) > 0 then (expected_fees - ifnull(paymentTbl.fee_paid, 0))
                                                     when (expected_fees - ifnull(paymentTbl.fee_paid, 0)) <= 0 then 0
                                                     else 0
                                                 end as 'Fees Past Due',
                                                 ifnull(nextPaymentTbl.scheduledPrincipalAmount, 0) as 'Scheduled Principal Amount',
                                                 ifnull(nextPaymentTbl.scheduledInterestAmount, 0) as 'Scheduled Interest Amount',
                                                 ifnull(nextPaymentTbl.scheduledFeesAmount, 0) as 'Scheduled Fees Amount',
                                                 ifnull(nextPaymentTbl.scheduledPaymentAmount, 0) as 'Scheduled Payment Amount',
                                                 ifnull(lastPaymentTbl.amount, 0) as 'Last Payment Amount',
                                                 ifnull(lastPaymentTbl.principal_portion_derived, 0) as 'Last Principal Amount',
                                                 ifnull(lastPaymentTbl.interest_portion_derived, 0) as 'Last Interest Amount',
                                                 ifnull(lastPaymentTbl.fee_charges_portion_derived, 0) as 'Last Fees Amount',
                                                 ifnull(lastPaymentTbl.fee_charges_portion_derived, 0) as 'Last Late Fees Amount',
                                                 ifnull(l.total_overpaid_derived, 0) as 'Last Excess Amount',
                                                 /* datediff(now(), laa.overdue_since_date_derived)               as 'Days in Arrears',*/
                                                 ifnull(case when max_date = min_date or max_date>min_date then datediff(date('${endDate}'), min_date) else datediff(max_date, min_date) End, 0) as 'Days in Arrears' /*,                                                          case when ((expected_princ - ifnull(paymentTbl.princ_paid,0)) or (expected_int- ifnull(waiverTbl.int_waived,0)- ifnull(paymentTbl.int_paid,0))) > 0 then repaymentInstalmentTbl.Instalment   else 0 end as 'Installment in Arrears',                                                                                          ifnull(repaymentInstalmentTbl.Instalment,0) as 'Installment in Arrears'*/
                                                 ,
                                                 ifnull(case when max_date >= curdate() then 0 else repaymentInstalmentTbl.Instalment end , 0 )as 'Installment in Arrears',
                                                 lastPaymentTbl.transaction_date as 'Last Payment Date',
                                                case
                                                     when (datediff(now(), laa.overdue_since_date_derived)>0
                                                     and l.maturedon_date<curdate()) then curdate()
                                                     else nextPaymentTbl.nextPaymentDueDate
                                                 end as 'Next Payment Due',
                                                 l.maturedon_date as 'Final Payment Date',
                                                 l.closedon_date as 'Date Closed',
                                                 case
                                                     when (con.enabled = false) then loanStatusTable.loanStatus
                                                     when con.enabled = true then case
                                                         when loanStatusTable.loanStatus = 'Pending Approval'
                                                         and loanStatusTable.loanDecisionState is null then loanStatusTable.loanStatus
                                                         when loanStatusTable.loanStatus = 'Pending Approval'
                                                         and loanStatusTable.loanDecisionState is not null
                                                         and loanStatusTable.loanDecisionState != 'Prepare And Sign Contract' then loanStatusTable.loanDecisionState
                                                         when loanStatusTable.loanStatus != 'Pending Approval' then loanStatusTable.loanStatus
                                                         when loanStatusTable.loanStatus = 'Pending Approval'
                                                         and loanStatusTable.loanDecisionState = 'Prepare And Sign Contract' then loanStatusTable.loanStatus
                                                     end
                                                 end as 'Loan Status' ,
                                                 l.description as 'Business Description' ,
                                                 bi.`Business Sector` as 'Industry/Sector of Activity',
                                                 bi.`Business Sub-Sector`,
                                                 l.created_on_utc,
                                                 l.last_modified_on_utc
                                             from
                                                 m_office o
                                             join m_office ounder ON
                                                 ounder.hierarchy like concat(o.hierarchy, '%')
                                                 AND ounder.hierarchy like CONCAT('${currentUserHierarchy}', '%')
                                             join m_client c ON
                                                 c.office_id = ounder.id
                                             join m_loan l ON
                                                 l.client_id = c.id
                                             left join m_loan_arrears_aging laa on
                                                 laa.loan_id = l.id
                                             join m_product_loan p ON
                                                 p.id = l.product_id
                                             join m_appuser u on
                                                 u.id = l.created_by
                                             left join m_staff st on
                                                 st.id = l.loan_officer_id
                                             left join m_currency currency ON
                                                 currency.code = p.currency_code
                                             left join m_code_value cvg ON
                                                 cvg.id = c.gender_cv_id
                                             left join m_client_other_info coi ON
                                                 coi.client_id = c.id
                                             left join m_client_additional_info cai on
                                                 cai.client_id = c.id
                                             left join m_client_recruitment_survey crs on
                                                 crs.client_id = c.id
                                             left join m_code_value cvn ON
                                                 cvn.id = coi.nationality_cv_id
                                             left join m_code_value cvs ON
                                                 cvs.id = coi.strata_cv_id
                                             left join m_code_value cvd ON
                                                 cvd.id = l.department_cv_id
                                             left join m_fund f ON
                                                 f.id = l.fund_id
                                             left join m_code_value cvc on
                                                 cvc.id = crs.cohort_cv_id
                                             left join m_business_detail bd on
                                                 bd.client_id = c.id
                                             left join m_code_value cvb on
                                                 cvb.id = bd.business_type_id
                                             left join m_loan_collateral_management lcm on
                                                 lcm.loan_id = l.id
                                             left join m_client_collateral_management_additional_details ccma on
                                                 ccma.client_collateral_id = lcm.client_collateral_id
                                             LEFT JOIN
                                                 (
                                                 SELECT
                                                     cdr.client_id,
                                                     MIN(addr.physical_address_sector) AS physical_address_sector,
                                                     MIN(addr.physical_address_district) AS physical_address_district,
                                                     MIN(addr.physical_address_cell) AS physical_address_cell,
                                                     MIN(addr.state_province_id) AS state_province_id
                                                 FROM
                                                     m_client_address cdr
                                                 JOIN
                                                      m_address addr ON
                                                     addr.id = cdr.address_id
                                                 GROUP BY
                                                     cdr.client_id
                                                  ) adr ON
                                                 c.id = adr.client_id
                                             left join m_client_non_person cid on
                                                 c.id = cid.client_id
                                             left join m_code_value cvp on
                                                 cvp.id = adr.state_province_id
                                             left join m_code_value loanPurposeTble on
                                                 loanPurposeTble.id = l.loanpurpose_cv_id
                                             left join (
                                                 select
                                                     *
                                                     from m_loan_transaction lt
                                                 where
                                                     lt.id in (
                                                     select
                                                         max(id)
                                                     from
                                                         m_loan_transaction t
                                                     where
                                                         t.transaction_type_enum = 2
                                                         and t.is_reversed = 0
                                                         and (date(t.transaction_date) between date('${startDate}') and date('${endDate}'))
                                                     group by
                                                         t.loan_id)) lastPaymentTbl on
                                                 lastPaymentTbl.loan_id = l.id
                                             left join (
                                                 select
                                                     *
                                                     from m_loan_repayment_schedule lt
                                                 where
                                                     lt.id in (
                                                     select
                                                         min(id)
                                                     from
                                                         m_loan_repayment_schedule rs
                                                     where
                                                         (date(rs.duedate) between date('${startDate}') and date('${endDate}'))
                                                     group by
                                                         rs.loan_id)) maxpaydateTbl on
                                                 maxpaydateTbl.loan_id = l.id
                                             left join (
                                                 select
                                                     t.loan_id,
                                                     sum(ifnull(t.principal_portion_derived, 0)) as princ_paid,
                                                     sum(ifnull(t.interest_portion_derived, 0)) as int_paid,
                                                     sum(ifnull(t.fee_charges_portion_derived, 0)) as fee_paid,
                                                     sum(ifnull(t.penalty_charges_portion_derived, 0)) as pen_paid
                                                 from
                                                     m_loan_transaction t
                                                 where
                                                     t.transaction_type_enum in (1, 2, 28)
                                                         and t.is_reversed = 0
                                                         and (date(t.transaction_date) between date('${startDate}') and date('${endDate}'))
                                                     group by
                                                         t.loan_id) paymentTbl on
                                                 paymentTbl.loan_id = l.id
                                             left join (
                                                 select
                                                     t.loan_id,
                                                     sum(ifnull(t.amount, 0))as Actual_paid
                                                 from
                                                     m_loan_transaction t
                                                 where
                                                     t.transaction_type_enum in (2)
                                                         and t.is_reversed = 0
                                                         and (date(t.transaction_date) between date('${startDate}') and date('${endDate}'))
                                                     group by
                                                         t.loan_id) paymentTbl1 on
                                                 paymentTbl1.loan_id = l.id
                                             left join (
                                                 select
                                                     t.loan_id,
                                                     sum(ifnull(amount_outstanding_derived, 0))Pen_outstanding ,
                                                     sum(ifnull(amount_paid_derived, 0))Pen_paid,
                                                     sum(ifnull(amount_waived_derived, 0))pen_waived
                                                 from
                                                     m_loan_charge t
                                                 where
                                                     (date(t.due_for_collection_as_of_date) between date('${startDate}') and date('${endDate}'))
                                                 group by
                                                     t.loan_id) penaltiesTbl on
                                                 penaltiesTbl.loan_id = l.id
                                             left join (
                                                 select
                                                     t.loan_id,
                                                     sum(ifnull(t.interest_portion_derived, 0)) as int_waived,
                                                     sum(ifnull(t.fee_charges_portion_derived, 0)) as fee_waived,
                                                     sum(ifnull(t.penalty_charges_portion_derived, 0)) as pen_waived
                                                 from
                                                     m_loan_transaction t
                                                 where
                                                     t.transaction_type_enum = 4
                                                     and t.is_reversed = 0
                                                     and (date(t.transaction_date) between date('${startDate}') and date('${endDate}'))
                                                 group by
                                                     t.loan_id) waiverTbl on
                                                 waiverTbl.loan_id = l.id
                                             left join m_loan_decision ld on
                                                 ld.loan_id = l.id
                                             left join c_configuration con on
                                                 con.name = 'Add-More-Stages-To-A-Loan-Life-Cycle'
                                             left join (
                                                 select
                                                     l.id as loanId,
                                                     case
                                                         when l.loan_status_id = 100 then 'Pending Approval'
                                                         when l.loan_status_id = 200 then 'Approval'
                                                         when l.loan_status_id = 300 then 'Active'
                                                         when l.loan_status_id = 303 then 'Transfer In Progress'
                                                         when l.loan_status_id = 304 then 'Transfer On Hold'
                                                         when l.loan_status_id = 400 then 'Withdrawn By Client'
                                                         when l.loan_status_id = 500 then 'Rejected'
                                                         when l.loan_status_id = 600 then 'Closed Obligations Met'
                                                         when l.loan_status_id = 601 then 'Closed Written Off'
                                                         when l.loan_status_id = 602 then 'Closed Reschedule Outstanding Amount'
                                                         when l.loan_status_id = 700 then 'Overpaid'
                                                     end as loanStatus,
                                                     case
                                                         when l.loan_decision_state = 1000 then 'Review Application'
                                                         when l.loan_decision_state = 1200 then 'Due Diligence'
                                                         when l.loan_decision_state = 1300 then 'Collateral Review'
                                                         when l.loan_decision_state = 1400 then 'IC Review Level One'
                                                         when l.loan_decision_state = 1500 then 'IC Review Level Two'
                                                         when l.loan_decision_state = 1600 then 'IC Review Level Three'
                                                         when l.loan_decision_state = 1700 then 'IC Review Level Four'
                                                         when l.loan_decision_state = 1800 then 'IC Review Level Five'
                                                         when l.loan_decision_state = 1900 then 'Prepare And Sign Contract'
                                                     end as loanDecisionState
                                                 from
                                                     m_loan l
                                                 left join m_loan_decision ld on
                                                     ld.loan_id = l.id) as loanStatusTable on
                                                 loanStatusTable.loanId = l.id
                                             left join (
                                                 select
                                                     count(*) as installemntsCount,
                                                     lrs.loan_id
                                                 from
                                                     m_loan_repayment_schedule lrs
                                                 where
                                                     lrs.completed_derived = false
                                                     and lrs.duedate < now()
                                                 group by
                                                     lrs.loan_id,
                                                     lrs.id) installmentArrears on
                                                 installmentArrears.loan_id = l.id
                                             left join (
                                                 select
                                                     sum(lrs.principal_amount) as expected_princ,
                                                     sum(lrs.interest_amount) as expected_int,
                                                     ifnull(sum(lrs.fee_charges_amount), 0) as expected_fees,
                                                     lrs.loan_id
                                                 from
                                                     m_loan_repayment_schedule lrs
                                                 where
                                                     (date(lrs.duedate) between date('${startDate}') and date('${endDate}'))
                                                 group by
                                                     lrs.loan_id) expectedrepaymentTbl on
                                                 expectedrepaymentTbl.loan_id = l.id
                                             left join (
                                                 select
                                                     count(*) as Instalment,
                                                     lrs.loan_id,
                                                     max(lrs.duedate) as max_date,
                                                     min(lrs.duedate) as min_date
                                                 from
                                                     m_loan_repayment_schedule lrs
                                                 where
                                                     (date(lrs.duedate) between date('${startDate}') and date('${endDate}'))
                                                         and ((lrs.completed_derived = false
                                                             AND lrs.obligations_met_on_date IS NULL)
                                                         OR lrs.obligations_met_on_date > date('${endDate}'))
                                                     group by
                                                         lrs.loan_id) repaymentInstalmentTbl on
                                                 repaymentInstalmentTbl.loan_id = l.id
                                             LEFT JOIN (
                                                 SELECT
                                                     lrs.duedate AS nextPaymentDueDate,
                                                     lrs.loan_id,
                                                     IFNULL(lrs.principal_amount, 0) AS scheduledPrincipalAmount,
                                                     IFNULL(lrs.interest_amount, 0) AS scheduledInterestAmount,
                                                     IFNULL(lrs.fee_charges_amount, 0) AS scheduledFeesAmount,
                                                     IFNULL(lrs.principal_amount, 0) + IFNULL(lrs.interest_amount, 0) AS scheduledPaymentAmount
                                                 FROM
                                                     (
                                                     SELECT
                                                         lrs.*,
                                                         ROW_NUMBER() OVER (PARTITION BY lrs.loan_id
                                                     ORDER BY
                                                         lrs.installment ASC) AS row_num
                                                     FROM
                                                         m_loan_repayment_schedule lrs
                                                     WHERE
                                                         lrs.completed_derived = false
                                                         AND lrs.obligations_met_on_date IS NULL
                                                         and lrs.duedate >= curdate()
                                                     GROUP BY
                                                         lrs.loan_id,
                                                         lrs.installment,
                                                         lrs.id
                                                     ORDER BY
                                                         lrs.installment ASC ) lrs
                                                 WHERE
                                                     lrs.row_num = 1 ) AS nextPaymentTbl on
                                                 nextPaymentTbl.loan_id = l.id
                                             left join (
                                                 SELECT
                                                     account_no,
                                                     disbursedon_date,
                                                     created_on_utc,
                                                     submittedon_date,
                                                     client_id,
                                                     id as loan_id,
                                                     CAST(ROW_NUMBER() OVER (PARTITION BY client_id ORDER BY submittedon_date) AS SIGNED)AS incremental_count
                                                 FROM
                                                     m_loan)loancountTbl on
                                                 loancountTbl.client_id = l.client_id
                                                 and loancountTbl.loan_id = l.id
                                             left join `Business Information` bi on
                                                 c.id = bi.client_id
                                             where
                                                 c.office_id = ${officeId}
                                                 and (l.product_id = '${loanProductId}'
                                                     or '-1' = '${loanProductId}')
                                                 and (ifnull(l.loan_officer_id, -10) = '${loanOfficerId}'
                                                     or '-1' = '${loanOfficerId}')
                                                 and (ifnull(l.fund_id, -10) = ${fundId}
                                                     or -1 = ${fundId})
                                                 and (ifnull(l.loanpurpose_cv_id, -10) = ${loanPurposeId}
                                                     or -1 = ${loanPurposeId})
                                                 and (l.currency_code = '${currencyId}'
                                                     or '-1' = '${currencyId}')
                                                 and (date(l.disbursedon_date) between date('${startDate}') and date('${endDate}'))    "
WHERE id = (SELECT tbl.id FROM (SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Portfolio Management") AS tbl);
