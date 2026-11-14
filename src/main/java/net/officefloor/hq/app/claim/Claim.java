package net.officefloor.hq.app.claim;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** A claim of a person: an amount (two decimals, as text), a date (yyyy-MM-dd), a note and a status
 *  (a {@link ClaimStatus} name), and its number on the sheet (given when opened and kept when voided;
 *  none while a draft), its settled due (fixed when settled; none otherwise, or when there was no rate), and
 *  who opened it (none while a draft), and whether its person's last statement bound it, with the
 *  figure it bound (none when it had none): a bound claim shows and totals that figure. A correction (a
 *  negative amount) keeps the number of the SETTLED claim of its person it corrects (none otherwise). */
public class Claim {
    private final long id;
    private final String sheetId;
    private final long personId;
    private String amount;
    private String date;
    private String note;
    private String status;
    private Long number;
    private String settledDue;
    private String openedBy;
    private boolean stated;
    private String statedDue;
    private Long corrects;

    public Claim(long id, String sheetId, long personId, String amount, String date, String note,
            String status, Long number, String settledDue, String openedBy) {
        this(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, false, null);
    }

    public Claim(long id, String sheetId, long personId, String amount, String date, String note,
            String status, Long number, String settledDue, String openedBy, boolean stated,
            String statedDue) {
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
    public String getSettledDue() { return settledDue; }
    public void setSettledDue(String settledDue) { this.settledDue = settledDue; }

    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }

    public Long getCorrects() { return corrects; }
    public void setCorrects(Long corrects) { this.corrects = corrects; }

    public boolean isStated() { return stated; }
    public String getStatedDue() { return statedDue; }

    /** Bind to a figure (null: none), or unbind when not {@code stated}. */
    public void bind(boolean stated, String statedDue) {
        this.stated = stated;
        this.statedDue = stated ? statedDue : null;
    }

    /** Whether it counts toward claimed, due-total and every total: only OPEN and SETTLED do. */
    @JsonIgnore
    public boolean isCounting() {
        return ClaimStatus.OPEN.name().equals(status) || ClaimStatus.SETTLED.name().equals(status);
    }

    /** The due it shows: the one fixed when settled, else the one bound by a statement, else
     *  {@code live} (the amount at the rate in force on its date; null: none). */
    public String due(String live) {
        if (settledDue != null) return settledDue;
        return stated ? statedDue : live;
    }

    /** The audit suffix: {@code " number=<n>"} for a claim that has a number. */
    @JsonIgnore
    public String getAuditNumber() {
        return number == null ? "" : " number=" + number;
    }
}
