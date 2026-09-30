package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Tag}. A Spring Data bean, injected into the OfficeFloor logic classes
 * (Spring beans are exposed to OfficeFloor by the rest-spring-boot starter).
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /** The whole shared pool of labels, oldest id first — the shape the "add a label" picker reads. */
    List<Tag> findAllByOrderByIdAsc();

    /**
     * The labels attached to one project, oldest id first — the chips a project's detail page shows.
     * Joins the tag pool to the {@link ProjectTag} pairings for the project.
     */
    @Query("SELECT t FROM Tag t, ProjectTag pt WHERE pt.tagId = t.id AND pt.projectId = :projectId "
            + "ORDER BY t.id ASC")
    List<Tag> findByProjectId(@Param("projectId") Long projectId);
}
