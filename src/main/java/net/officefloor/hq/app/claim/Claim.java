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

    public Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
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
    /** The claim's number on its sheet, given when it is opened; null for a DRAFT or VOID claim. */
    public Long getNumber() { return number; }
    public void setNumber(Long number) { this.number = number; }
}
