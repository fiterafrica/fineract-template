/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.transactions.service;

import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionStatusService {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public TransactionStatusService(RoutingDataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * If correlationId is not in the table then we assume the transaction completed.
     */
    public boolean isComplete(String correlationId) {
        return jdbcTemplate.queryForList("SELECT id FROM c_correlation WHERE id = ?", String.class, correlationId).isEmpty();
    }

    /**
     * Log this correlationId so that transaction check API is able to tell the status of the transaction
     */
    public void logCorrelationId(String correlationId) {
        jdbcTemplate.update("REPLACE INTO c_correlation VALUES(?)", correlationId);
    }

    /**
     * End transaction processing by removing correlation id from log so transaction check API won't find it
     */
    public void endProcessing(String correlationId) {
        jdbcTemplate.update("DELETE FROM c_correlation WHERE id = ?", correlationId);
    }
}
