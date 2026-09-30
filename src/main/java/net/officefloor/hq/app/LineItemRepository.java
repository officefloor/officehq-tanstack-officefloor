package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link LineItem}. A Spring Data bean, injected into the OfficeFloor logic classes
 * (Spring beans are exposed to OfficeFloor by the rest-spring-boot starter).
 */
public interface LineItemRepository extends JpaRepository<LineItem, Long> {

    /** Every line on one invoice, oldest id first — the shape an invoice's line-item list renders. */
    List<LineItem> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
