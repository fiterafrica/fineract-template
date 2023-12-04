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

import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@XmlRootElement(name = "ws:getProduct168")
@Data
public class TransUnionRwandaCorporateVerificationData {

    private Integer id;
    private String username;

    private String password;

    private String code;
    private String infinityCode;
    private String name1;
    private String name2;
    private String name3;
    private String name4;
    private String nationalID;
    private String passportNo;
    private int reportSector;
    private int reportReason;
    private String companyName;
    private String companyRegNo;

    public TransUnionRwandaCorporateVerificationData() {}

    public TransUnionRwandaCorporateVerificationData(Integer id, String companyName, String companyRegNo) {
        this.id = id;
        this.companyName = companyName;
        this.companyRegNo = companyRegNo;
    }
}