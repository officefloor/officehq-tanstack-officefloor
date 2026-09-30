package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Client}. A Spring Data bean, injected into the OfficeFloor logic classes
 * that back the /api/clients routes.
 */
public interface ClientRepository extends JpaRepository<Client, Long> {

    /** All clients, oldest first, so the list order is stable for the UI and tests. */
    List<Client> findAllByOrderByIdAsc();

    /**
     * Clients whose name contains {@code term} (case-insensitive), oldest first. Backs the
     * name search box on the clients page ({@code GET /api/clients?q=...}).
     */
    List<Client> findByNameContainingIgnoreCaseOrderByIdAsc(String term);
}
