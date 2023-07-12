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

package org.apache.fineract.portfolio.client.data;

import java.io.Serializable;
import java.util.Collection;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;

public final class ClientOtherInfoData implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long clientId;
    private CodeValueData yearArrivedInHostCountry;
    private Integer numberOfChildren;
    private Integer numberOfDependents;
    private String coSignors;
    private String guarantor;

    private CodeValueData nationality;
    private CodeValueData strata;

    // template holder
    private Collection<CodeValueData> nationalityOptions;
    private Collection<CodeValueData> strataOptions;

    private Collection<CodeValueData> yearArrivedInHostCountryOptions;

    public ClientOtherInfoData(Long id, Long clientId, CodeValueData strata, CodeValueData yearArrivedInHostCountry,
            CodeValueData nationality, Integer numberOfChildren, Integer numberOfDependents, String coSignors, String guarantor,
            Collection<CodeValueData> nationalityOptions, Collection<CodeValueData> strataOptions,
            Collection<CodeValueData> yearArrivedInHostCountryOptions) {

        this.id = id;
        this.clientId = clientId;
        this.strata = strata;
        this.yearArrivedInHostCountry = yearArrivedInHostCountry;
        this.nationality = nationality;
        this.numberOfChildren = numberOfChildren;
        this.numberOfDependents = numberOfDependents;
        this.coSignors = coSignors;
        this.guarantor = guarantor;
        this.nationalityOptions = nationalityOptions;
        this.strataOptions = strataOptions;
        this.yearArrivedInHostCountryOptions = yearArrivedInHostCountryOptions;
    }

    public static ClientOtherInfoData template(final Collection<CodeValueData> nationalityOptions,
            final Collection<CodeValueData> strataOptions, final Collection<CodeValueData> yearArrivedInHostCountryOptions) {
        Long id = null;
        Long clientId = null;
        CodeValueData strata = null;
        CodeValueData yearArrivedInHostCountry = null;
        CodeValueData nationality = null;
        Integer numberOfChildren = null;
        Integer numberOfDependents = null;
        String coSignors = null;
        String guarantor = null;

        return new ClientOtherInfoData(id, clientId, strata, yearArrivedInHostCountry, nationality, numberOfChildren, numberOfDependents,
                coSignors, guarantor, nationalityOptions, strataOptions, yearArrivedInHostCountryOptions);
    }

    public static ClientOtherInfoData instance(final Long id, final Long clientId, final CodeValueData strata,
            final CodeValueData yearArrivedInHostCountry, final CodeValueData nationality, final Integer numberOfChildren,
            final Integer numberOfDependents, final String coSignors, final String guarantor) {
        return new ClientOtherInfoData(id, clientId, strata, yearArrivedInHostCountry, nationality, numberOfChildren, numberOfDependents,
                coSignors, guarantor, null, null, null);
    }

}