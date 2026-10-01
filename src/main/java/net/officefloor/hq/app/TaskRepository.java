package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Task}. A Spring bean, injected into the OfficeFloor logic classes. */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** The tasks of one project, in id order — the shape the project detail page lists. */
    List<Task> findByProjectIdOrderByIdAsc(Long projectId);
}
