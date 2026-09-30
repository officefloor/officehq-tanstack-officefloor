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

    /**
     * Every project (archived included) with its client's name and archived flag, oldest id first.
     * The main list carries the flag so it can hide archived rows by default and reveal them on the
     * show-archived toggle without a second request.
     */
    @Query("SELECT new net.officefloor.hq.app.ProjectView(p.id, p.name, c.name, p.archived, p.status, p.code) "
            + "FROM Project p, Client c WHERE p.clientId = c.id ORDER BY p.id ASC")
    List<ProjectView> findAllViews();

    /** Whether a project already uses this reference code — no two projects may share one. */
    boolean existsByCode(String code);

    /**
     * A client's own project list, oldest id first — every project (finished and archived included),
     * each carrying its {@code status} and {@code archived} flag. The client's page shows only the
     * ACTIVE, non-archived ones by default and reveals the finished and hidden ones on its toggle,
     * so the list carries the flags and does the filtering without a second request.
     */
    @Query("SELECT new net.officefloor.hq.app.ProjectView(p.id, p.name, c.name, p.archived, p.status, p.code) "
            + "FROM Project p, Client c WHERE p.clientId = c.id AND p.clientId = :clientId "
            + "ORDER BY p.id ASC")
    List<ProjectView> findViewsByClientId(@Param("clientId") Long clientId);
}
