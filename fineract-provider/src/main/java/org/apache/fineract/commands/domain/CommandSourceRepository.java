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
package org.apache.fineract.commands.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommandSourceRepository extends JpaRepository<CommandSource, Long>, JpaSpecificationExecutor<CommandSource> {

    /**
     * Check if a pending savings transaction command exists with the given refNo. Used to prevent duplicate
     * transactions during bulk upload when maker-checker is enabled.
     *
     * @param refNo
     *            the transaction reference number to search for in the command JSON
     * @return true if a pending command with this reference exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(cs) > 0 THEN true ELSE false END FROM CommandSource cs " + "WHERE cs.processingResult = 2 " + // 2
                                                                                                                                 // =
                                                                                                                                 // AWAITING_APPROVAL
            "AND cs.entityName = 'SAVINGSACCOUNT' " + "AND (cs.actionName = 'DEPOSIT' OR cs.actionName = 'WITHDRAWAL') "
            + "AND cs.commandAsJson LIKE CONCAT('%\"refNo\":\"', :refNo, '\"%')")
    boolean existsPendingSavingsTransactionByRefNo(@Param("refNo") String refNo);
}
