package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's claim on a sheet. {@code amount} has two decimals and is 0.00 or more, or negative for
 *  a correction, which names the number of the settled claim of its person it corrects; it starts
 *  as {@link ClaimStatus#DRAFT}. */
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

    public Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal settledDue, String openedBy, boolean stated,
            BigDecimal statedDue, String corrects) {
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
        this.stated = stated;
        this.statedDue = statedDue;
        this.corrects = corrects;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public long getPersonId() { return personId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public ClaimStatus getStatus() { return status; }
    /** The number given when the claim was opened (the sheet-wide 1, 2, ..., or <period>-<n> / X-<n>
     *  while the sheet numbers by period); null for a draft, or a claim voided as a draft;
     *  a claim voided once open keeps it. */
    public String getNumber() { return number; }
    /** The due fixed when the claim was settled; null when not settled, or settled with no rate. */
    public BigDecimal getSettledDue() { return settledDue; }
    /** The user who opened the claim; null until it is opened. */
    public String getOpenedBy() { return openedBy; }
    /** Whether the person's last statement bound the claim to its figure then. */
    public boolean isStated() { return stated; }
    /** The figure the claim was bound to by the person's last statement; null when it had none (n/a)
     *  or is not bound. */
    public BigDecimal getStatedDue() { return statedDue; }
    /** Whether the claim keeps the figure its person's last statement bound it to: not while it is
     *  PENDING, when it follows its live figure (it is bound again if rejected back to OPEN). */
    public boolean isBound() { return stated && statedDue != null && status != ClaimStatus.PENDING; }
    /** The number of the claim this correction corrects; null when it is not a correction. */
    public String getCorrects() { return corrects; }
    /** Whether the claim is a correction: a negative amount, with no floor and no late charge. */
    public boolean isCorrection() { return amount.signum() < 0; }
    public void setPersonId(long personId) { this.personId = personId; }
    public void setCorrects(String corrects) { this.corrects = corrects; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setNote(String note) { this.note = note; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    public void setNumber(String number) { this.number = number; }
    public void setSettledDue(BigDecimal settledDue) { this.settledDue = settledDue; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
    public void setStated(boolean stated) { this.stated = stated; }
    public void setStatedDue(BigDecimal statedDue) { this.statedDue = statedDue; }
}
