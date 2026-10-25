package net.officefloor.hq.app.plan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanChangeRepository extends JpaRepository<PlanChange, Long> {

    /** The plan's amount changes, oldest first. */
    List<PlanChange> findByPlanIdOrderByIdAsc(Long planId);
}
