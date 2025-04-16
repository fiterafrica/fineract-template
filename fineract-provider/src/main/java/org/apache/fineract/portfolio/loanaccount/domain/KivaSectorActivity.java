package org.apache.fineract.portfolio.loanaccount.domain;

import lombok.Data;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "m_kiva_sector_activity")
public class KivaSectorActivity extends AbstractPersistableCustom {

    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "sector_name")
    private String sector;

    @Column(name = "activity_name")
    private String activity;

}
