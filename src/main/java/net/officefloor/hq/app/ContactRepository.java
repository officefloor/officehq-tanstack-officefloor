package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Contact}. Injected into the OfficeFloor REST logic classes. */
public interface ContactRepository extends JpaRepository<Contact, Long> {

    /** Every contact belonging to one client — used to keep "one main contact per client". */
    List<Contact> findByClientId(Long clientId);
}
