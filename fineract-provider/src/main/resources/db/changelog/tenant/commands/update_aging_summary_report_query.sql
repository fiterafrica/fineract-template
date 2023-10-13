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

UPDATE stretchy_report SET report_sql = "SELECT
  IFNULL(periods.currencyName, periods.currency) as currency,
  periods.period_no 'Weeks In Arrears (Up To)',
  IFNULL(ars.loanId, 0) 'No Of Loans',
  IFNULL(ars.principal,0.0) 'Original Principal',
  IFNULL(ars.interest,0.0) 'Original Interest',
  IFNULL(ars.prinPaid,0.0) 'Principal Paid',
  IFNULL(ars.intPaid,0.0) 'Interest Paid',
  IFNULL(ars.prinOverdue,0.0) 'Principal Overdue',
  IFNULL(ars.intOverdue,0.0) 'Interest Overdue',
ars.age as 'Age',
ars.clientUID as 'Client UID',
ars.gender as 'Gender',
ars.cohort as 'Cohort',
ars.cycle as 'Cycle',
ars.nationality as 'Nationality',
ars.location as 'Location',
ars.strata as 'Strata'

FROM
	
  (SELECT curs.code as currency, curs.name as currencyName, pers.* from
	(SELECT 'On Schedule' period_no,1 pid UNION
		SELECT '1',2 UNION
		SELECT '2',3 UNION
		SELECT '3',4 UNION
		SELECT '4',5 UNION
		SELECT '5',6 UNION
		SELECT '6',7 UNION
		SELECT '7',8 UNION
		SELECT '8',9 UNION
		SELECT '9',10 UNION
		SELECT '10',11 UNION
		SELECT '11',12 UNION
		SELECT '12',13 UNION
		SELECT '12+',14) pers,
	(SELECT distinctrow moc.code, moc.name
  	FROM m_office mo2
   	INNER JOIN m_office ounder2 ON ounder2.hierarchy
				LIKE CONCAT(mo2.hierarchy, '%')
AND ounder2.hierarchy like CONCAT('${currentUserHierarchy}', '%')
   	INNER JOIN m_client mc2 ON mc2.office_id=ounder2.id
   	INNER JOIN m_loan ml2 ON ml2.client_id = mc2.id
	INNER JOIN m_organisation_currency moc ON moc.code = ml2.currency_code
	WHERE ml2.loan_status_id=300 
	AND mo2.id=${officeId}
AND (ml2.currency_code = '${currencyId}' or '-1' = '${currencyId}')) curs) periods


LEFT JOIN 
(SELECT
  	z.currency, z.arrPeriod,
	COUNT(z.loanId) as loanId, SUM(z.principal) as principal, SUM(z.interest) as interest,
	SUM(z.prinPaid) as prinPaid, SUM(z.intPaid) as intPaid,
	SUM(z.prinOverdue) as prinOverdue, SUM(z.intOverdue) as intOverdue,
                z.age,
                z.clientUID,
                z.gender,
                z.cohort,
                z.cycle,
                z.nationality,
                z.location,
                z.strata
FROM
	
	(SELECT x.loanId, x.currency, x.principal, x.interest, x.prinPaid, x.intPaid, x.prinOverdue, x.intOverdue,
                x.age,
                x.clientUID,
                x.gender,
                x.cohort,
                x.cycle,
                x.nationality,
                x.location,
                x.strata,
		IF(DATEDIFF(CURDATE(), minOverdueDate)<1, 'On Schedule',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<8, '1',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<15, '2',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<22, '3',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<29, '4',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<36, '5',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<43, '6',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<50, '7',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<57, '8',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<64, '9',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<71, '10',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<78, '11',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<85, '12',
				 '12+'))))))))))))) AS arrPeriod

	FROM 
		(SELECT ml.id AS loanId, ml.currency_code as currency,
   			ml.principal_disbursed_derived as principal,
			   ml.interest_charged_derived as interest,
   			ml.principal_repaid_derived as prinPaid,
			   ml.interest_repaid_derived intPaid,

			   laa.principal_overdue_derived as prinOverdue,
			   laa.interest_overdue_derived as intOverdue,

			   IFNULL(laa.overdue_since_date_derived, curdate()) as minOverdueDate,
                year(now()) - YEAR(mc.date_of_birth) as age,
                mc.external_id as clientUID,
                cdg.code_value as gender,
                cvc.code_value as cohort,
                ml.loan_counter cycle,
                cvn.code_value nationality,
                cvl.code_value location,
                cvs.code_value as strata

  		FROM m_office mo
   		INNER JOIN m_office ounder ON ounder.hierarchy
				LIKE CONCAT(mo.hierarchy, '%')
AND ounder.hierarchy like CONCAT('${currentUserHierarchy}', '%')
   		INNER JOIN m_client mc ON mc.office_id=ounder.id
   		INNER JOIN m_loan ml ON ml.client_id = mc.id
		   LEFT JOIN m_loan_arrears_aging laa on laa.loan_id = ml.id
  		left join m_client_other_info coi on coi.client_id = mc.id
        left join m_code_value cdg on cdg.id = mc.gender_cv_id
        left join m_client_recruitment_survey crs on crs.client_id = mc.id
        left join m_code_value cvc on cvc.id = crs.cohort_cv_id
        left join m_code_value cvn on cvn.id = coi.nationality_cv_id
        left join m_code_value cvl on cvl.id = crs.survey_location_cv_id
        left join m_code_value cvs on cvs.id = coi.strata_cv_id
		WHERE ml.loan_status_id=300 
     		AND mo.id=${officeId}
     AND (ml.currency_code = '${currencyId}' or '-1' = '${currencyId}')
  		GROUP BY ml.id, cvc.code_value, cvn.code_value, cvl.code_value, cvs.code_value) x
	) z
GROUP BY z.currency, z.arrPeriod, z.age, z.clientUID, z.gender, z.cohort, z.cycle, z.nationality, z.location, z.strata ) ars ON ars.arrPeriod=periods.period_no and ars.currency = periods.currency
ORDER BY periods.currency, periods.pid " WHERE id = (SELECT tbl.id FROM (SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Aging Summary (Arrears in Weeks)") AS tbl);


UPDATE stretchy_report SET report_sql = "SELECT
  IFNULL(periods.currencyName, periods.currency) as currency,
  periods.period_no 'Days In Arrears',
  IFNULL(ars.loanId, 0) 'No Of Loans',
  IFNULL(ars.principal,0.0) 'Original Principal',
  IFNULL(ars.interest,0.0) 'Original Interest',
  IFNULL(ars.prinPaid,0.0) 'Principal Paid',
  IFNULL(ars.intPaid,0.0) 'Interest Paid',
  IFNULL(ars.prinOverdue,0.0) 'Principal Overdue',
  IFNULL(ars.intOverdue,0.0)'Interest Overdue',
ars.age as 'Age',
ars.clientUID as 'Client UID',
ars.gender as 'Gender',
ars.cohort as 'Cohort',
ars.cycle as 'Cycle',
ars.nationality as 'Nationality',
ars.location as 'Location',
ars.strata as 'Strata'
FROM

  (SELECT curs.code as currency, curs.name as currencyName, pers.* from
	(SELECT 'On Schedule' period_no,1 pid UNION
		SELECT '0 - 30',2 UNION
		SELECT '30 - 60',3 UNION
		SELECT '60 - 90',4 UNION
		SELECT '90 - 180',5 UNION
		SELECT '180 - 360',6 UNION
		SELECT '> 360',7 ) pers,
	(SELECT distinctrow moc.code, moc.name
  	FROM m_office mo2
   	INNER JOIN m_office ounder2 ON ounder2.hierarchy
				LIKE CONCAT(mo2.hierarchy, '%')
AND ounder2.hierarchy like CONCAT('${currentUserHierarchy}', '%')
   	INNER JOIN m_client mc2 ON mc2.office_id=ounder2.id
   	INNER JOIN m_loan ml2 ON ml2.client_id = mc2.id
	INNER JOIN m_organisation_currency moc ON moc.code = ml2.currency_code
	WHERE ml2.loan_status_id=300
	AND mo2.id=${officeId}
AND (ml2.currency_code = '${currencyId}' or '-1' = '${currencyId}')) curs) periods


LEFT JOIN
(SELECT
  	z.currency, z.arrPeriod,
	COUNT(z.loanId) as loanId, SUM(z.principal) as principal, SUM(z.interest) as interest,
	SUM(z.prinPaid) as prinPaid, SUM(z.intPaid) as intPaid,
	SUM(z.prinOverdue) as prinOverdue, SUM(z.intOverdue) as intOverdue,
    z.age,z.clientUID, z.gender,z.cohort,z.cycle,z.nationality,z.location,z.strata
FROM

	(SELECT x.loanId, x.currency, x.principal, x.interest, x.prinPaid, x.intPaid, x.prinOverdue, x.intOverdue,
                x.age, x.clientUID, x.gender, x.cohort, x.cycle, x.nationality, x.location, x.strata,
		IF(DATEDIFF(CURDATE(), minOverdueDate)<1, 'On Schedule',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<31, '0 - 30',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<61, '30 - 60',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<91, '60 - 90',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<181, '90 - 180',
		IF(DATEDIFF(CURDATE(), minOverdueDate)<361, '180 - 360',
				 '> 360')))))) AS arrPeriod

	FROM
		(SELECT ml.id AS loanId, ml.currency_code as currency,
   			ml.principal_disbursed_derived as principal,
			   ml.interest_charged_derived as interest,
   			ml.principal_repaid_derived as prinPaid,
			   ml.interest_repaid_derived intPaid,

			   laa.principal_overdue_derived as prinOverdue,
			   laa.interest_overdue_derived as intOverdue,

			   IFNULL(laa.overdue_since_date_derived, curdate()) as minOverdueDate,
                year(now()) - YEAR(mc.date_of_birth) as age,
                mc.external_id as clientUID,
                cdg.code_value as gender,
                cvc.code_value as cohort,
                ml.loan_counter cycle,
                cvn.code_value nationality,
                cvl.code_value location,
                cvs.code_value as strata

  		FROM m_office mo
   		INNER JOIN m_office ounder ON ounder.hierarchy
				LIKE CONCAT(mo.hierarchy, '%')
AND ounder.hierarchy like CONCAT('${currentUserHierarchy}', '%')
   		INNER JOIN m_client mc ON mc.office_id=ounder.id
   		INNER JOIN m_loan ml ON ml.client_id = mc.id
		   LEFT JOIN m_loan_arrears_aging laa on laa.loan_id = ml.id
  		left join m_client_other_info coi on coi.client_id = mc.id
        left join m_code_value cdg on cdg.id = mc.gender_cv_id
        left join m_client_recruitment_survey crs on crs.client_id = mc.id
        left join m_code_value cvc on cvc.id = crs.cohort_cv_id
        left join m_code_value cvn on cvn.id = coi.nationality_cv_id
        left join m_code_value cvl on cvl.id = crs.survey_location_cv_id
        left join m_code_value cvs on cvs.id = coi.strata_cv_id
		WHERE ml.loan_status_id=300
     		AND mo.id=${officeId}
     AND (ml.currency_code = '${currencyId}' or '-1' = '${currencyId}')
  		GROUP BY ml.id, cvc.code_value, cvn.code_value, cvl.code_value, cvs.code_value) x
	) z
GROUP BY z.currency, z.arrPeriod, z.age, z.clientUID, z.gender, z.cohort, z.cycle, z.nationality, z.location, z.strata ) ars ON ars.arrPeriod=periods.period_no and ars.currency = periods.currency
ORDER BY periods.currency, periods.pid " WHERE id = (SELECT tbl.id FROM (SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Aging Summary (Arrears in Months)") AS tbl);

