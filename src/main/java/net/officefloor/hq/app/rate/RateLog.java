package net.officefloor.hq.app.rate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One rate set on a sheet, as it was set, with the sheet's today then; when forward-only, the highest claim id there
 *  was then (it applies only to claims with an id above it). */
@Entity
@Table(name = "rate_log")
public class RateLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal pct;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "made_on", nullable = false)
    private LocalDate madeOn;

    @Column(name = "forward_after")
    private Long forwardAfter;

    protected RateLog() {
    }

    public RateLog(Rate rate, LocalDate madeOn, Long forwardAfter) {
        this(rate, madeOn);
        this.forwardAfter = forwardAfter;
    }

    public RateLog(Rate rate, LocalDate madeOn) {
        this.sheetId = rate.getSheetId();
        this.grade = rate.getGrade();
        this.pct = rate.getPct();
        this.fromDate = rate.getFromDate();
        this.toDate = rate.getToDate();
        this.madeOn = madeOn;
    }

    public String getGrade() {
        return grade;
    }

    public BigDecimal getPct() {
        return pct;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public LocalDate getMadeOn() {
        return madeOn;
    }

    /** The highest claim id there was when it was set forward-only (null: it applies to every claim). */
    public Long getForwardAfter() {
        return forwardAfter;
    }
}
