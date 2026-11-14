package net.officefloor.hq.app.claim;

/** A claim of a person: an amount (two decimals, as text), a date (yyyy-MM-dd), a note and a status
 *  (a {@link ClaimStatus} name). */
public class Claim {
    private final long id;
    private final String sheetId;
    private final long personId;
    private String amount;
    private String date;
    private String note;
    private String status;

    public Claim(long id, String sheetId, long personId, String amount, String date, String note,
            String status) {
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
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
