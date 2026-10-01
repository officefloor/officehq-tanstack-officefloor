package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link InvoicePayment}. A Spring bean, injected into the OfficeFloor logic. */
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long> {

    /** The payments made against one invoice, in id order — the shape the invoice page lists. */
    List<InvoicePayment> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
