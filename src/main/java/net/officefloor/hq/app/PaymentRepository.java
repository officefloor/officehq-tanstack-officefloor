package net.officefloor.hq.app;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Payment}. A Spring Data bean, injected into the OfficeFloor logic classes
 * that back the /api/payments routes.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Every payment, oldest first, so the list order is stable for the UI and tests. */
    List<Payment> findAllByOrderByIdAsc();
}
