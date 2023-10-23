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

UPDATE stretchy_report SET report_sql = "select l.disbursedon_date, o.name as 'Office', departmentTbl.code_value as 'Department', cvn.code_value as 'Country', cvl.code_value as 'Location',
       concat(c.firstname, ' ', c.lastname) as 'Member Name', g.external_id as 'Group UID', l.account_no as 'Loan Account Number',
       l.principal_disbursed_derived as 'Loan Amount', l.interest_charged_derived as 'Interest', l.total_repayment_derived as 'Total Payments',
       l.principal_repaid_derived as 'Total Principal Paid', l.interest_repaid_derived as 'Total Interest Paid',
       l.fee_charges_repaid_derived as 'Total Fees Paid', l.penalty_charges_repaid_derived as 'Total Late Fees Paid',
       l.principal_outstanding_derived as 'Outstanding Principal', l.interest_outstanding_derived as 'Outstanding Interest',
       l.fee_charges_outstanding_derived as 'Outstanding Fees', l.penalty_charges_outstanding_derived as 'Outstanding Late Fees',
       p.name as 'Product', f.name as 'Fund'
from m_group topgroup
join m_office o on o.id = topgroup.office_id and o.hierarchy like concat('${currentUserHierarchy}', '%')
join m_group g on g.hierarchy like concat(topgroup.hierarchy, '%')
join m_loan l on l.group_id = g.id
join m_product_loan p on p.id = l.product_id
left join m_group_client gc on gc.group_id = l.group_id
left join m_client c on c.id = gc.client_id
left join m_client_other_info coi on coi.client_id = c.id
left join m_client_recruitment_survey crs on crs.client_id = c.id
left join m_currency cur on cur.code = l.currency_code
left join m_fund f on f.id = l.fund_id
left join m_code_value departmentTbl on departmentTbl.id = l.department_cv_id
left join m_code_value cvn on cvn.id = coi.nationality_cv_id
left join m_code_value cvl on cvl.id = crs.survey_location_cv_id
where o.id = ${officeId}
and (l.product_id = '${loanProductId}' or '-1' = '${loanProductId}')
and (l.department_cv_id = '${departmentId}' or '-1' = '${departmentId}')
and (l.loan_officer_id = '${loanOfficerId}' or '-1' = '${loanOfficerId}')
and (l.fund_id = '${fundId}' or '-1' = '${fundId}')
and (l.disbursedon_date >= '${startDate}' and l.disbursedon_date <= '${endDate}') " WHERE id = (SELECT tbl.id FROM (SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Group Loans") AS tbl);
