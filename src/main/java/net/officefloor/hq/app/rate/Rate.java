package net.officefloor.hq.app.rate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A sheet's rate for a grade: a percent with two decimals, from a date on (null: from the beginning of time),
 *  through a to-date (null: no end); the newest of its versions, numbered from 1 (older ones are in {@link RateVersion}). */
@Entity
@Table(name = "rate")
public class Rate {

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

    @Column(nullable = false)
    private int version = 1;

    protected Rate() {
    }

    public Rate(String sheetId, String grade, BigDecimal pct, LocalDate fromDate, LocalDate toDate) {
        this.sheetId = sheetId;
        this.grade = grade;
        this.pct = pct;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public Long getId() {
        return id;
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getGrade() {
        return grade;
    }

    public BigDecimal getPct() {
        return pct;
    }

    public void setPct(BigDecimal pct) {
        this.pct = pct;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    /** The last date it applies on (null: no end). */
    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    /** Its version number: how many versions it has had (1 when first set). */
    public int getVersion() {
        return version;
    }

    /** Makes it the next version: the caller keeps the one it replaces. */
    public void nextVersion() {
        version++;
    }

    /** Whether it has ended by a date: it has a to-date before it. */
    public boolean endedBy(LocalDate on) {
        return toDate != null && toDate.isBefore(on);
    }
}
