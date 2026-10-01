package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link InvoiceLineItem}. A Spring bean, injected into the OfficeFloor logic. */
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, Long> {

    /** The line items of one invoice, in id order — the shape the invoice detail page lists. */
    List<InvoiceLineItem> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
