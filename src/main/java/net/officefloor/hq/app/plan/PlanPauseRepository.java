package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanPauseRepository extends JpaRepository<PlanPause, Long> {

    /** The plan's pauses, in the order they were taken. */
    List<PlanPause> findByPlanIdOrderByIdAsc(Long planId);

    /** Whether the plan date falls in one of the plan's pauses, and so is skipped. */
    default boolean skips(Long planId, LocalDate date) {
        return findByPlanIdOrderByIdAsc(planId).stream().anyMatch(p -> p.skips(date));
    }

    /** Whether the plan is paused now: its latest pause not yet resumed. */
    default boolean isPaused(Long planId) {
        return findByPlanIdOrderByIdAsc(planId).stream().anyMatch(PlanPause::isOpen);
    }
}
