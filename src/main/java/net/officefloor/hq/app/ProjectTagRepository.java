package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link ProjectTag} (the project↔tag join). Injected into the REST logic classes. */
public interface ProjectTagRepository extends JpaRepository<ProjectTag, Long> {

    /** The tag assignments of one project, oldest first. */
    List<ProjectTag> findByProjectIdOrderByIdAsc(Long projectId);

    /** Whether a project already carries a given tag (guards against double-tagging). */
    boolean existsByProjectIdAndTagId(Long projectId, Long tagId);

    /** The assignment row(s) joining a project and a tag, so one can be removed. */
    List<ProjectTag> findByProjectIdAndTagId(Long projectId, Long tagId);
}
