package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Invoice}. A Spring Data bean, injected into the OfficeFloor logic classes
 * that back the /api/invoices routes.
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /** All invoices, oldest first, so the list order is stable for the UI and tests. */
    List<Invoice> findAllByOrderByIdAsc();
}
