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
     * Every invoice across all projects with the NAME of the project it belongs to, oldest id
     * first — the shape the one all-invoices list renders.
     */
    @Query("SELECT new net.officefloor.hq.app.InvoiceView(i.id, i.projectId, p.name, i.amount, "
            + "i.status) FROM Invoice i, Project p WHERE i.projectId = p.id ORDER BY i.id ASC")
    List<InvoiceView> findAllViews();

    /**
     * Total amount still owed: the sum of every not-yet-paid invoice's amount across all projects
     * (DRAFT or SENT). COALESCE keeps it 0 (never null) when nothing is outstanding — the
     * dashboard's headline figure.
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.status <> 'PAID'")
    BigDecimal sumOutstanding();
}
