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

    /**
     * Non-archived projects whose name contains {@code term} (case-insensitive), oldest first —
     * backs the global search ({@code GET /api/search?q=...}). Archived projects are tucked away, so
     * they drop off the search, mirroring how the clients search excludes archived clients.
     */
    List<Project> findByArchivedFalseAndNameContainingIgnoreCaseOrderByIdAsc(String term);

    /** Whether any project already carries {@code code} — backs the uniqueness check on create. */
    boolean existsByCode(String code);
}
