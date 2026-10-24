package net.officefloor.hq.app.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One line of a claim's status log: a status it entered, who made the change, the sheet's today then and the
 * claim's due as it was worked out at that moment (null: n/a). */
@Entity
@Table(name = "claim_log")
public class ClaimLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(nullable = false)
    private String status;

    @Column(name = "by_user", nullable = false)
    private String byUser;

    @Column(name = "on_date", nullable = false)
    private LocalDate onDate;

    @Column(precision = 19, scale = 2)
    private BigDecimal due;

    protected ClaimLog() {
    }

    public ClaimLog(Claim claim, String byUser, LocalDate onDate, BigDecimal due) {
        this.sheetId = claim.getSheetId();
        this.claimId = claim.getId();
        this.status = claim.getStatus().name();
        this.byUser = byUser;
        this.onDate = onDate;
        this.due = due;
    }

    /** A copy of the line for the claim a transfer made of this line's claim on another sheet. Not saved. */
    public ClaimLog copyFor(Claim moved) {
        ClaimLog copy = new ClaimLog();
        copy.sheetId = moved.getSheetId();
        copy.claimId = moved.getId();
        copy.status = status;
        copy.byUser = byUser;
        copy.onDate = onDate;
        copy.due = due;
        return copy;
    }

    public Long getClaimId() {
        return claimId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getOnDate() {
        return onDate;
    }

    /** The claim's due as it was worked out then (null: n/a); a SETTLED line's is the due it was settled at. */
    public BigDecimal getDue() {
        return due;
    }

    /** The line as the screen shows it: <STATUS> by <user> on <date> due=<due>. */
    public String line() {
        return status + " by " + byUser + " on " + onDate + " due=" + (due == null ? "n/a" : due.setScale(2).toPlainString());
    }
}
