package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Task}. Injected into the OfficeFloor REST logic classes. */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** The tasks for one project, oldest first, so a project's detail page lists only its own. */
    List<Task> findByProjectIdOrderByIdAsc(Long projectId);
}
