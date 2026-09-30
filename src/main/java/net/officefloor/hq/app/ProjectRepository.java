package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Project}. A Spring Data bean, injected into the OfficeFloor logic classes
 * (Spring beans are exposed to OfficeFloor by the rest-spring-boot starter).
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** Every project with its client's name, oldest id first — the shape the list renders. */
    @Query("SELECT new net.officefloor.hq.app.ProjectView(p.id, p.name, c.name) "
            + "FROM Project p, Client c WHERE p.clientId = c.id ORDER BY p.id ASC")
    List<ProjectView> findAllViews();

    /** Every project belonging to one client, oldest id first — a client's own project list. */
    @Query("SELECT new net.officefloor.hq.app.ProjectView(p.id, p.name, c.name) "
            + "FROM Project p, Client c WHERE p.clientId = c.id AND p.clientId = :clientId "
            + "ORDER BY p.id ASC")
    List<ProjectView> findViewsByClientId(@Param("clientId") Long clientId);
}
