package net.officefloor.hq.app.plan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanClaimRepository extends JpaRepository<PlanClaim, Long> {

    /** The claims the plan has made. */
    List<PlanClaim> findByPlanId(Long planId);

    long countByPlanId(Long planId);
}
