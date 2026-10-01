package net.officefloor.hq.app;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link DashboardReference}. A Spring bean, injected into the OfficeFloor logic
 * classes. At most one row exists (the seeded reference date); none means "use today".
 */
public interface DashboardReferenceRepository extends JpaRepository<DashboardReference, Long> {
}
