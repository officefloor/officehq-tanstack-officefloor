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

    public Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, Integer number) {
        this.id = id;
        this.sheetId = sheetId;
        this.personId = personId;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.status = status;
        this.number = number;
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
