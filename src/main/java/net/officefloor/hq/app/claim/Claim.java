package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's claim, as the store holds it. {@code id} is null until first saved. */
public class Claim {
    private Long id;
    private final String sheetId;
    private final long personId;
    private BigDecimal amount;
    private LocalDate date;
    private String note;
    private ClaimStatus status;
    private Integer number;
    private Integer voidedNumber;
    private BigDecimal settledDue;
    private String openedBy;
    private boolean stated;
    private BigDecimal statedDue;

    public Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, Integer number, BigDecimal settledDue,
            String openedBy) {
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
    /** The claim's number on its sheet, given when it is opened; null for a draft or a voided claim. */
    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }
    /** The due fixed when it was settled; null while its due is live. */
    public BigDecimal getSettledDue() { return settledDue; }
    public void setSettledDue(BigDecimal settledDue) { this.settledDue = settledDue; }
    /** The user who opened it; null until it is opened. */
    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
    /** Whether its person's last statement bound it to a figure. */
    public boolean isStated() { return stated; }
    /** The figure its person's last statement bound it to; null for n/a, or when it is not stated. */
    public BigDecimal getStatedDue() { return statedDue; }
    /** Bind it to a figure (null: n/a), or leave it unbound (live) when {@code stated} is false. */
    public void bind(boolean stated, BigDecimal statedDue) {
        this.stated = stated;
        this.statedDue = stated ? statedDue : null;
    }
    /** The number its audit lines carry: its number, or the one it had before it was voided. */
    public Integer getAuditNumber() { return number != null ? number : voidedNumber; }

    /** Move to the next status, when the current one allows it. */
    public void moveTo(ClaimStatus next) throws InvalidClaimStatusException {
        if (!status.canMoveTo(next)) throw new InvalidClaimStatusException(status, next);
        status = next;
        if (next == ClaimStatus.VOID) {
            voidedNumber = number;
            number = null;
        }
    }
}
