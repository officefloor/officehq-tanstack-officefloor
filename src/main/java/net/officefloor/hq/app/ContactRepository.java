package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Contact}. A Spring Data bean, injected into the OfficeFloor logic classes
 * (Spring beans are exposed to OfficeFloor by the rest-spring-boot starter).
 */
public interface ContactRepository extends JpaRepository<Contact, Long> {

    /** Every contact belonging to one client, oldest id first — a client's own contact list. */
    List<Contact> findByClientIdOrderByIdAsc(Long clientId);
}
