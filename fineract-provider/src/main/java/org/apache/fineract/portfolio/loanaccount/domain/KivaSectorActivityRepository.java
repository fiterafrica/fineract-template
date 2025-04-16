package org.apache.fineract.portfolio.loanaccount.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface KivaSectorActivityRepository extends JpaRepository<KivaSectorActivity, Long>, JpaSpecificationExecutor<KivaSectorActivity> {

    Optional<KivaSectorActivity> findBySectorAndActivity(String sectorName, String activityName);

}
