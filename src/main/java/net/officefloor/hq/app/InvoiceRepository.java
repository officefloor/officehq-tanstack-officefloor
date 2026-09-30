package net.officefloor.hq.app;

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
     * Every invoice raised for one client, across all of that client's projects, oldest id first —
     * the shape a client's statement renders. Joins each invoice to its project to filter by the
     * project's client.
     */
    @Query("SELECT i FROM Invoice i, Project p WHERE i.projectId = p.id AND p.clientId = ?1 "
            + "ORDER BY i.id ASC")
    List<Invoice> findByClientId(Long clientId);

    /**
     * Every invoice across all projects with the NAME of the project it belongs to, oldest id
     * first — the shape the one all-invoices list renders.
     */
    @Query("SELECT new net.officefloor.hq.app.InvoiceView(i.id, i.projectId, p.name, i.amount, "
            + "i.status) FROM Invoice i, Project p WHERE i.projectId = p.id ORDER BY i.id ASC")
    List<InvoiceView> findAllViews();

    /**
     * Every SENT invoice across all projects — the invoices that count toward what is owed (a DRAFT
     * has not been billed and a PAID one is settled). The dashboard sums each one's DISCOUNTED amount
     * ({@link ProjectInvoice#owedAmount}) so its outstanding headline reflects the discount, the same
     * way the invoice row and the client statement do.
     */
    List<Invoice> findByStatus(String status);

    /**
     * How many SENT invoices are overdue as of the given reference date: sent-but-not-paid invoices
     * whose due date has already passed. Only SENT invoices count (a DRAFT has not been billed and a
     * PAID one is settled), and the due date must be strictly before {@code asOf}. Dates are ISO
     * strings (YYYY-MM-DD), so a lexical comparison is a date comparison; a null due date is never
     * overdue. The dashboard's overdue headline figure.
     */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.status = 'SENT' AND i.dueDate < ?1")
    long countOverdue(String asOf);
}
