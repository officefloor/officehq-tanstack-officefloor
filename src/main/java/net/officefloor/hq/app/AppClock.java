package net.officefloor.hq.app;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The fixed reference date ("as of") the dashboard measures "overdue" against, so the overdue count
 * is deterministic rather than dependent on the wall clock. Persisted as a single row (id = 1) in the
 * {@code app_clock} table (Flyway V23). The harness seed sets it (see {@link TestSupportController});
 * when absent the overdue endpoint falls back to the real current date.
 */
@Entity
@Table(name = "app_clock")
public class AppClock {

    @Id
    private Long id;

    @Column(name = "as_of")
    private LocalDate asOf;

    public AppClock() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getAsOf() {
        return asOf;
    }

    public void setAsOf(LocalDate asOf) {
        this.asOf = asOf;
    }
}
