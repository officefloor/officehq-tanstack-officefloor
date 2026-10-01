package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Invoice}. A Spring bean, injected into the OfficeFloor logic classes. */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /** The invoices of one project, in id order — the shape the project detail page lists. */
    List<Invoice> findByProjectIdOrderByIdAsc(Long projectId);
}
