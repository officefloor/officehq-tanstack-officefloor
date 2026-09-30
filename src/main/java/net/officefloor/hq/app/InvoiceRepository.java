package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Data access for {@link Invoice}. A Spring Data bean, injected into the OfficeFloor logic classes
 * (Spring beans are exposed to OfficeFloor by the rest-spring-boot starter).
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /** Every invoice for one project, oldest id first — the shape a project's list renders. */
    List<Invoice> findByProjectIdOrderByIdAsc(Long projectId);

    /**
     * Total amount still owed: the sum of every UNPAID invoice's amount across all projects.
     * COALESCE keeps it 0 (never null) when nothing is outstanding — the dashboard's headline
     * figure.
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.status = 'UNPAID'")
    BigDecimal sumOutstanding();
}
