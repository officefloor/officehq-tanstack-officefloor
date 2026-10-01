package net.officefloor.hq.app;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Client}. A Spring bean, injected into the OfficeFloor logic classes. */
public interface ClientRepository extends JpaRepository<Client, Long> {
}
