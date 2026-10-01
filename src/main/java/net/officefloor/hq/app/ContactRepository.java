package net.officefloor.hq.app;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Contact}. Injected into the OfficeFloor REST logic classes. */
public interface ContactRepository extends JpaRepository<Contact, Long> {
}
