package net.officefloor.hq.app.claim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

/** A claim of a person: an amount (two decimals, as text), a date (yyyy-MM-dd), a note and a status
 *  (a {@link ClaimStatus} name), and its number on the sheet (given when opened and kept when voided;
 *  none while a draft: the sheet-wide number, or {@code <period>-<n>} while period numbering is in force), its settled due (fixed when settled; none otherwise, or when there was no rate), and
 *  who opened it (none while a draft), and whether its person's last statement bound it, with the
 *  figure it bound (none when it had none): a bound claim shows and totals that figure. A correction (a
 *  negative amount) keeps the number of the SETTLED claim of its person it corrects (none otherwise). */
public class Claim {
    private final long id;
    private final String sheetId;
    private long personId;
    private String amount;
    private String date;
    private String note;
    private String status;
    private String number;
    private String settledDue;
    private String openedBy;
    private boolean stated;
    private String statedDue;
    private String corrects;
    private String roundDate;
    private String dueBy;
    private List<String> log = List.of();
    private String deferredUntil;
    private boolean deferred;
    private int proofs;
    private String splitFrom;
    private boolean reversal;
    private String lockedBy;
    private boolean lateEntry;

    public Claim(long id, String sheetId, long personId, String amount, String date, String note,
            String status, String number, String settledDue, String openedBy) {
        this(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, false, null);
    }

    public Claim(long id, String sheetId, long personId, String amount, String date, String note,
            String status, String number, String settledDue, String openedBy, boolean stated,
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
    public void setPersonId(long personId) { this.personId = personId; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getSettledDue() { return settledDue; }
    /** Whether this change voided a SETTLED claim, reversing its frozen figure (not stored). */
    @JsonIgnore
    public boolean isReversal() { return reversal; }
    public void setReversal(boolean reversal) { this.reversal = reversal; }
    public void setSettledDue(String settledDue) { this.settledDue = settledDue; }

    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }

    public String getCorrects() { return corrects; }
    public void setCorrects(String corrects) { this.corrects = corrects; }

    /** The date its figures round by (see {@link Dues.Late#rounding}): its own date, or for a
     *  correction the date of the claim it corrects. */
    public String getRoundDate() { return roundDate == null ? date : roundDate; }
    public void setRoundDate(String roundDate) { this.roundDate = roundDate; }

    /** Its due-by (yyyy-MM-dd), fixed when it was added: its date then plus the terms of its
     *  person's grade in force on that date. */
    public String getDueBy() { return dueBy; }
    public void setDueBy(String dueBy) { this.dueBy = dueBy; }

    /** Its status log, oldest first (see {@link ClaimLogRepository}). */
    public List<String> getLog() { return log; }
    public void setLog(List<String> log) { this.log = log; }

    /** The date it was last deferred until (yyyy-MM-dd; none: never deferred). */
    public String getDeferredUntil() { return deferredUntil; }

    /** Whether it is deferred on the sheet's today: OPEN and deferred until a later date. A deferred
     *  claim counts in no total, is not overdue, cannot be settled and is left unbound by a statement. */
    public boolean isDeferred() { return deferred; }

    public void setDeferral(String deferredUntil, boolean deferred) {
        this.deferredUntil = deferredUntil;
        this.deferred = deferred;
    }

    /** Defer it until a date after the sheet's today. */
    public void defer(String until) {
        setDeferral(until, true);
    }

    /** How many proofs it has (0 until one is added). */
    public int getProofs() { return proofs; }
    public void setProofs(int proofs) { this.proofs = proofs; }

    /** The number of the claim it was split from (none: not split off a claim). */
    public String getSplitFrom() { return splitFrom; }
    public void setSplitFrom(String splitFrom) { this.splitFrom = splitFrom; }

    /** The user whose open editor locks it for everyone else (none: unlocked). */
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }

    /** Whether it was added while the period its due-by falls in was soft-closed. */
    public boolean isLateEntry() { return lateEntry; }
    public void setLateEntry(boolean lateEntry) { this.lateEntry = lateEntry; }

    public boolean isStated() { return stated; }
    public String getStatedDue() { return statedDue; }

    /** Bind to a figure (null: none), or unbind when not {@code stated}. */
    public void bind(boolean stated, String statedDue) {
        this.stated = stated;
        this.statedDue = stated ? statedDue : null;
    }

    /** Whether it counts toward claimed, due-total and every total: only OPEN, PENDING and SETTLED do. */
    @JsonIgnore
    public boolean isCounting() {
        return ClaimStatus.isOpen(status) || ClaimStatus.SETTLED.name().equals(status);
    }

    /** Whether a statement's figure binds it now: it was bound to one and is not PENDING (a PENDING
     *  claim keeps its binding, but shows and totals its live figure until it is rejected). */
    @JsonIgnore
    public boolean isBound() {
        return stated && statedDue != null && !ClaimStatus.PENDING.name().equals(status);
    }

    /** The due it shows: the one fixed when settled, else the one bound by a statement (not while
     *  PENDING), else {@code live} (the amount at the rate in force on its date; null: none). */
    public String due(String live) {
        if (settledDue != null) return settledDue;
        return stated && !ClaimStatus.PENDING.name().equals(status) ? statedDue : live;
    }

    /** The audit suffix: {@code " number=<n>"} for a claim that has a number. */
    @JsonIgnore
    public String getAuditNumber() {
        return number == null ? "" : " number=" + number;
    }
}
