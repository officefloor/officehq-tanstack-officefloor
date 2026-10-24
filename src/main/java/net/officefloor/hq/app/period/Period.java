package net.officefloor.hq.app.period;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** A sheet's period: a name (unique on the sheet) and a from and to date, inclusive; once closed, its due-total
 *  and each person's are fixed at what they were on closing, until a steward reopens it. */
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

    /** Whether the period is closed: no claim whose due-by falls in it can be added, edited or change status. */
    @Column(nullable = false)
    private boolean closed;

    /** Whether the period is soft-closed: its figures are fixed as on closing, but claims whose due-by falls in it can still be
     *  added, edited and change status. A period is never both closed and soft-closed. */
    @Column(name = "soft_closed", nullable = false)
    private boolean softClosed;

    /** The due-total fixed when the period was closed (or soft-closed); null while it is open. */
    @Column(name = "closed_total", precision = 19, scale = 2)
    private BigDecimal closedTotal;

    /** Each person's due-total fixed when the period was closed, by person id; empty while it is open. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "period_person_total", joinColumns = @JoinColumn(name = "period_id"))
    @MapKeyColumn(name = "person_id")
    @Column(name = "total", precision = 19, scale = 2)
    private Map<Long, BigDecimal> personTotals = new HashMap<>();

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

    public boolean isSoftClosed() {
        return softClosed;
    }

    public BigDecimal getClosedTotal() {
        return closedTotal;
    }

    public Map<Long, BigDecimal> getPersonTotals() {
        return personTotals;
    }

    /** Closes the period, fixing each person's due-total for it and its own, their sum. */
    public void close(Map<Long, BigDecimal> personTotals) {
        this.closed = true;
        this.softClosed = false;
        fix(personTotals);
    }

    /** Soft-closes the period, fixing each person's due-total for it and its own, their sum, as closing does. */
    public void softClose(Map<Long, BigDecimal> personTotals) {
        this.softClosed = true;
        fix(personTotals);
    }

    private void fix(Map<Long, BigDecimal> personTotals) {
        this.personTotals.clear();
        this.personTotals.putAll(personTotals);
        this.closedTotal = personTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    /** Reopens the period, so its due-total is live again. */
    public void reopen() {
        this.closed = false;
        this.softClosed = false;
        this.closedTotal = null;
        this.personTotals.clear();
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
