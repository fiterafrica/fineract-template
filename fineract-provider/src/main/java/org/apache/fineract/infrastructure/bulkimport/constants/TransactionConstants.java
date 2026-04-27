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
package org.apache.fineract.infrastructure.bulkimport.constants;

public final class TransactionConstants {

    private TransactionConstants() {

    }

    public static final int OFFICE_NAME_COL = 0;
    public static final int SAVINGS_ACCOUNT_NO_COL = 1;
    public static final int PRODUCT_COL = 2;
    public static final int OPENING_BALANCE_COL = 3;
    public static final int TRANSACTION_TYPE_COL = 4;
    public static final int AMOUNT_COL = 5;
    public static final int TRANSACTION_DATE_COL = 6;
    public static final int PAYMENT_TYPE_COL = 7;
    public static final int ACCOUNT_NO_COL = 8;
    public static final int CHECK_NO_COL = 9;
    public static final int ROUTING_CODE_COL = 10;
    public static final int RECEIPT_NO_COL = 11;
    public static final int BANK_NO_COL = 12;
    public static final int TRANSACTION_REFERENCE_COL = 13;
    public static final int STATUS_COL = 14;
    public static final int FAILURE_REPORT_COL = 15;
    public static final int LOOKUP_CLIENT_NAME_COL = 16;
    public static final int LOOKUP_ACCOUNT_NO_COL = 17;
    public static final int LOOKUP_PRODUCT_COL = 18;
    public static final int LOOKUP_OPENING_BALANCE_COL = 19;
    public static final int LOOKUP_SAVINGS_ACTIVATION_DATE_COL = 20;
    public static final int LOOKUP_SAVINGS_ID_COL = 21;

    // Transaction type constants
    public static final String TRANSACTION_TYPE_WITHDRAWAL = "Withdrawal";
    public static final String TRANSACTION_TYPE_DEPOSIT = "Deposit";

}
