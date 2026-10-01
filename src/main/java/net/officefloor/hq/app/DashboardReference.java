package net.officefloor.hq.app;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The single reference date the dashboard measures "overdue" against. Maps the one row of the
 * {@code dashboard_reference} table (Flyway V24__dashboard_reference.sql). In a real deploy there is
 * no row and the dashboard falls back to today; the harness seeds a fixed date (asOf) so the overdue
 * count is deterministic. The id is a fixed constant, not generated.
 */
@Entity
@Table(name = "dashboard_reference")
public class DashboardReference {

    @Id
    private Long id;

    @Column(name = "as_of", nullable = false)
    private LocalDate asOf;

    protected DashboardReference() {
    }

    public Long getId() {
        return id;
    }

    public LocalDate getAsOf() {
        return asOf;
    }
}
