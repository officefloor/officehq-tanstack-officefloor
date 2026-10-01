package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Invoice}. Injected into the OfficeFloor REST logic classes. */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /** The invoices for one project, oldest first, so a project's detail page lists only its own. */
    List<Invoice> findByProjectIdOrderByIdAsc(Long projectId);
}
