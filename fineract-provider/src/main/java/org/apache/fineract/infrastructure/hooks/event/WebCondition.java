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

package org.apache.fineract.infrastructure.hooks.event;

import org.apache.commons.lang3.StringUtils;

public class WebCondition {

    private String field;
    private String operator;
    private String value;

    public WebCondition(String field, String operator, String value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    // Constuctor used to check conditons with 2 fiels e.g {{client.mobileNo}} | NOT_EMPTY
    public WebCondition(String field, String operator) {
        this.field = field;
        this.operator = operator;
        this.value = null;
    }

    public boolean isSatisfied() {
        if ("EQUALS".equalsIgnoreCase(operator)) {
            return field.equals(value);
        } else if ("CONTAINS".equalsIgnoreCase(operator)) {
            return field.contains(value);
        } else if ("NOT_EMPTY".equalsIgnoreCase(operator)) {
            return StringUtils.isNotEmpty(field);
        } else if ("STARTS_WITH".equalsIgnoreCase(operator)) {
            return field.startsWith(value);
        } else if ("EQUALS_NUMERIC".equalsIgnoreCase(operator)) {
            try {
                double inputValue = Double.parseDouble(field);
                double conditionValue = Double.parseDouble(value);
                return Math.abs(inputValue - conditionValue) < 0.0001;
            } catch (NumberFormatException e) {
                return false; // If parsing fails, consider it as not satisfying the condition
            }
        }
        return false;
    }
}
