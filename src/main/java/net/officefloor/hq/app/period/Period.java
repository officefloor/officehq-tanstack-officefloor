package net.officefloor.hq.app.period;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A sheet's period: a name (unique on the sheet) and a from and to date, inclusive; once closed, its due-total
 *  is fixed at what it was on closing. */
@Entity
@Table(name = "period")
public class Period {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(nullable = false)
    private String name;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    /** Whether the period is closed: no claim dated in it can be added, edited or change status. */
    @Column(nullable = false)
    private boolean closed;

    /** The due-total fixed when the period was closed; null while it is open. */
    @Column(name = "closed_total", precision = 19, scale = 2)
    private BigDecimal closedTotal;

    protected Period() {
    }

    public Period(String sheetId, String name, LocalDate fromDate, LocalDate toDate) {
        this.sheetId = sheetId;
        this.name = name;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public Long getId() {
        return id;
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getName() {
        return name;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public boolean isClosed() {
        return closed;
    }

    public BigDecimal getClosedTotal() {
        return closedTotal;
    }

    /** Closes the period, fixing its due-total. */
    public void close(BigDecimal total) {
        this.closed = true;
        this.closedTotal = total;
    }

    /** Whether a date falls in this period (inclusive). */
    public boolean contains(LocalDate date) {
        return !date.isBefore(fromDate) && !date.isAfter(toDate);
    }

    /** Whether this period's dates overlap the given ones (both inclusive). */
    public boolean overlaps(LocalDate from, LocalDate to) {
        return !fromDate.isAfter(to) && !from.isAfter(toDate);
    }
}
