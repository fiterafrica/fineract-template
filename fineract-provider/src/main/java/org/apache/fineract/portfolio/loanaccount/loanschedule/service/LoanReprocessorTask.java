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

package org.apache.fineract.portfolio.loanaccount.loanschedule.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionReprocess;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionReprocessRepository;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanaccount.service.LoanUtilService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class LoanReprocessorTask implements Callable<String> {

    private static final Logger LOG = LoggerFactory.getLogger(LoanReprocessorTask.class);
    final HashMap<BusinessDateType, LocalDate> businessDates;
    final FineractPlatformTenant tenant;
    Pair<Long, Long> loanPair;
    final LoanRepository loanRepository2;
    final LoanUtilService loanUtilService2;
    final LoanTransactionReprocessRepository loanTransactionReprocessRepository;

    final LoanAssembler loanAssembler;

    LoanReprocessorTask(Pair<Long, Long> incoming, final LoanRepository loanRepository, final LoanUtilService loanUtilService,
            final LoanTransactionReprocessRepository loanTransactionReprocessRepository, final LoanAssembler loanAssembler) {
        this.loanRepository2 = loanRepository;
        this.loanUtilService2 = loanUtilService;
        loanPair = incoming;
        businessDates = ThreadLocalContextUtil.getBusinessDates();
        tenant = ThreadLocalContextUtil.getTenant();
        this.loanAssembler = loanAssembler;
        this.loanTransactionReprocessRepository = loanTransactionReprocessRepository;
    }

    @Override
    public String call() {
        ThreadLocalContextUtil.setBusinessDates(businessDates);
        ThreadLocalContextUtil.setTenant(tenant);
        final Long startTime = System.currentTimeMillis();
        final Long loanId = loanPair.getRight();
        final Long reprocessId = loanPair.getLeft();

        LoanTransactionReprocess reprocess = loanTransactionReprocessRepository.findById(reprocessId).get();
        String exceptionString = null;
        try {
            handleLoan(loanId);

        } catch (Exception e) {
            LOG.error("Error occured while reprocessing loan with id " + reprocessId + " and exception is " + e.getMessage());
            exceptionString = e.getMessage();
        } finally {
            final Long endTime = System.currentTimeMillis();
            final Long duration = endTime - startTime;
            reprocess.setExceptionMessage(exceptionString);
            reprocess.setProcessDuration(duration);
            reprocess.setProcessed(true);
            reprocess.setProcessedOnDate(DateUtils.getLocalDateTimeOfTenant());
            this.loanTransactionReprocessRepository.save(reprocess);
        }

        return "Successfull reprocessing of loan with id " + reprocessId + " and loan id " + loanId + " and exception is "
                + exceptionString;

    }

    @Transactional
    private void handleLoan(Long loanId) {
        final Loan loan = loanAssembler.assembleFrom(loanId);
        loan.restoreLoanScheduleAndTransactions();
        this.loanRepository2.saveAndFlush(loan);
    }
}
