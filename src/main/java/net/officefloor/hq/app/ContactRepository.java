package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Contact}. A Spring Data bean, injected into the OfficeFloor logic classes
 * that back the /api/contacts routes.
 */
public interface ContactRepository extends JpaRepository<Contact, Long> {

    /** All contacts, oldest first, so the list order is stable for the UI and tests. */
    List<Contact> findAllByOrderByIdAsc();
}
