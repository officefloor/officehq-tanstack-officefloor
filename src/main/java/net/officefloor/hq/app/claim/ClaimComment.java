package net.officefloor.hq.app.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** One comment on a claim: its text, who wrote it and the sheet's today when it was written. */
@Entity
@Table(name = "claim_comment")
public class ClaimComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "by_user", nullable = false)
    private String byUser;

    @Column(name = "on_date", nullable = false)
    private LocalDate onDate;

    @Column(nullable = false)
    private String text;

    protected ClaimComment() {
    }

    public ClaimComment(Claim claim, String byUser, LocalDate onDate, String text) {
        this.sheetId = claim.getSheetId();
        this.claimId = claim.getId();
        this.byUser = byUser;
        this.onDate = onDate;
        this.text = text;
    }

    public Long getClaimId() {
        return claimId;
    }

    public LocalDate getOnDate() {
        return onDate;
    }

    public String getText() {
        return text;
    }

    /** The comment as the screen shows it: <user> on <date>: <text>. */
    public String line() {
        return byUser + " on " + onDate + ": " + text;
    }
}
