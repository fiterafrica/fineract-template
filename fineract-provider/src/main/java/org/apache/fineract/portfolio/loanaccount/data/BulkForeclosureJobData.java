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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkForeclosureJobData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private String status; // PENDING, RUNNING, COMPLETED, FAILED
    private Integer total;
    private Integer successful;
    private Integer failed;
    private LocalDateTime createdOn;
    private LocalDateTime completedOn;
    private Long submittedByUserId;
    private String submittedByUserName;
    private java.time.LocalDate foreclosureDate;
    private List<BulkForeclosureSuccessData> successes = new ArrayList<>();
    private List<BulkForeclosureFailureData> failures = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkForeclosureSuccessData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String loanId;
        private String loanAccountNo;
        private String clientName;
        private LocalDateTime processedOn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkForeclosureFailureData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String loanId;
        private String loanAccountNo;
        private String clientName;
        private String reason;
    }

    public static BulkForeclosureJobData pending(String jobId, int total) {
        BulkForeclosureJobData data = new BulkForeclosureJobData();
        data.setJobId(jobId);
        data.setStatus("PENDING");
        data.setTotal(total);
        data.setSuccessful(0);
        data.setFailed(0);
        data.setSuccesses(new ArrayList<>());
        data.setFailures(new ArrayList<>());
        return data;
    }
}
