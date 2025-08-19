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
package org.apache.fineract.infrastructure.sms.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.util.Collection;

@Getter
public class SmsProviderBatchRequest {
    private long providerId;
    private Collection<SmsMessageApiQueueResourceData> messages;


    /**
     * SmsProviderBatchRequest constructor
     **/
    protected SmsProviderBatchRequest() {
    }


    public SmsProviderBatchRequest(long providerId, Collection<SmsMessageApiQueueResourceData> messages) {
        this.providerId = providerId;
        this.messages = messages;
    }

    public static String toJsonString(SmsProviderBatchRequest request) {
        try {
            return new ObjectMapper().writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing provider batch request", e);
        }
    }

    // getters and setters
}
