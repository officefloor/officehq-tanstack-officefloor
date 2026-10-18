package net.officefloor.hq.app.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim a person on a sheet has made: an amount (negative only for a correction, which names the settled claim it corrects), a date, a note (may be empty) and a
 *  status (starting DRAFT), once opened, its number on the sheet and, once settled, its fixed due; once opened, who opened it. */
@Entity
@Table(name = "claim")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "claim_date", nullable = false)
    private LocalDate date;

    @Column(name = "note", nullable = false)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClaimStatus status = ClaimStatus.DRAFT;

    /** Given when opened, the next on its sheet; none while a draft, kept when voided. */
    @Column(name = "claim_number")
    private Integer number;

    /** Given instead of a number when opened while period numbering is on: <period name>-<n>, or X-<n>
     *  for a claim dated in no period; none otherwise, kept when voided. */
    @Column(name = "period_number")
    private String periodNumber;

    /** Fixed when settled (amount × rate / 100, two decimals); none while live or when no rate applied. */
    @Column(name = "settled_due", precision = 14, scale = 2)
    private BigDecimal settledDue;

    /** The signed-in user who opened it; none while a draft. */
    @Column(name = "opened_by")
    private String openedBy;

    /** For a correction (a negative amount), the number of the SETTLED claim of the same person it corrects; none otherwise. */
    @Column(name = "corrects")
    private Integer corrects;

    protected Claim() {
    }

    public Claim(String sheetId, Long personId) {
        this.sheetId = sheetId;
        this.personId = personId;
    }

    public Long getId() {
        return id;
    }

    public String getSheetId() {
        return sheetId;
    }

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getPeriodNumber() {
        return periodNumber;
    }

    public void setPeriodNumber(String periodNumber) {
        this.periodNumber = periodNumber;
    }

    /** The number as shown: its period number when it has one, else its sheet-wide number; null for none. */
    public String getShownNumber() {
        return periodNumber != null ? periodNumber : number == null ? null : String.valueOf(number);
    }

    public BigDecimal getSettledDue() {
        return settledDue;
    }

    public void setSettledDue(BigDecimal settledDue) {
        this.settledDue = settledDue;
    }

    public Integer getCorrects() {
        return corrects;
    }

    public void setCorrects(Integer corrects) {
        this.corrects = corrects;
    }

    /** Whether this claim is a correction: its amount is negative. */
    public boolean isCorrection() {
        return amount != null && amount.signum() < 0;
    }

    public String getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(String openedBy) {
        this.openedBy = openedBy;
    }
}
