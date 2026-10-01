package net.officefloor.hq.app;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Client}. A Spring bean, injected into the OfficeFloor logic classes. */
public interface ClientRepository extends JpaRepository<Client, Long> {

    /** Whether a client already holds this email — the uniqueness guard CreateClient enforces. */
    boolean existsByEmail(String email);
}
