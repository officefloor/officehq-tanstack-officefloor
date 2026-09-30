package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Invoice}. A Spring Data bean, injected into the OfficeFloor logic classes
 * (Spring beans are exposed to OfficeFloor by the rest-spring-boot starter).
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /** Every invoice for one project, oldest id first — the shape a project's list renders. */
    List<Invoice> findByProjectIdOrderByIdAsc(Long projectId);
}
