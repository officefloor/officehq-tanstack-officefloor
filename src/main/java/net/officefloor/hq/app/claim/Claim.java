package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim a person on a sheet has made, DRAFT (and unnumbered) until it is opened. `id` is null until the claim is first saved. */
public class Claim {
    private Long id;
    private final String sheetId;
    private final long personId;
    private BigDecimal amount;
    private LocalDate date;
    private String note;
    private ClaimStatus status;
    private Long number;
    private BigDecimal due;
    private String openedBy;
    private boolean stated;
    private BigDecimal statedDue;

    public Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, Long number, BigDecimal due, String openedBy) {
        this.id = id;
        this.sheetId = sheetId;
        this.personId = personId;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.status = status;
        this.number = number;
        this.due = due;
        this.openedBy = openedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    /** The claim's number on its sheet, given when it is opened; null for a DRAFT, or a claim voided while a DRAFT. A voided claim keeps its number. */
    public Long getNumber() { return number; }
    public void setNumber(Long number) { this.number = number; }
    /** The due fixed when the claim was settled; null when it has none (not settled, or settled with no rate). */
    public BigDecimal getDue() { return due; }
    public void setDue(BigDecimal due) { this.due = due; }
    /** The user who opened the claim; null while it has not been opened. */
    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
    /** Whether the claim is bound by its person's last statement to the figure it had then. */
    public boolean isStated() { return stated; }
    public void setStated(boolean stated) { this.stated = stated; }
    /** The figure the last statement bound the claim to; null when unbound, or bound to n/a. */
    public BigDecimal getStatedDue() { return statedDue; }
    public void setStatedDue(BigDecimal statedDue) { this.statedDue = statedDue; }
}
