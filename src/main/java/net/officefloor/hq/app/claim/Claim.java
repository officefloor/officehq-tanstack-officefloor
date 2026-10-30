package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim by a person on a sheet. Mutable so an Apply step can change it in place. */
public class Claim {

    private final long id;
    private final String sheetId;
    private long personId;
    private BigDecimal amount;
    private LocalDate date;
    private String note;
    private ClaimStatus status;
    private String number;
    private BigDecimal settledDue;
    private String openedBy;
    private boolean stated;
    private BigDecimal statedDue;
    private String corrects;
    private LocalDate correctedDate;
    private LocalDate dueBy;
    private java.util.List<String> log = java.util.List.of();

    public Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal settledDue, String openedBy) {
        this.id = id;
        this.sheetId = sheetId;
        this.personId = personId;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.status = status;
        this.number = number;
        this.settledDue = settledDue;
        this.openedBy = openedBy;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public long getPersonId() { return personId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public ClaimStatus getStatus() { return status; }
    /** The number given when the claim was opened (sheet-wide, or <period>-<n> / X-<n> within its period); null for a draft (a voided claim keeps the number it had). */
    public String getNumber() { return number; }
    /** The due fixed when the claim was settled; null while its due is live. */
    public BigDecimal getSettledDue() { return settledDue; }
    /** The user who opened the claim; null if it was never opened. */
    public String getOpenedBy() { return openedBy; }
    public void setPersonId(long personId) { this.personId = personId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setNote(String note) { this.note = note; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    public void setNumber(String number) { this.number = number; }
    /** Whether the person's last statement bound this claim to a figure. */
    public boolean isStated() { return stated; }
    /** The figure the last statement bound this claim to; null when unbound or when it was n/a. */
    public BigDecimal getStatedDue() { return statedDue; }
    /** Whether the last statement binds this claim's figure now: a PENDING claim is not bound (it shows its live figure). */
    public boolean isBound() { return stated && status != ClaimStatus.PENDING; }
    public void setStated(boolean stated) { this.stated = stated; }
    public void setStatedDue(BigDecimal statedDue) { this.statedDue = statedDue; }
    public void setSettledDue(BigDecimal settledDue) { this.settledDue = settledDue; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
    /** The number of the SETTLED claim this correction corrects; null for any claim that is not a correction. */
    public String getCorrects() { return corrects; }
    public void setCorrects(String corrects) { this.corrects = corrects; }
    /** The date of the claim this correction corrects; null when it is not a correction or that claim is not found. */
    public void setCorrectedDate(LocalDate correctedDate) { this.correctedDate = correctedDate; }
    /** The date the claim's rounding goes by: a correction's is the date of the claim it corrects, any other claim's its own. */
    public LocalDate getRoundingDate() { return correctedDate != null ? correctedDate : date; }
    /** The claim's due-by, fixed when it was created: its date plus the terms of its person's grade in force on that date. */
    public LocalDate getDueBy() { return dueBy; }
    public void setDueBy(LocalDate dueBy) { this.dueBy = dueBy; }
    /** A correction is a claim with a negative amount: its due has no floor and no late charge. */
    public boolean isCorrection() { return amount.signum() < 0; }
    /** The claim's status log, oldest first: one "<STATUS> by <user> on <date> due=<due>" line per status it has had. */
    public java.util.List<String> getLog() { return log; }
    public void setLog(java.util.List<String> log) { this.log = log; }
}
