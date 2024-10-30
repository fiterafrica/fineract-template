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

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;

public class LoanDetailToKivaData {

    private Long activity_id;
    private Boolean client_waiver_signed;
    private String currency;
    private String description;
    private Integer description_language_id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date disburse_time;
    private String group_name;
    private String image_encoded;
    private String internal_client_id;
    private String internal_loan_id;
    private String loanuse;
    private String location;
    private Integer theme_type_id;
    private List<KivaLoanAccount> entreps;
    private List<KivaLoanAccountSchedule> schedule;
    private List<Boolean> not_pictured;
    private KivaLoanAccountScheduleParameters schedule_parameters;

    public LoanDetailToKivaData(Long activity_id, Boolean client_waiver_signed, String currency, String description,
            Integer description_language_id, Date disburse_time, String group_name, String image_encoded, String internal_client_id,
            String internal_loan_id, String loanuse, String location, Integer theme_type_id, List<KivaLoanAccount> entreps,
            List<KivaLoanAccountSchedule> schedule, List<Boolean> not_pictured, KivaLoanAccountScheduleParameters schedule_parameters) {
        this.activity_id = activity_id;
        this.client_waiver_signed = client_waiver_signed;
        this.currency = currency;
        this.description = description;
        this.description_language_id = description_language_id;
        this.disburse_time = disburse_time;
        this.group_name = group_name;
        this.image_encoded = image_encoded;
        this.internal_client_id = internal_client_id;
        this.internal_loan_id = internal_loan_id;
        this.loanuse = loanuse;
        this.location = location;
        this.theme_type_id = theme_type_id;
        this.entreps = entreps;
        this.schedule = schedule;
        this.not_pictured = null; // not_pictured set when needs to send group loans;
        this.schedule_parameters = schedule_parameters;
    }

    @Override
    public String toString() {
        return "LoanDetailToKivaData{" + "activity_id=" + activity_id + ", client_waiver_signed=" + client_waiver_signed + ", currency='"
                + currency + '\'' + ", description='" + description + '\'' + ", description_language_id='" + description_language_id + '\''
                + ", disburse_time=" + disburse_time + ", group_name='" + group_name + '\'' + ", image_encoded='" + image_encoded + '\''
                + ", internal_client_id=" + internal_client_id + ", internal_loan_id=" + internal_loan_id + ", loanuse='" + loanuse + '\''
                + ", location='" + location + '\'' + ", theme_type_id='" + theme_type_id + '\'' + ", entreps=" + entreps + ", schedule="
                + schedule + ", schedule_parameters=" + schedule_parameters + '}';
    }
}
