package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link ProjectTag}. A Spring bean, injected into the OfficeFloor logic classes. */
public interface ProjectTagRepository extends JpaRepository<ProjectTag, ProjectTagId> {

    /** The links of one project, in tag-id order — which tags are on this project. */
    List<ProjectTag> findByProjectIdOrderByTagIdAsc(Long projectId);
}
