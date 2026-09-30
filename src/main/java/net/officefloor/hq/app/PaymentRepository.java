package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Payment}. A Spring Data bean, injected into the OfficeFloor logic classes
 * (Spring beans are exposed to OfficeFloor by the rest-spring-boot starter).
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Every payment on one invoice, oldest id first — the shape an invoice's payment list renders. */
    List<Payment> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
