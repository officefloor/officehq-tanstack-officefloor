package net.officefloor.hq.app.claim;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** A claim of a person: an amount (two decimals, as text), a date (yyyy-MM-dd), a note and a status
 *  (a {@link ClaimStatus} name), and its number on the sheet (given when opened; none while a draft or
 *  once voided). */
public class Claim {
    private final long id;
    private final String sheetId;
    private final long personId;
    private String amount;
    private String date;
    private String note;
    private String status;
    private Long number;
    /** The number a claim had before this request cleared it (a void), for its audit line. */
    private Long numberHad;

    public Claim(long id, String sheetId, long personId, String amount, String date, String note,
            String status, Long number) {
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
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getNumber() { return number; }
    public void setNumber(Long number) { this.number = number; }

    /** Clear the number, keeping the one it had for the audit line. */
    public void clearNumber() {
        this.numberHad = number;
        this.number = null;
    }

    /** The audit suffix: {@code " number=<n>"} for a claim that has (or just had) a number. */
    @JsonIgnore
    public String getAuditNumber() {
        Long n = number != null ? number : numberHad;
        return n == null ? "" : " number=" + n;
    }
}
