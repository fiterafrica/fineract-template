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
package org.apache.fineract.commands.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.springframework.stereotype.Component;

@Component
public class CommandProcessingResultSerializer extends JsonSerializer<CommandProcessingResult> {

    @Override
    public void serialize(CommandProcessingResult value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("commandId", value.commandId());
        gen.writeObjectField("officeId", value.getOfficeId());
        gen.writeObjectField("groupId", value.getGroupId());
        gen.writeObjectField("clientId", value.getClientId());
        gen.writeObjectField("loanId", value.getLoanId());
        gen.writeObjectField("savingsId", value.getSavingsId());
        gen.writeObjectField("resourceId", value.resourceId());
        gen.writeObjectField("subResourceId", value.getSubResourceId());
        gen.writeObjectField("transactionId", value.getTransactionId());
        gen.writeObjectField("changes", value.getChanges());
        gen.writeObjectField("creditBureauReportData", value.getCreditReport());
        gen.writeObjectField("resourceIdentifier", value.getResourceIdentifier());
        gen.writeObjectField("productId", value.getProductId());
        gen.writeObjectField("gsimId", value.getGsimId());
        gen.writeObjectField("glimId", value.getGlimId());
        gen.writeObjectField("rollbackTransaction", value.isRollbackTransaction());
        gen.writeEndObject();
    }
}
