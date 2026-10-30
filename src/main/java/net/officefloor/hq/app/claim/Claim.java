package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim by a person on a sheet. Mutable so an Apply step can change it in place. */
public class Claim {

    private final long id;
    private final String sheetId;
    private final long personId;
    private BigDecimal amount;
    private LocalDate date;
    private String note;
    private ClaimStatus status;
    private Long number;

    public Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, Long number) {
        this.id = id;
        this.sheetId = sheetId;
        this.personId = personId;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.status = status;
        this.number = number;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public long getPersonId() { return personId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public ClaimStatus getStatus() { return status; }
    /** The number given when the claim was opened; null for a draft or a voided claim. */
    public Long getNumber() { return number; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setNote(String note) { this.note = note; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    public void setNumber(Long number) { this.number = number; }
}
