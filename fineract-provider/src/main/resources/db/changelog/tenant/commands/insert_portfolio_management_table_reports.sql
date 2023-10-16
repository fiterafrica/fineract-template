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

INSERT INTO stretchy_report (report_name, report_type, report_category, report_sql, description, core_report, use_report, self_service_user_report) 
VALUES ("Portfolio Management", "Table", "Loan", "select concat(u.firstname, ' ', u.lastname) as 'User', concat(c.firstname, ' ', c.lastname) as 'Client Name', c.external_id as 'Client UUID', l.account_no as 'Loan Number', loanPurposeTble.code_value as 'Purpose',
cvd.code_value as 'Department', cvs.code_value as  'Strata', '-' as 'KIVA Loan ID', c.id as 'Client ID', c.date_of_birth as 'Date of Birth', '-' as 'KIVA Client ID', l.loan_counter as 'Cycle', cvc.code_value as 'Cohort',
'-' as 'Is Startup', cvg.code_value as 'Gender', cvp.code_value as 'Province', cvb.code_value as 'Sector', cvn.code_value as 'Nationality', coi.telephone_no as 'Telephone', l.approvedon_date as 'Approval Date',
l.disbursedon_date as 'Disbursement Date', l.principal_amount_proposed as 'Applied Amount', l.approved_principal as 'Approved Amount', l.principal_disbursed_derived as 'Disbursed Amount',
(l.principal_disbursed_derived - l.principal_amount_proposed) as 'Difference', currency.name as 'Currency Type', l.number_of_repayments as 'Re-payment Term',
        case
            when l.loan_type_enum = 1 then 'Individual'
            when l.loan_type_enum = 2 then 'Group'
            when l.loan_type_enum = 3 then 'JLG'
            when l.loan_type_enum = 4 then 'GLIM'
            when l.loan_type_enum = 5 then 'GSIM'
        end as 'Loan Type',
l.term_frequency as 'Terms Duration', l.total_repayment_derived as 'Actual Payment Amount(KES)', l.principal_repaid_derived as 'Principal Paid(KES)', l.interest_repaid_derived as 'Interest Paid(KES)',
l.fee_charges_repaid_derived as 'Fees Paid(kes)', l.penalty_charges_repaid_derived as 'Total Late Fees Paid(KES)', l.total_overpaid_derived as 'Excess Amount Paid(KES)', l.total_outstanding_derived as 'Current Balance(KES)',
l.principal_outstanding_derived as 'Principal Balance(KES)', l.interest_outstanding_derived as 'Interest Balance(KES)', l.fee_charges_outstanding_derived as 'Fees Balance(KES)', laa.total_overdue_derived as 'Amount Past Due(KES)',
laa.principal_overdue_derived as 'Principal Past Due(KES)', laa.interest_overdue_derived as 'Interest Past Due(KES)', laa.fee_charges_overdue_derived as 'Fees Past Due(KES)',
nextPaymentTbl.scheduledPrincipalAmount as 'Scheduled Principal Amount(KES)', nextPaymentTbl.scheduledInterestAmount as 'Scheduled Interest Amount', nextPaymentTbl.scheduledFeesAmount as 'Scheduled Fees Amount(KES)',
lastPaymentTbl.amount as 'Last Payment Amount(KES)', lastPaymentTbl.principal_portion_derived as 'Last Principal Amount(KES)', lastPaymentTbl.interest_portion_derived as 'Last Interest Amount(KES)',
lastPaymentTbl.fee_charges_portion_derived as 'Last Fees Amount', lastPaymentTbl.fee_charges_portion_derived as 'Last Fees Amount(KES)', 0 as 'Last Late Fees Amount',
0 as 'Last Late Fees Amount(KES)', 0 as 'Last Excess Amount', 0 as 'Last Excess Amount(KES)',
datediff(now(), laa.overdue_since_date_derived) as 'Days in Arrears', installmentArrears.installemntsCount as 'Installment in Arrears',
lastPaymentTbl.transaction_date as 'Last Payment Date', nextPaymentTbl.nextPaymentDueDate as 'Next Payment Due', l.maturedon_date as 'Final Payment Date', l.closedon_date as 'Date Closed',
       case
           when ( con.enabled = false ) then loanStatusTable.loanStatus
           when con.enabled = true then
               case
                   when loanStatusTable.loanStatus = 'Pending Approval' and loanStatusTable.loanDecisionState is null then loanStatusTable.loanStatus
                   when loanStatusTable.loanStatus = 'Pending Approval' and loanStatusTable.loanDecisionState is not null and  loanStatusTable.loanDecisionState != 'Prepare And Sign Contract' then loanStatusTable.loanDecisionState
                   when loanStatusTable.loanStatus != 'Pending Approval' then loanStatusTable.loanStatus
                   when loanStatusTable.loanStatus = 'Pending Approval' and loanStatusTable.loanDecisionState = 'Prepare And Sign Contract' then loanStatusTable.loanStatus
              end
       end as 'Loan Status'
from m_office o
join m_office ounder ON ounder.hierarchy like concat(o.hierarchy, '%') AND ounder.hierarchy like CONCAT('${currentUserHierarchy}', '%')
join m_client c ON c.office_id = ounder.id
join m_loan l ON l.client_id = c.id
left join m_loan_arrears_aging laa on laa.loan_id = l.id
join m_product_loan p ON p.id=l.product_id
join m_appuser u on u.id = l.created_by
left join m_currency currency ON currency.code = p.currency_code
left join m_code_value cvg ON cvg.id = c.gender_cv_id
left join m_client_other_info coi ON coi.client_id = c.id
left join m_client_recruitment_survey crs on crs.client_id = c.id
left join m_code_value cvn ON cvn.id = coi.nationality_cv_id
left join m_code_value cvs ON cvs.id = coi.strata_cv_id
left join m_code_value cvd ON cvd.id = l.department_cv_id
left join m_fund f ON f.id = l.fund_id
left join m_code_value cvc on cvc.id = crs.cohort_cv_id
left join m_business_detail bd on bd.client_id = c.id
left join m_code_value cvb on cvb.id = bd.business_type_id
left join m_loan_collateral_management lcm on lcm.loan_id = l.id
left join m_client_collateral_management_additional_details ccma on ccma.client_collateral_id = lcm.client_collateral_id
left join m_code_value cvp on cvp.id = ccma.province_cv_id
left join m_code_value loanPurposeTble on loanPurposeTble.id = l.loanpurpose_cv_id
left join (select * from m_loan_transaction lt group by lt.loan_id, lt.submitted_on_date order by lt.submitted_on_date desc limit 1 )  lastPaymentTbl on lastPaymentTbl.loan_id = l.id
left join m_loan_decision ld on ld.loan_id = l.id
left join c_configuration con on con.name = 'Add-More-Stages-To-A-Loan-Life-Cycle'
left join (select l.id as loanId,
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
from m_loan l
left join m_loan_decision ld on ld.loan_id = l.id ) as loanStatusTable on loanStatusTable.loanId = l.id
left join (select count(*) as installemntsCount, lrs.loan_id from m_loan_repayment_schedule lrs
            where lrs.completed_derived = false and lrs.duedate < now() group by lrs.loan_id)  installmentArrears on installmentArrears.loan_id = l.id
left join (select min(datediff(now(), lrs.duedate)) as minDays, lrs.duedate as nextPaymentDueDate,
lrs.loan_id, (ifnull(lrs.principal_amount, 0) - ifnull(lrs.principal_completed_derived,0) - ifnull(lrs.principal_writtenoff_derived, 0))  as scheduledPrincipalAmount,
(ifnull(lrs.interest_amount, 0) - ifnull(lrs.interest_completed_derived, 0) - ifnull(lrs.interest_waived_derived, 0) - ifnull(lrs.interest_writtenoff_derived, 0)) as scheduledInterestAmount,
(ifnull(lrs.fee_charges_amount, 0) - ifnull(lrs.fee_charges_completed_derived, 0) - ifnull(lrs.fee_charges_waived_derived, 0) - ifnull(lrs.fee_charges_writtenoff_derived, 0)) as scheduledFeesAmount
from m_loan_repayment_schedule lrs where lrs.completed_derived = false group by loan_id) nextPaymentTbl  on nextPaymentTbl.loan_id = l.id
where o.id = ${officeId}
and (l.product_id = '${loanProductId}' or '-1' = '${loanProductId}')
and (ifnull(l.loan_officer_id, -10) = '${loanOfficerId}' or '-1' = '${loanOfficerId}')
and (ifnull(l.fund_id, -10) = ${fundId} or -1 = ${fundId})
and (ifnull(l.loanpurpose_cv_id, -10) = ${loanPurposeId} or -1 = ${loanPurposeId})
and (date(l.submittedon_date) between date('${startDate}') and date('${endDate}')) ", "Portfolio Management Report", true, true, false);