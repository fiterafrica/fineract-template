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

package org.apache.fineract.infrastructure.transactions.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.transactions.service.TransactionStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/transactions/status")
@Component
@Scope("singleton")
@Tag(name = "Savings Account Transactions Status", description = "")
public class TransactionStatusApiResource {

    private final TransactionStatusService transactionStatusService;
    private final DefaultToApiJsonSerializer<StatusResponse> toApiJsonSerializer;

    @Autowired
    public TransactionStatusApiResource(TransactionStatusService transactionStatusService,
            DefaultToApiJsonSerializer<StatusResponse> toApiJsonSerializer) {
        this.transactionStatusService = transactionStatusService;
        this.toApiJsonSerializer = toApiJsonSerializer;
    }

    @GET
    @Path("{correlationId}")
    public String getStatus(@PathParam("correlationId") String correlationId) {
        StatusResponse response = new StatusResponse(this.transactionStatusService.isComplete(correlationId));
        return toApiJsonSerializer.serialize(response);
    }

    private static class StatusResponse {

        private boolean complete;

        public StatusResponse(boolean complete) {
            this.complete = complete;
        }
    }
}
