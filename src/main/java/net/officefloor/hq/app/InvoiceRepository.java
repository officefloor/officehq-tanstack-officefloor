package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Data access for {@link Invoice}. A Spring Data bean, injected into the OfficeFloor logic classes
 * that back the /api/invoices routes.
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /** All invoices, oldest first, so the list order is stable for the UI and tests. */
    List<Invoice> findAllByOrderByIdAsc();

    /**
     * Every invoice at one lifecycle stage, oldest first, backing the all-invoices status filter.
     * The stable oldest-first order matches {@link #findAllByOrderByIdAsc()} so filtering only
     * narrows the list, never reorders it.
     */
    List<Invoice> findByStatusOrderByIdAsc(String status);

    /**
     * All invoices, earliest due date first (id as a stable tiebreaker), backing the project
     * invoices' sort-by-due-date control.
     */
    List<Invoice> findAllByOrderByDueDateAscIdAsc();

    /**
     * The money still owed: the sum of every UNPAID invoice's amount, or 0 when none are unpaid.
     * Backs the dashboard's outstanding total ({@code GET /api/dashboard}).
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.status = 'UNPAID'")
    BigDecimal sumUnpaidAmount();
}
