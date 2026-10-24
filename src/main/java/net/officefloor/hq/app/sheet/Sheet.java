package net.officefloor.hq.app.sheet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A sheet, chosen by its id; everything else in the app is kept per sheet. */
@Entity
@Table(name = "sheet")
public class Sheet {

    @Id
    private String id;

    /** The sheet's today: every date decision and default date uses it, never the real clock. */
    @Column(nullable = false)
    private LocalDate today;

    /** The clerk limit: a clerk settles a claim only when its due is at or below it; null when there is none. */
    @Column(name = "clerk_limit", precision = 19, scale = 2)
    private BigDecimal clerkLimit;

    /** The floor: a claim's live due is at least it; a sheet starts at 1.00. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal floor = Sheet.DEFAULT_FLOOR;

    /** The floor a sheet starts at. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    protected Sheet() {
    }

    public Sheet(String id) {
        this.id = id;
        this.today = LocalDate.now(); // a new sheet starts on the real date
    }

    public String getId() {
        return id;
    }

    public LocalDate getToday() {
        return today;
    }

    public void setToday(LocalDate today) {
        this.today = today;
    }

    public BigDecimal getClerkLimit() {
        return clerkLimit;
    }

    public void setClerkLimit(BigDecimal clerkLimit) {
        this.clerkLimit = clerkLimit;
    }

    public BigDecimal getFloor() {
        return floor;
    }

    public void setFloor(BigDecimal floor) {
        this.floor = floor;
    }
}
