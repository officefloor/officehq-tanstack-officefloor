package net.officefloor.hq.app;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link ProjectTag} (the project↔tag join). A Spring Data bean, injected into the
 * OfficeFloor logic classes that back the /api/project-tags routes.
 */
public interface ProjectTagRepository extends JpaRepository<ProjectTag, Long> {

    /** Every project-tag pairing, oldest first, so the list order is stable for the UI and tests. */
    List<ProjectTag> findAllByOrderByIdAsc();

    /** The pairing of a given project and tag, if that tag is already on the project. */
    Optional<ProjectTag> findByProjectIdAndTagId(Long projectId, Long tagId);

    /** Whether the tag is already on the project (so it is not applied twice). */
    boolean existsByProjectIdAndTagId(Long projectId, Long tagId);
}
