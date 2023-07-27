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
package org.apache.fineract.portfolio.collateralmanagement.data;

import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.portfolio.collateralmanagement.domain.ClientCollateralManagementAdditionalDetails;

import java.math.BigDecimal;

public final class ClientCollateralManagementAdditionalData {

    private final Long id;
    private final String upiNo;

    private final String chassisNo;

    private final String collateralOwnerFirst;

    private final String idNoOfCollateralOwnerFirst;

    private final String collateralOwnerSecond;

    private final String idNoOfCollateralOwnerSecond;

    private final BigDecimal worthOfCollateral;

    private final CodeValueData province;

    private final CodeValueData district;

    private final CodeValueData sector;

    private final CodeValueData cell;

    private final CodeValueData village;


    public ClientCollateralManagementAdditionalData(Long id, String upiNo, String chassisNo, String collateralOwnerFirst, String idNoOfCollateralOwnerFirst, String collateralOwnerSecond, String idNoOfCollateralOwnerSecond, BigDecimal worthOfCollateral, CodeValueData province, CodeValueData district, CodeValueData sector, CodeValueData cell, CodeValueData village) {
        this.id = id;
        this.upiNo = upiNo;
        this.chassisNo = chassisNo;
        this.collateralOwnerFirst = collateralOwnerFirst;
        this.idNoOfCollateralOwnerFirst = idNoOfCollateralOwnerFirst;
        this.collateralOwnerSecond = collateralOwnerSecond;
        this.idNoOfCollateralOwnerSecond = idNoOfCollateralOwnerSecond;
        this.worthOfCollateral = worthOfCollateral;
        this.province = province;
        this.district = district;
        this.sector = sector;
        this.cell = cell;
        this.village = village;
    }
    public static ClientCollateralManagementAdditionalData instance(ClientCollateralManagementAdditionalDetails details, CodeValueData province, CodeValueData district, CodeValueData sector, CodeValueData cell, CodeValueData village) {
        return new ClientCollateralManagementAdditionalData(details.getId(), details.getUpiNo(), details.getChassisNo(), details.getCollateralOwnerFirst(),details.getIdNoOfCollateralOwnerFirst(), details.getCollateralOwnerSecond(),
                details.getIdNoOfCollateralOwnerSecond(), details.getWorthOfCollateral(), province, district, sector, cell, village);
    }

}