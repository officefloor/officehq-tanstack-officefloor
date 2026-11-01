package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim a person on a sheet has made, DRAFT (and unnumbered) until it is opened. `id` is null until the claim is first saved. */
public class Claim {
    private Long id;
    private final String sheetId;
    private long personId;
    private BigDecimal amount;
    private LocalDate date;
    private String note;
    private ClaimStatus status;
    private String number;
    private BigDecimal due;
    private String openedBy;
    private boolean stated;
    private BigDecimal statedDue;
    private String corrects;
    private LocalDate dueBy;
    private LocalDate deferredUntil;
    private boolean deferred;
    private int proofs;
    private int proofPhotos;
    private String splitFrom;
    private String lockedBy;
    private boolean lateEntry;
    private LocalDate remindedOn;
    private boolean escalated;
    private boolean escalatedNow;
    private boolean waived;
    private boolean held;
    private String ref;
    private String tags;
    private String tagsUsed;
    private String voidReason;
    private String editedBy;
    private Integer dueTerms;
    private Integer batch;
    private Long addedSeq;

    public Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal due, String openedBy) {
        this.id = id;
        this.sheetId = sheetId;
        this.personId = personId;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.status = status;
        this.number = number;
        this.due = due;
        this.openedBy = openedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSheetId() { return sheetId; }
    public long getPersonId() { return personId; }
    public void setPersonId(long personId) { this.personId = personId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    /** The claim's number on its sheet, given when it is opened (sheet-wide "7", or within its period "P1-2" / "X-1"); null for a DRAFT, or a claim voided while a DRAFT. A voided claim keeps its number. */
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    /** The due fixed when the claim was settled; null when it has none (not settled, or settled with no rate). */
    public BigDecimal getDue() { return due; }
    public void setDue(BigDecimal due) { this.due = due; }
    /** The user who opened the claim; null while it has not been opened. */
    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
    /** Whether the claim is bound by its person's last statement to the figure it had then. */
    public boolean isStated() { return stated; }
    public void setStated(boolean stated) { this.stated = stated; }
    /** The figure the last statement bound the claim to; null when unbound, or bound to n/a. */
    public BigDecimal getStatedDue() { return statedDue; }
    public void setStatedDue(BigDecimal statedDue) { this.statedDue = statedDue; }
    /** The number of the SETTLED claim of the same person this correction corrects; null when it is not a correction. */
    public String getCorrects() { return corrects; }
    public void setCorrects(String corrects) { this.corrects = corrects; }
    /** The date the claim is due by, fixed when it was added: its date then plus the terms of its
     *  person's grade in force on that date. Later changes to the terms or the claim's date leave it;
     *  a holiday added on or before it re-fixes it (unless SETTLED or VOID) from its date, with the same terms. */
    public LocalDate getDueBy() { return dueBy; }
    public void setDueBy(LocalDate dueBy) { this.dueBy = dueBy; }
    /** The terms (business days) the claim's due-by was fixed with; a holiday added re-fixes it with these. */
    public Integer getDueTerms() { return dueTerms; }
    public void setDueTerms(Integer dueTerms) { this.dueTerms = dueTerms; }
    /** The date the claim was last deferred until; null when it never was. */
    public LocalDate getDeferredUntil() { return deferredUntil; }
    public void setDeferredUntil(LocalDate deferredUntil) { this.deferredUntil = deferredUntil; }
    /** Whether the claim is deferred now: the sheet's today (as it was when the claim was read) is
     *  before its deferred-until date (only an OPEN claim is deferred, and a voided one keeps showing
     *  it). A deferred claim counts in no total, is not overdue, cannot be settled and is left
     *  unbound by a statement. */
    public boolean isDeferred() { return deferred; }
    public void setDeferred(boolean deferred) { this.deferred = deferred; }
    /** How many proofs the claim has: a claim whose due is above the sheet's proof threshold on its
     *  date cannot be settled or approved while it has none. */
    public int getProofs() { return proofs; }
    public void setProofs(int proofs) { this.proofs = proofs; }
    /** How many of the claim's proofs are photos; the rest are receipts. */
    public int getProofPhotos() { return proofPhotos; }
    public void setProofPhotos(int proofPhotos) { this.proofPhotos = proofPhotos; }
    /** How many of the claim's proofs are of this kind (receipt or photo). */
    public int proofsOf(String kind) {
        return ProofKind.PHOTO.equals(kind) ? proofPhotos : ProofKind.RECEIPT.equals(kind) ? proofs - proofPhotos : 0;
    }
    /** The number of the claim this one was split off; null when it was not split off one. */
    public String getSplitFrom() { return splitFrom; }
    public void setSplitFrom(String splitFrom) { this.splitFrom = splitFrom; }
    /** The user whose open editor locks the claim for everyone else; null when no editor holds it. */
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    /** Whether the claim was added while its period was soft-closed. */
    public boolean isLateEntry() { return lateEntry; }
    public void setLateEntry(boolean lateEntry) { this.lateEntry = lateEntry; }
    /** The last day (sheet-today) a reminder was written for the claim; null when it never was. */
    public LocalDate getRemindedOn() { return remindedOn; }
    public void setRemindedOn(LocalDate remindedOn) { this.remindedOn = remindedOn; }
    /** Whether the claim has been escalated by a reminder: at most once, ever. */
    public boolean isEscalated() { return escalated; }
    public void setEscalated(boolean escalated) { this.escalated = escalated; }
    /** Whether the reminder being taken now escalates the claim (not stored). */
    public boolean isEscalatedNow() { return escalatedNow; }
    public void setEscalatedNow(boolean escalatedNow) { this.escalatedNow = escalatedNow; }
    /** Whether a steward has waived the claim's late charge: it bears none from then on, whatever its overdue state. */
    public boolean isWaived() { return waived; }
    public void setWaived(boolean waived) { this.waived = waived; }
    /** Whether the claim is held: shown only; it still counts in every total. */
    public boolean isHeld() { return held; }
    public void setHeld(boolean held) { this.held = held; }
    /** The claim's external reference, unique on its sheet without regard to case; null when it has none. */
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }
    /** The claim's tags, lower-cased, sorted and comma separated; null when it has none. */
    public String getTags() { return tags; }
    /** Sets the claim's tags, adding them to the tags it has ever carried. */
    public void setTags(String tags) {
        this.tags = tags;
        this.tagsUsed = ClaimRequest.union(tagsUsed, tags);
    }
    /** Every tag the claim has ever carried, in the same form as its tags; null when none. */
    public String getTagsUsed() { return tagsUsed; }
    public void setTagsUsed(String tagsUsed) { this.tagsUsed = tagsUsed; }
    /** The reason from its sheet's list the claim was voided with; null when none was given. */
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
    /** Who last edited the claim (its form or its row's amount entry); null while never edited. */
    public String getEditedBy() { return editedBy; }
    public void setEditedBy(String editedBy) { this.editedBy = editedBy; }
    /** The number of the bulk settle that settled the claim; null when none did. */
    public Integer getBatch() { return batch; }
    public void setBatch(Integer batch) { this.batch = batch; }
    /** The sequence the claim was added in, across every sheet (a clone keeps the claim's own); null for its id. */
    public Long getAddedSeq() { return addedSeq; }
    public void setAddedSeq(Long addedSeq) { this.addedSeq = addedSeq; }
    /** The order the claim was added in, for which rates apply to it (see {@link net.officefloor.hq.app.rate.Rates#pct}):
     *  its added sequence, else its id; null for a claim not yet added. */
    public Long seq() { return addedSeq != null ? addedSeq : id; }
    /** Whether the claim is a correction: its amount is negative. Its due has no floor and no late charge. */
    public boolean isCorrection() { return amount.signum() < 0; }
}
