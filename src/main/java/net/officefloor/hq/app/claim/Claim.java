package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's claim on a sheet. {@code amount} has two decimals and is 0.00 or more; it starts
 *  as {@link ClaimStatus#DRAFT}. */
public class Claim {
    private final long id;
    private final String sheetId;
    private final long personId;
    private BigDecimal amount;
    private LocalDate date;
    private String note;
    private ClaimStatus status;

    public Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status) {
        this.id = id;
        this.sheetId = sheetId;
        this.personId = personId;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.status = status;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public long getPersonId() { return personId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public ClaimStatus getStatus() { return status; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setNote(String note) { this.note = note; }
    public void setStatus(ClaimStatus status) { this.status = status; }
}
