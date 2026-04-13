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
package org.apache.fineract.portfolio.savings.exception;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformDomainRuleException;

public class PostInterestAsOnDateException extends AbstractPlatformDomainRuleException {

    public enum PostInterestAsOnExceptionType {

        FUTURE_DATE, VALID_DATE, ACTIVATION_DATE, LAST_TRANSACTION_DATE, INTEREST_POSTING_NOT_ALLOWED_AFTER_MATURITY, INTEREST_POSTING_NOT_ALLOWED_BEFORE_MATURITY_FOR_TENURE;

        public String errorMessage() {
            if (name().toString().equalsIgnoreCase("FUTURE_DATE")) {
                return "Cannot Post Interest in future Dates";
            } else if (name().toString().equalsIgnoreCase("VALID_DATE")) {
                return "Please Pass a valid date";
            } else if (name().toString().equalsIgnoreCase("ACTIVATION_DATE")) {
                return "Post Interest Date must be after the Activation date";
            } else if (name().toString().equalsIgnoreCase("LAST_TRANSACTION_DATE")) {
                return "Cannot Post Interest before last transaction date";
            } else if (name().toString().equalsIgnoreCase("INTEREST_POSTING_NOT_ALLOWED_AFTER_MATURITY")) {
                return "Interest posting is not allowed after maturity date. Account has already matured.";
            } else if (name().toString().equalsIgnoreCase("INTEREST_POSTING_NOT_ALLOWED_BEFORE_MATURITY_FOR_TENURE")) {
                return "Interest posting is not allowed before maturity date when interest posting period is At-Maturity. Interest will be posted only at maturity.";
            }
            return name().toString();
        }

        public String errorCode() {
            if (name().toString().equalsIgnoreCase("FUTURE_DATE")) {
                return "error.msg.futureDate";
            } else if (name().toString().equalsIgnoreCase("VALID_DATE")) {
                return "error.msg.nullDatePassed";
            } else if (name().toString().equalsIgnoreCase("ACTIVATION_DATE")) {
                return "error.msg.before activation date";
            } else if (name().toString().equalsIgnoreCase("LAST_TRANSACTION_DATE")) {
                return "error.msg.countInterest";
            } else if (name().toString().equalsIgnoreCase("INTEREST_POSTING_NOT_ALLOWED_AFTER_MATURITY")) {
                return "error.msg.interest.posting.not.allowed.after.maturity";
            } else if (name().toString().equalsIgnoreCase("INTEREST_POSTING_NOT_ALLOWED_BEFORE_MATURITY_FOR_TENURE")) {
                return "error.msg.interest.posting.not.allowed.before.maturity.for.at.maturity.accounts";
            }
            return name().toString();
        }
    }

    public PostInterestAsOnDateException(final PostInterestAsOnExceptionType reason) {
        super(reason.errorCode(), reason.errorMessage());
    }

}
