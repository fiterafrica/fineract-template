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
package org.apache.fineract.portfolio.loanaccount.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkForeclosureJobDetailRepository extends JpaRepository<BulkForeclosureJobDetail, Long> {

    List<BulkForeclosureJobDetail> findByJob(BulkForeclosureJob job);

    boolean existsByJob_IdAndLoanId(Long jobId, Long loanId);

    /**
     * Find failed records by detail IDs and job.
     *
     * @param detailIds
     *            list of detail IDs
     * @param jobId
     *            the job primary key ID
     * @return list of failed details
     */
    @Query("SELECT d FROM BulkForeclosureJobDetail d WHERE d.loanId IN :detailIds AND d.job.id = :jobId AND d.status = 'FAILED'")
    List<BulkForeclosureJobDetail> findFailedByIdsAndJob(@Param("detailIds") List<Long> detailIds, @Param("jobId") Long jobId);

    /**
     * Find all failed records for a job.
     *
     * @param jobId
     *            the job primary key ID
     * @return list of failed details
     */
    @Query("SELECT d FROM BulkForeclosureJobDetail d WHERE d.job.id = :jobId AND d.status = 'FAILED'")
    List<BulkForeclosureJobDetail> findAllFailedByJob(@Param("jobId") Long jobId);

    /**
     * Count failed records for a job.
     *
     * @param jobId
     *            the job primary key ID
     * @return count of failed records
     */
    @Query("SELECT COUNT(d) FROM BulkForeclosureJobDetail d WHERE d.job.id = :jobId AND d.status = 'FAILED'")
    long countFailedByJob(@Param("jobId") Long jobId);

    /**
     * Count successful records for a job.
     *
     * @param jobId
     *            the job primary key ID
     * @return count of successful records
     */
    @Query("SELECT COUNT(d) FROM BulkForeclosureJobDetail d WHERE d.job.id = :jobId AND d.status = 'SUCCESS'")
    long countSuccessfulByJob(@Param("jobId") Long jobId);
}
