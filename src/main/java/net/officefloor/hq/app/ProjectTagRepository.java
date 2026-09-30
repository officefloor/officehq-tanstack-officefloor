package net.officefloor.hq.app;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link ProjectTag} (the project-to-label pairings). A Spring Data bean, injected
 * into the OfficeFloor logic classes (Spring beans are exposed to OfficeFloor by the
 * rest-spring-boot starter).
 */
public interface ProjectTagRepository extends JpaRepository<ProjectTag, ProjectTagId> {
}
