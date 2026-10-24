package net.officefloor.hq.app.rate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A sheet's rate for a grade: a percent with two decimals, from a date on (null: from the beginning of time). */
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

    protected Rate() {
    }

    public Rate(String sheetId, String grade, BigDecimal pct, LocalDate fromDate) {
        this.sheetId = sheetId;
        this.grade = grade;
        this.pct = pct;
        this.fromDate = fromDate;
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
}
