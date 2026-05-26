package com.raksha.sentinelcore.repository;

import com.raksha.sentinelcore.entity.Entitlement;
import com.raksha.sentinelcore.enums.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {

    List<Entitlement> findByPlan(PlanType plan);
}
