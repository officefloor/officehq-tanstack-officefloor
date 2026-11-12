package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's claim on a sheet. {@code amount} has two decimals and is 0.00 or more, or negative for
 *  a correction, which names the number of the settled claim of its person it corrects; it starts
 *  as {@link ClaimStatus#DRAFT}. */
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
    private boolean statedNa;
    private String corrects;
    private final LocalDate dueBy;
    private LocalDate deferredUntil;
    private int proofs;
    private String splitFrom;
    private boolean lateEntry;
    private LocalDate remindedOn;
    private boolean escalated;
    private LocalDate reviewOn;
    private String ref;
    private String tags;
    private String voidReason;
    private String editedBy;
    private Long batch;

    private long addedSeq;

    public Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal settledDue, String openedBy, boolean stated,
            BigDecimal statedDue, String corrects, LocalDate dueBy) {
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
        this.corrects = corrects;
        this.dueBy = dueBy;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public long getPersonId() { return personId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public ClaimStatus getStatus() { return status; }
    /** The number given when the claim was opened (the sheet-wide 1, 2, ..., or <period>-<n> / X-<n>
     *  while the sheet numbers by period); null for a draft, or a claim voided as a draft;
     *  a claim voided once open keeps it. */
    public String getNumber() { return number; }
    /** The due fixed when the claim was settled; null when not settled, or settled with no rate. */
    public BigDecimal getSettledDue() { return settledDue; }
    /** The user who opened the claim; null until it is opened. */
    public String getOpenedBy() { return openedBy; }
    /** Whether the person's last statement bound the claim to its figure then. */
    public boolean isStated() { return stated; }
    /** The figure the claim was bound to by the person's last statement; null when it had none (n/a)
     *  or is not bound. */
    public BigDecimal getStatedDue() { return statedDue; }
    /** Whether the person's last statement bound the claim to n/a: the rate that would apply to it had
     *  ended (its to-date) before the claim's date. */
    public boolean isStatedNa() { return statedNa; }
    /** Whether the claim keeps the figure its person's last statement bound it to (a figure, or n/a when
     *  {@link #isStatedNa()}): not while it is PENDING, when it follows its live figure (it is bound
     *  again if rejected back to OPEN). */
    public boolean isBound() { return stated && (statedDue != null || statedNa) && status != ClaimStatus.PENDING; }
    /** The number of the claim this correction corrects; null when it is not a correction. */
    public String getCorrects() { return corrects; }
    /** Whether the claim is a correction: a negative amount, with no floor and no late charge. */
    /** The date the claim is due by, fixed when it was added; later changes to the terms or its date
     *  leave it as it is. */
    public LocalDate getDueBy() { return dueBy; }
    public boolean isCorrection() { return amount.signum() < 0; }
    /** The date the claim was last deferred until; null when it was never deferred. */
    public LocalDate getDeferredUntil() { return deferredUntil; }
    /** Whether the claim is deferred on {@code today}: OPEN and deferred until a date after it. Until
     *  then it counts in no total, is not overdue, cannot be settled and is left unbound by a statement. */
    public boolean isDeferredOn(LocalDate today) {
        return status == ClaimStatus.OPEN && deferredUntil != null && today != null && today.isBefore(deferredUntil);
    }
    /** How many proofs the claim has; a claim due above the sheet's proof threshold on its date cannot be
     *  settled or approved while it has none (see {@link CheckProofs}). */
    public int getProofs() { return proofs; }
    public void setProofs(int proofs) { this.proofs = proofs; }
    /** The number of the claim this claim was split off; null when it was not split off another. */
    public String getSplitFrom() { return splitFrom; }
    public void setSplitFrom(String splitFrom) { this.splitFrom = splitFrom; }
    /** Whether the claim was added while the period its due-by falls in was soft-closed. */
    public boolean isLateEntry() { return lateEntry; }
    public void setLateEntry(boolean lateEntry) { this.lateEntry = lateEntry; }
    /** The last sheet's today a reminder was written for the claim; null when it was never reminded. */
    public LocalDate getRemindedOn() { return remindedOn; }
    public void setRemindedOn(LocalDate remindedOn) { this.remindedOn = remindedOn; }
    /** Whether a reminder ever escalated the claim (it is escalated once only). */
    public boolean isEscalated() { return escalated; }
    public void setEscalated(boolean escalated) { this.escalated = escalated; }
    /** The sheet's today the claim went to REVIEW on (its review day); null when it never went to REVIEW. */
    public LocalDate getReviewOn() { return reviewOn; }
    public void setReviewOn(LocalDate reviewOn) { this.reviewOn = reviewOn; }
    /** The external reference, unique on the sheet without regard to case; null when it has none. */
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }
    /** Its tags (see {@link Tags}): sorted, comma separated; null when it has none. */
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    /** The reason it was voided with, from the sheet's list then; null when not voided, or voided while the list was empty. */
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
    /** The user who last edited the claim (claim form or amount entry); null when it was never edited. */
    public String getEditedBy() { return editedBy; }
    public void setEditedBy(String editedBy) { this.editedBy = editedBy; }
    /** The bulk settle batch it was settled in; null when settled one at a time, or not settled. */
    public Long getBatch() { return batch; }
    public void setBatch(Long batch) { this.batch = batch; }

    /** When it was added, in the order of change_seq (see {@code RateStore}); a copy keeps its original's. 0 for
     *  claims added before forward-only rates. */
    public long getAddedSeq() { return addedSeq; }
    public void setAddedSeq(long addedSeq) { this.addedSeq = addedSeq; }
    public void setDeferredUntil(LocalDate deferredUntil) { this.deferredUntil = deferredUntil; }
    public void setPersonId(long personId) { this.personId = personId; }
    public void setCorrects(String corrects) { this.corrects = corrects; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setNote(String note) { this.note = note; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    public void setNumber(String number) { this.number = number; }
    public void setSettledDue(BigDecimal settledDue) { this.settledDue = settledDue; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
    public void setStated(boolean stated) { this.stated = stated; }
    public void setStatedDue(BigDecimal statedDue) { this.statedDue = statedDue; }
    public void setStatedNa(boolean statedNa) { this.statedNa = statedNa; }
}
