package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Tag}. A Spring Data bean, injected into the OfficeFloor logic classes that
 * back the /api/tags and /api/project-tags routes.
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /** All tags, oldest first, so the list order is stable for the UI and tests. */
    List<Tag> findAllByOrderByIdAsc();
}
