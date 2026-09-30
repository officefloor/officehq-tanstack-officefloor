package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link LineItem}. A Spring Data bean, injected into the OfficeFloor logic classes
 * that back the /api/lineitems routes.
 */
public interface LineItemRepository extends JpaRepository<LineItem, Long> {

    /** All line items, oldest first, so the list order is stable for the UI and tests. */
    List<LineItem> findAllByOrderByIdAsc();

    /** One invoice's line items, oldest first, used to recompute that invoice's derived amount. */
    List<LineItem> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
