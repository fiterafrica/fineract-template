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
package org.apache.fineract.portfolio.loanaccount.data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data class representing the result of a bulk foreclosure retry operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkForeclosureRetryResultData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private String status; // PENDING, RUNNING, COMPLETED
    private Integer totalRetried;
    private Integer successful;
    private Integer failed;
    private LocalDateTime retriedOn;
    private Long retriedByUserId;
    private String retriedByUserName;
    private List<RetryDetailResult> results = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryDetailResult implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long detailId;
        private Long loanId;
        private String loanAccountNo;
        private String clientName;
        private String previousStatus;
        private String newStatus;
        private String failureReason;
        private Integer retryCount;
    }

    public static BulkForeclosureRetryResultData pending(String jobId, int totalRetried, Long userId) {
        BulkForeclosureRetryResultData data = new BulkForeclosureRetryResultData();
        data.setJobId(jobId);
        data.setStatus("PENDING");
        data.setTotalRetried(totalRetried);
        data.setSuccessful(0);
        data.setFailed(0);
        data.setRetriedByUserId(userId);
        data.setResults(new ArrayList<>());
        return data;
    }
}
