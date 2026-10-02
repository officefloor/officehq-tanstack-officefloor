package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Payment}. Injected into the OfficeFloor REST logic classes. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** The payments on one invoice, oldest first, so an invoice's detail shows only its own. */
    List<Payment> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
