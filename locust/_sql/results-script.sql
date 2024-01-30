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

# DELETE from acc_gl_journal_entry WHERE 1 = 1;
# DELETE FROM m_savings_account_transaction WHERE 1 = 1;
# DELETE FROM m_note WHERE savings_account_id IS NOT NULL;

SET @stop_time := 5;

SELECT
    @minDate := min(created_date) AS min_date,
    @maxDate := max(created_date) AS max_date
FROM m_savings_account_transaction;

SELECT @_5mlater := date_add(@minDate, INTERVAL @stop_time MINUTE);

SELECT
    @minDate,
    @_5mlater;


SELECT @txUnder5mins := count(*) AS txUnder5mins
FROM m_savings_account_transaction sac
WHERE sac.created_date <= @`_5mlater`;


SELECT @txTotal := count(*) AS txOver5mins
FROM m_savings_account_transaction sac;


SELECT
    @txUnder5mins,
    @txTotal,
    @minDate,
    @maxDate,
    timediff(@maxDate, @minDate)                         AS totalTime,
    @txTotal / time_to_sec(timediff(@maxDate, @minDate)) AS rps;

# SELECT count(* ) as jounals from acc_gl_journal_entry;
# SELECT count(* ) as transactions from m_savings_account_transaction;
