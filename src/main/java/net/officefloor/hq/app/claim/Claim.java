package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim by a person on a sheet. Mutable so an Apply step can change it in place. */
public class Claim {

    private final long id;
    private final String sheetId;
    private long personId;
    private BigDecimal amount;
    private LocalDate date;
    private String note;
    private ClaimStatus status;
    private String number;
    private BigDecimal settledDue;
    private String openedBy;
    private boolean stated;
    private BigDecimal statedDue;
    private String corrects;
    private LocalDate correctedDate;
    private LocalDate dueBy;
    private Integer terms;
    private LocalDate deferredUntil;
    private int proofs;
    private String splitFrom;
    private java.util.List<String> log = java.util.List.of();

    public Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal settledDue, String openedBy) {
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

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public long getPersonId() { return personId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public ClaimStatus getStatus() { return status; }
    /** The number given when the claim was opened (sheet-wide, or <period>-<n> / X-<n> within its period); null for a draft (a voided claim keeps the number it had). */
    public String getNumber() { return number; }
    /** The due fixed when the claim was settled; null while its due is live. */
    public BigDecimal getSettledDue() { return settledDue; }
    /** The user who opened the claim; null if it was never opened. */
    public String getOpenedBy() { return openedBy; }
    public void setPersonId(long personId) { this.personId = personId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setNote(String note) { this.note = note; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    public void setNumber(String number) { this.number = number; }
    /** Whether the person's last statement bound this claim to a figure. */
    public boolean isStated() { return stated; }
    /** The figure the last statement bound this claim to; null when unbound or when it was n/a. */
    public BigDecimal getStatedDue() { return statedDue; }
    /** Whether the last statement binds this claim's figure now: a PENDING claim is not bound (it shows its live figure). */
    public boolean isBound() { return stated && status != ClaimStatus.PENDING; }
    public void setStated(boolean stated) { this.stated = stated; }
    public void setStatedDue(BigDecimal statedDue) { this.statedDue = statedDue; }
    public void setSettledDue(BigDecimal settledDue) { this.settledDue = settledDue; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
    /** The user who last edited the claim; null if it was never edited. */
    private String editedBy;
    public String getEditedBy() { return editedBy; }
    public void setEditedBy(String editedBy) { this.editedBy = editedBy; }
    /** The number of the SETTLED claim this correction corrects; null for any claim that is not a correction. */
    public String getCorrects() { return corrects; }
    public void setCorrects(String corrects) { this.corrects = corrects; }
    /** The date of the claim this correction corrects; null when it is not a correction or that claim is not found. */
    public void setCorrectedDate(LocalDate correctedDate) { this.correctedDate = correctedDate; }
    public LocalDate getCorrectedDate() { return correctedDate; }
    /** The date the claim's rounding goes by: a correction's is the date of the claim it corrects, any other claim's its own. */
    public LocalDate getRoundingDate() { return correctedDate != null ? correctedDate : date; }
    /** The claim's due-by, fixed when it was created: its date plus the first of its person's own terms, their grade's terms and the sheet's terms in force on that date. */
    public LocalDate getDueBy() { return dueBy; }
    public void setDueBy(LocalDate dueBy) { this.dueBy = dueBy; }
    /** The terms in business days its due-by was fixed with; null when it has no due-by. */
    public Integer getTerms() { return terms; }
    public void setTerms(Integer terms) { this.terms = terms; }
    /** The date that places the claim in its period: its due-by as fixed at creation (its date when it has none). */
    public LocalDate getPeriodDate() { return dueBy != null ? dueBy : date; }
    /** The date the claim was last deferred until; null if it was never deferred. */
    public LocalDate getDeferredUntil() { return deferredUntil; }
    public void setDeferredUntil(LocalDate deferredUntil) { this.deferredUntil = deferredUntil; }
    /** Whether the claim is deferred on the date: OPEN and deferred until a later date. A deferred claim counts in no
     *  total, is not overdue, cannot be settled and is left unbound by a statement. */
    public boolean isDeferredOn(LocalDate today) {
        return status == ClaimStatus.OPEN && deferredUntil != null && today.isBefore(deferredUntil);
    }
    /** The last day (the sheet's today then) a reminder was written for the claim; null if it was never reminded. */
    private LocalDate remindedOn;
    public LocalDate getRemindedOn() { return remindedOn; }
    public void setRemindedOn(LocalDate remindedOn) { this.remindedOn = remindedOn; }
    /** The claim's proofs count: raised by one at a time. */
    public int getProofs() { return proofs; }
    public void setProofs(int proofs) { this.proofs = proofs; }
    /** The number of the claim this claim was split from; null for any claim a split did not make. */
    public String getSplitFrom() { return splitFrom; }
    public void setSplitFrom(String splitFrom) { this.splitFrom = splitFrom; }

    /** The number of the bulk settle batch that settled the claim; null when it was settled one at a time, or never. */
    private Integer settleBatch;
    public Integer getSettleBatch() { return settleBatch; }
    public void setSettleBatch(Integer settleBatch) { this.settleBatch = settleBatch; }

    /** Whether the claim was added while its period was soft-closed. */
    private boolean lateEntry;
    public boolean isLateEntry() { return lateEntry; }
    public void setLateEntry(boolean lateEntry) { this.lateEntry = lateEntry; }

    /** The claim's external reference; null when it has none. */
    private String ref;
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    /** The claim's tags: lower-cased, sorted and comma separated; null when it has none. */
    private String tags;
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    /** The reason the claim was voided with, from the sheet's list; null when none. */
    private String voidReason;
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }

    /** The user whose open editor locks the claim; null when unlocked. */
    private String lockedBy;
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    /** A correction is a claim with a negative amount: its due has no floor and no late charge. */
    public boolean isCorrection() { return amount.signum() < 0; }
    /** The claim's status log, oldest first: one "<STATUS> by <user> on <date> due=<due>" line per status it has had. */
    public java.util.List<String> getLog() { return log; }
    public void setLog(java.util.List<String> log) { this.log = log; }
    private java.util.List<ClaimComment> comments = java.util.List.of();
    /** The claim's comments, oldest first (empty unless listed). */
    public java.util.List<ClaimComment> getComments() { return comments; }
    public void setComments(java.util.List<ClaimComment> comments) { this.comments = comments; }
    /** The status the claim had at the end of the day, as its status log records it (each line dated the sheet's
     *  today when the change was made); null when it was added after that day. */
    public ClaimStatus statusAt(LocalDate end) {
        ClaimStatus at = null;
        for (String line : log) {
            int on = line.lastIndexOf(" on ");
            int due = line.indexOf(" due=", on);
            if (on < 0 || due < 0) continue;
            if (!LocalDate.parse(line.substring(on + 4, due)).isAfter(end)) {
                at = ClaimStatus.valueOf(line.substring(0, line.indexOf(' ')));
            }
        }
        return at;
    }
}
