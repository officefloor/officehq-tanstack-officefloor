package net.officefloor.hq.app;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Client}. Injected into the OfficeFloor REST logic classes. */
public interface ClientRepository extends JpaRepository<Client, Long> {
}
