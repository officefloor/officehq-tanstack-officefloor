package net.officefloor.hq.app.claim;

/** A claim of a person: an amount (two decimals, as text), a date (yyyy-MM-dd) and a note. */
public class Claim {
    private final long id;
    private final String sheetId;
    private final long personId;
    private String amount;
    private String date;
    private String note;

    public Claim(long id, String sheetId, long personId, String amount, String date, String note) {
        this.id = id;
        this.sheetId = sheetId;
        this.personId = personId;
        this.amount = amount;
        this.date = date;
        this.note = note;
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
}
