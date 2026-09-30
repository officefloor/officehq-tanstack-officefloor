package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Task}. A Spring Data bean, injected into the OfficeFloor logic classes
 * (Spring beans are exposed to OfficeFloor by the rest-spring-boot starter).
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Every task on one project, oldest id first — the shape a project's checklist renders. */
    List<Task> findByProjectIdOrderByIdAsc(Long projectId);
}
