package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Client}. A Spring Data bean, injected into the OfficeFloor logic classes
 * (Spring beans are exposed to OfficeFloor by the rest-spring-boot starter).
 */
public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findAllByOrderByIdAsc();

    /**
     * Every client that has not been tucked away, oldest id first — archived clients are excluded so
     * a tucked-away client drops off the list (and its search, which filters this same list).
     */
    List<Client> findAllByArchivedFalseOrderByIdAsc();
}
