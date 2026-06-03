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
package org.apache.fineract.portfolio.loanaccount.service;

import java.time.LocalDate;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.portfolio.loanaccount.data.BulkForeclosureJobData;
import org.apache.fineract.portfolio.loanaccount.data.BulkForeclosureRetryResultData;

public interface LoanBulkForeclosureService {

    BulkForeclosureJobData triggerBulkForeclosure(List<Long> loanIds, LocalDate foreclosureDate, String executionMode);

    BulkForeclosureJobData getJobStatus(String jobId);

    Page<BulkForeclosureJobData> getJobList(Integer offset, Integer limit);

    void forecloseSingleLoan(Long loanId, LocalDate foreclosureDate);

    Response downloadJobReport(String jobId, String reportType);

    /**
     * Retry failed foreclosure records for a given job.
     *
     * @param jobId
     *            the job ID
     * @param detailIds
     *            list of detail record IDs to retry (failed records)
     * @param executionMode
     *            "SYNC" or "ASYNC"
     * @return retry result data
     */
    BulkForeclosureRetryResultData retryFailedRecords(String jobId, List<Long> detailIds, String executionMode);
}
