package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Project}. A Spring Data bean, injected into the OfficeFloor logic classes
 * that back the /api/projects routes.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** All projects, oldest first, so the list order is stable for the UI and tests. */
    List<Project> findAllByOrderByIdAsc();
}
