package net.officefloor.hq.app;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Project}. Injected into the OfficeFloor REST logic classes. */
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
