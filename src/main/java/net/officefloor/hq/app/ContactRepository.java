package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Contact}. A Spring bean, injected into the OfficeFloor logic classes. */
public interface ContactRepository extends JpaRepository<Contact, Long> {

    /** The contacts of one client, in id order — the shape a client's detail page lists. */
    List<Contact> findByClientIdOrderByIdAsc(Long clientId);

    /** How many contacts one client keeps — the figure the client detail badge surfaces. */
    long countByClientId(Long clientId);
}
