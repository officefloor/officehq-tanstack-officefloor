package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim made by a person on a sheet. Mutable so an Apply step changes it in place. */
public class Claim {

    private final long id;
    private final String sheetId;
    private final long personId;
    private BigDecimal amount;
    private LocalDate date;
    private String note;
    private ClaimStatus status;
    private Integer number;
    private BigDecimal settledDue;
    private String openedBy;
    private boolean stated;
    private BigDecimal statedDue;

    public Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, Integer number, BigDecimal settledDue, String openedBy,
            boolean stated, BigDecimal statedDue) {
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
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public long getPersonId() { return personId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    /** The claim's number on the sheet: null for a draft or a voided claim. */
    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }
    /** The due fixed when the claim was settled: null while it is live (not settled, or settled without a rate). */
    public BigDecimal getSettledDue() { return settledDue; }
    public void setSettledDue(BigDecimal settledDue) { this.settledDue = settledDue; }
    /** The user who opened the claim: null until it is opened. */
    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
    /** Whether the person's last statement bound the claim to {@link #getStatedDue()}. */
    public boolean isStated() { return stated; }
    public void setStated(boolean stated) { this.stated = stated; }
    /** The figure the last statement bound the claim to: null when unbound, or bound without a rate (n/a). */
    public BigDecimal getStatedDue() { return statedDue; }
    public void setStatedDue(BigDecimal statedDue) { this.statedDue = statedDue; }
}
