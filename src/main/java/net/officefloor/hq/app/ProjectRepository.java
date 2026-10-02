package net.officefloor.hq.app;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Project}. Injected into the OfficeFloor REST logic classes. */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** Whether a project already carries this reference code (uniqueness check, Flyway V29). */
    boolean existsByCode(String code);
}
