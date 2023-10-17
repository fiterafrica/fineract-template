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
VALUES ("IC Minutes", "Table", "Loan", "select c.external_id as 'Client UID', concat(c.firstname,c.lastname) as 'Client/Company Name', l.loan_counter as 'Cycle', cvg.code_value as 'Gender', cvs.code_value as  'Strata',
cvb.code_value as 'Business Sector', loanPurposeTble.code_value as 'Purpose of the loan', l.principal_amount_proposed as 'Applied Amount', icReviewTbl.IC_Level_One as 'IC Decision Level One',
icReviewTbl.IC_Level_Two as 'IC Decision Level Two', icReviewTbl.IC_Level_Three as 'IC Decision Level Three',icReviewTbl.IC_Level_Four as 'IC Decision Level Four',
icReviewTbl.IC_Level_Five as 'IC Decision Level Five', l.approved_principal as 'IC Approved Amount', 0 as 'Loan Tenure in months', '' as 'IC Approved Amount in Words', l.principal_disbursed_derived as 'Installment Amount', 
l.penalty_charges_charged_derived as 'Late Fees',
coi.tax_identification_number as 'Company Tin Number', cvn.code_value as 'Nationality', villageTbl.code_value as 'Village', cellTbl.code_value as 'Cell', cvb.code_value as 'Sector', districtTbl.code_value as 'District',
coi.telephone_no as 'Contact Number', coi.co_signors as 'Cosigner Number', l.account_no as 'Loan ID Number', '' as 'Bank Account Number', '' as 'Bank Name', ccma.UPI_NO as 'UPI Number', cvp.code_value as 'Province',
ccma.collateral_owner_first as 'Owners Name 1', ccma.collateral_owner_second as 'Owners Name 2', ccma.id_no_of_collateral_owner_first as 'Owners ID 1', ccma.id_no_of_collateral_owner_second as 'Owners ID 2',
'' as 'OMV Figures', '' as 'OMV in letters', coi.guarantor as 'Witness Name'
from m_office o
join m_office ounder ON ounder.hierarchy like concat(o.hierarchy, '%') AND ounder.hierarchy like CONCAT('${currentUserHierarchy}', '%')
join m_client c ON c.office_id = ounder.id
join m_loan l ON l.client_id = c.id
left join m_loan_arrears_aging laa on laa.loan_id = l.id
join m_product_loan p ON p.id=l.product_id
left join m_code_value cvg ON cvg.id = c.gender_cv_id
left join m_client_other_info coi ON coi.client_id = c.id
left join m_client_recruitment_survey crs on crs.client_id = c.id
left join m_code_value cvn ON cvn.id = coi.nationality_cv_id
left join m_code_value cvs ON cvs.id = coi.strata_cv_id
left join m_business_detail bd on bd.client_id = c.id
left join m_code_value cvb on cvb.id = bd.business_type_id
left join m_loan_collateral_management lcm on lcm.loan_id = l.id
left join m_client_collateral_management_additional_details ccma on ccma.client_collateral_id = lcm.client_collateral_id
left join m_code_value cvp on cvp.id = ccma.province_cv_id
left join m_code_value loanPurposeTble on loanPurposeTble.id = l.loanpurpose_cv_id
left join m_code_value villageTbl on villageTbl.id = ccma.village_cv_id
left join m_code_value districtTbl on districtTbl.id = ccma.district_cv_id
left join m_code_value cellTbl on cellTbl.id = ccma.cell_cv_id
left join (select ld.loan_id,
        case
        when ld.loan_decision_state = 1000 then 'Review Application'
        when ld.loan_decision_state = 1200 then 'Due Diligence'
        when ld.loan_decision_state = 1300 then 'Collateral Review'
        when ld.loan_decision_state = 1400 then 'IC Review Level One'
        when ld.loan_decision_state = 1500 then 'IC Review Level Two'
        when ld.loan_decision_state = 1600 then 'IC Review Level Three'
        when ld.loan_decision_state = 1700 then 'IC Review Level Four'
        when ld.loan_decision_state = 1800 then 'IC Review Level Five'
        when ld.loan_decision_state = 1900 then 'Prepare And Sign Contract'
    end as loanDecisionState,
    ld.next_loan_ic_review_decision_state as 'Next IC Level',
    if(ld.is_ic_review_decision_level_one_signed = true, 'Approved', if(ld.is_reject_ic_review_decision_level_one = true, 'Rejected', '')) as 'IC_Level_One',
    if(ld.is_ic_review_decision_level_two_signed = true, 'Approved', if(ld.is_reject_ic_review_decision_level_two = true, 'Rejected', '')) as 'IC_Level_Two',
    if(ld.is_ic_review_decision_level_three_signed = true, 'Approved', if(ld.is_reject_ic_review_decision_level_three = true, 'Rejected', '')) as 'IC_Level_Three',
    if(ld.is_ic_review_decision_level_four_signed = true, 'Approved', if(ld.is_reject_ic_review_decision_level_four = true, 'Rejected', '')) as 'IC_Level_Four',
    if(ld.is_ic_review_decision_level_five_signed = true, 'Approved', if(ld.is_reject_ic_review_decision_level_five = true, 'Rejected', '')) as 'IC_Level_Five'
from m_loan_decision ld ) as icReviewTbl on icReviewTbl.loan_id = l.id
where o.id = ${officeId}
and (l.product_id = '${loanProductId}' or '-1' = '${loanProductId}')
and (ifnull(l.loan_officer_id, -10) = '${loanOfficerId}' or '-1' = '${loanOfficerId}')
and (ifnull(l.fund_id, -10) = ${fundId} or -1 = ${fundId})
and (ifnull(l.loanpurpose_cv_id, -10) = ${loanPurposeId} or -1 = ${loanPurposeId})
and (date(l.submittedon_date) between date('${startDate}') and date('${endDate}')) ", "IC Minutes Report", true, true, false);