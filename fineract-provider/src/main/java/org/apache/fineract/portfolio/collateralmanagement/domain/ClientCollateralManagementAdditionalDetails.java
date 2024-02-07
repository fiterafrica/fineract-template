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
package org.apache.fineract.portfolio.collateralmanagement.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;

@Entity
@Table(name = "m_client_collateral_management_additional_details")
public class ClientCollateralManagementAdditionalDetails extends AbstractPersistableCustom {

    @OneToOne(optional = false)
    @JoinColumn(name = "client_collateral_id", nullable = false)
    private ClientCollateralManagement clientCollateral;

    @Column(name = "UPI_NO")
    private String upiNo;

    @Column(name = "chassis_no")
    private String chassisNo;

    @Column(name = "collateral_owner_first", nullable = false)
    private String collateralOwnerFirst;

    @Column(name = "id_no_of_collateral_owner_first", nullable = false)
    private String idNoOfCollateralOwnerFirst;

    @Column(name = "collateral_owner_second")
    private String collateralOwnerSecond;

    @Column(name = "id_no_of_collateral_owner_second")
    private String idNoOfCollateralOwnerSecond;

    @Column(name = "worth_of_collateral")
    private BigDecimal worthOfCollateral;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_cv_id")
    private CodeValue province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_cv_id")
    private CodeValue district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_cv_id")
    private CodeValue sector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cell_cv_id")
    private CodeValue cell;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_cv_id")
    private CodeValue village;

    public ClientCollateralManagementAdditionalDetails() {

    }

    public ClientCollateralManagementAdditionalDetails(ClientCollateralManagement clientCollateral, String upiNo, String chassisNo,
            String collateralOwnerFirst, String idNoOfCollateralOwnerFirst, String collateralOwnerSecond,
            String idNoOfCollateralOwnerSecond, BigDecimal worthOfCollateral, CodeValue province, CodeValue district, CodeValue sector,
            CodeValue cell, CodeValue village) {
        this.clientCollateral = clientCollateral;
        this.upiNo = upiNo;
        this.chassisNo = chassisNo;
        this.collateralOwnerFirst = collateralOwnerFirst;
        this.idNoOfCollateralOwnerFirst = idNoOfCollateralOwnerFirst;
        this.collateralOwnerSecond = collateralOwnerSecond;
        this.idNoOfCollateralOwnerSecond = idNoOfCollateralOwnerSecond;
        this.province = province;
        this.district = district;
        this.sector = sector;
        this.cell = cell;
        this.village = village;
        this.worthOfCollateral = worthOfCollateral;
    }

    public static ClientCollateralManagementAdditionalDetails createNew(final ClientCollateralManagement clientCollateral,
            JsonCommand command, final CodeValue province, final CodeValue district, final CodeValue sector, final CodeValue cell,
            final CodeValue village) {
        final String upiNo = command.stringValueOfParameterNamed("upiNo");
        final String chassisNo = command.stringValueOfParameterNamed("chassisNo");
        final String collateralOwnerFirst = command.stringValueOfParameterNamed("collateralOwnerFirst");
        final String idNoOfCollateralOwnerFirst = command.stringValueOfParameterNamed("idNoOfCollateralOwnerFirst");
        final String collateralOwnerSecond = command.stringValueOfParameterNamed("collateralOwnerSecond");
        final String idNoOfCollateralOwnerSecond = command.stringValueOfParameterNamed("idNoOfCollateralOwnerSecond");
        final BigDecimal worthOfCollateral = command.bigDecimalValueOfParameterNamed("worthOfCollateral");

        return new ClientCollateralManagementAdditionalDetails(clientCollateral, upiNo, chassisNo, collateralOwnerFirst,
                idNoOfCollateralOwnerFirst, collateralOwnerSecond, idNoOfCollateralOwnerSecond, worthOfCollateral, province, district,
                sector, cell, village);
    }

    public Map<String, Object> update(JsonCommand command) {
        final Map<String, Object> changes = new LinkedHashMap<>(15);

        if (command.isChangeInStringParameterNamed(ClientApiConstants.upiNoParamName, this.upiNo)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.upiNoParamName);
            changes.put(ClientApiConstants.upiNoParamName, newValue);
            this.upiNo = newValue;
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.chassisNoParamName, this.chassisNo)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.chassisNoParamName);
            changes.put(ClientApiConstants.chassisNoParamName, newValue);
            this.chassisNo = newValue;
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.collateralOwnerFirstParamName, this.collateralOwnerFirst)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.collateralOwnerFirstParamName);
            changes.put(ClientApiConstants.collateralOwnerFirstParamName, newValue);
            this.collateralOwnerFirst = newValue;
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.idNoOfCollateralOwnerFirstParamName,
                this.idNoOfCollateralOwnerFirst)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.idNoOfCollateralOwnerFirstParamName);
            changes.put(ClientApiConstants.idNoOfCollateralOwnerFirstParamName, newValue);
            this.idNoOfCollateralOwnerFirst = newValue;
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.collateralOwnerSecondParamName, this.collateralOwnerSecond)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.collateralOwnerSecondParamName);
            changes.put(ClientApiConstants.collateralOwnerSecondParamName, newValue);
            this.collateralOwnerSecond = newValue;
        }
        if (command.isChangeInStringParameterNamed(ClientApiConstants.idNoOfCollateralOwnerSecondParamName,
                this.idNoOfCollateralOwnerSecond)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.idNoOfCollateralOwnerSecondParamName);
            changes.put(ClientApiConstants.idNoOfCollateralOwnerSecondParamName, newValue);
            this.idNoOfCollateralOwnerSecond = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed(ClientApiConstants.worthOfCollateralParamName, this.worthOfCollateral)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(ClientApiConstants.worthOfCollateralParamName);
            changes.put(ClientApiConstants.worthOfCollateralParamName, newValue);
            this.worthOfCollateral = newValue;
        }
        if (command.isChangeInLongParameterNamed(ClientApiConstants.provinceIdParamName, this.provinceId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.provinceIdParamName);
            changes.put(ClientApiConstants.provinceIdParamName, newValue);
        }
        if (command.isChangeInLongParameterNamed(ClientApiConstants.districtIdParamName, this.districtId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.districtIdParamName);
            changes.put(ClientApiConstants.districtIdParamName, newValue);
        }
        if (command.isChangeInLongParameterNamed(ClientApiConstants.sectorIdParamName, this.sectorId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.sectorIdParamName);
            changes.put(ClientApiConstants.sectorIdParamName, newValue);
        }
        if (command.isChangeInLongParameterNamed(ClientApiConstants.cellIdParamName, this.cellId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.cellIdParamName);
            changes.put(ClientApiConstants.cellIdParamName, newValue);
        }
        if (command.isChangeInLongParameterNamed(ClientApiConstants.villageIdParamName, this.villageId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.villageIdParamName);
            changes.put(ClientApiConstants.villageIdParamName, newValue);
        }
        return changes;
    }

    private Long provinceId() {
        return this.province == null ? null : this.province.getId();
    }

    private Long districtId() {
        return this.district == null ? null : this.district.getId();
    }

    private Long sectorId() {
        return this.sector == null ? null : this.sector.getId();
    }

    private Long cellId() {
        return this.cell == null ? null : this.cell.getId();
    }

    private Long villageId() {
        return this.village == null ? null : this.village.getId();
    }

    public void updateProvince(final CodeValue province) {
        this.province = province;
    }

    public void updateDistrict(final CodeValue district) {
        this.district = district;
    }

    public void updateSector(final CodeValue sector) {
        this.sector = sector;
    }

    public void updateCell(final CodeValue cell) {
        this.cell = cell;
    }

    public void updateVillage(final CodeValue village) {
        this.village = village;
    }

    public String getUpiNo() {
        return upiNo;
    }

    public String getChassisNo() {
        return chassisNo;
    }

    public String getCollateralOwnerFirst() {
        return collateralOwnerFirst;
    }

    public String getIdNoOfCollateralOwnerFirst() {
        return idNoOfCollateralOwnerFirst;
    }

    public String getCollateralOwnerSecond() {
        return collateralOwnerSecond;
    }

    public String getIdNoOfCollateralOwnerSecond() {
        return idNoOfCollateralOwnerSecond;
    }

    public BigDecimal getWorthOfCollateral() {
        return worthOfCollateral;
    }

    public CodeValue getProvince() {
        return province;
    }

    public CodeValue getDistrict() {
        return district;
    }

    public CodeValue getSector() {
        return sector;
    }

    public CodeValue getCell() {
        return cell;
    }

    public CodeValue getVillage() {
        return village;
    }
}
