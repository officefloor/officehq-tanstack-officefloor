package net.officefloor.hq.app.rate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** An older version of a grade's rate for a from-date, as it stood when a newer one replaced it, with the sheet's
 *  today then. */
@Entity
@Table(name = "rate_version")
public class RateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(nullable = false)
    private String grade;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal pct;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "replaced_on", nullable = false)
    private LocalDate replacedOn;

    protected RateVersion() {
    }

    public RateVersion(Rate rate, LocalDate replacedOn) {
        this.sheetId = rate.getSheetId();
        this.grade = rate.getGrade();
        this.fromDate = rate.getFromDate();
        this.version = rate.getVersion();
        this.pct = rate.getPct();
        this.toDate = rate.getToDate();
        this.replacedOn = replacedOn;
    }

    public int getVersion() {
        return version;
    }

    public BigDecimal getPct() {
        return pct;
    }
}
