package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Data access for {@link Payment}. A Spring Data bean, injected into the OfficeFloor logic classes
 * (Spring beans are exposed to OfficeFloor by the rest-spring-boot starter).
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Every payment on one invoice, oldest id first — the shape an invoice's payment list renders. */
    List<Payment> findByInvoiceIdOrderByIdAsc(Long invoiceId);

    /**
     * Total already settled against one invoice: the sum of its payments. COALESCE keeps it 0 (never
     * null) when the invoice has no payments yet, so the amount-due calculation stays purely
     * arithmetic (amount - paid).
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoiceId = ?1")
    BigDecimal sumByInvoiceId(Long invoiceId);
}
