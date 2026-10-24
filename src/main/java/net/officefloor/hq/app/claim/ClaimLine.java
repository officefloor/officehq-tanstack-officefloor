package net.officefloor.hq.app.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One line of a claim: its label, quantity and unit amount, and the sheet's today when it was added. */
@Entity
@Table(name = "claim_line")
public class ClaimLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int qty;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unit;

    @Column(name = "on_date", nullable = false)
    private LocalDate onDate;

    protected ClaimLine() {
    }

    public ClaimLine(Claim claim, String label, int qty, BigDecimal unit, LocalDate onDate) {
        this.sheetId = claim.getSheetId();
        this.claimId = claim.getId();
        this.label = label;
        this.qty = qty;
        this.unit = unit;
        this.onDate = onDate;
    }

    /** A copy of the line for the claim a transfer or clone made of this line's claim on another sheet. Not saved. */
    public ClaimLine copyFor(Claim moved) {
        return new ClaimLine(moved, label, qty, unit, onDate);
    }

    public Long getClaimId() {
        return claimId;
    }

    public LocalDate getOnDate() {
        return onDate;
    }

    /** The quantity times the unit amount. */
    public BigDecimal total() {
        return unit.multiply(BigDecimal.valueOf(qty)).setScale(2);
    }

    /** The line as the screen shows it: <label> <qty> x <unit>. */
    public String line() {
        return label + " " + qty + " x " + unit.setScale(2).toPlainString();
    }
}
