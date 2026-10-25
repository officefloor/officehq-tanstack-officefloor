package net.officefloor.hq.app.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;
import net.officefloor.hq.app.holiday.BusinessDays;

/** A claim by a person on a sheet: an amount (negative only for a correction, which names the number of the
 * settled claim it corrects), a date, a note (may be empty)
 * a status (DRAFT to start), a number (given when opened), who opened it, a due-by (fixed when created) and a due (fixed when settled). */
@Entity
@Table(name = "claim")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "claim_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String note;

    @Column(nullable = false)
    private String status = ClaimStatus.DRAFT.name();

    /** The number given when the claim was opened (the sheet's next, or the next in its period: P1-1, X-1); null for a
     *  claim never opened; a voided claim keeps it. */
    @Column(name = "claim_number")
    private String number;

    /** The due fixed when the claim was settled; null while it is not settled or its grade had no rate then. */
    @Column(precision = 19, scale = 2)
    private BigDecimal due;

    /** The signed-in name that opened the claim; null while it was never opened. */
    @Column(name = "opened_by")
    private String openedBy;

    /** The signed-in name that last edited the claim (its form or its amount entry); null while it was never edited. */
    @Column(name = "edited_by")
    private String editedBy;

    /** The number of the bulk settle batch that settled it (or left it PENDING); null when settled one at a time. */
    @Column(name = "settle_batch")
    private Integer settleBatch;

    /** Whether the claim is bound by its person's last statement to the figure it had then. */
    @Column(nullable = false)
    private boolean stated;

    /** The figure the claim was bound to by its person's last statement; null while unbound or when it had none (n/a). */
    @Column(name = "stated_due", precision = 19, scale = 2)
    private BigDecimal statedDue;

    /** The claim's due-by: the due-terms-th business day after its due-from, skipping weekends and the sheet's
     *  holidays now on its list; worked out again whenever the holidays change. */
    @Column(name = "due_by", nullable = false)
    private LocalDate dueBy;

    /** The date its due-by counts from, fixed when it was created (its date then). It does not change when its date
     *  changes later. */
    /** Whether a steward set the due-by directly; a due-by set so stays as set and is not worked out again. */
    @Column(name = "due_by_set", nullable = false)
    private boolean dueBySet;

    @Column(name = "due_from", nullable = false)
    private LocalDate dueFrom;

    /** The terms its due-by was fixed with when it was created: its person's own, else their grade's, else the sheet's,
     *  each in force on the day it was created (the sheet's today then). It does not change when the terms change later. */
    @Column(name = "due_terms", nullable = false)
    private int dueTerms;

    /** The number of the settled claim of the same person this correction corrects; null for any other claim. */
    @Column(name = "corrects")
    private String corrects;

    /** The date of the claim this correction corrects (it rounds by that date); null for any other claim. */
    @Column(name = "corrects_date")
    private LocalDate correctsDate;

    /** Where the claim is listed under its person: newest first by this key. The store gives a new claim the next
     *  key, and a moved claim the next again (it is listed as its new person's newest). */
    @Column(nullable = false, insertable = false, updatable = false)
    private Long listed;

    /** The date the claim is deferred until; null for a claim never deferred. */
    @Column(name = "deferred_until")
    private LocalDate deferredUntil;

    /** The last sheet's today a reminder was written for the claim; null for a claim never reminded. */
    @Column(name = "reminded_on")
    private LocalDate remindedOn;

    /** Whether a reminder ever escalated the claim; a claim is escalated once at most. */
    @Column(nullable = false)
    private boolean escalated;

    /** Whether a steward waived the claim's late charge; a waived claim bears no late charge from then on. */
    @Column(nullable = false)
    private boolean waived;

    /** Whether the claim is held (only an OPEN or PENDING claim can be); a held claim still counts in every total. */
    @Column(nullable = false)
    private boolean held;

    /** The sheet's today when the claim last went to REVIEW; null for a claim never in review. */
    @Column(name = "review_on")
    private LocalDate reviewOn;

    /** The sheet's today when the claim last went to PENDING; null for a claim never pending. */
    @Column(name = "pending_on")
    private LocalDate pendingOn;

    /** The user name a steward assigned the claim's review to; null while never assigned. */
    @Column(name = "review_assigned")
    private String reviewAssigned;

    /** How many proofs the claim has: raised by one at a time; 0 for a new claim. */
    @Column(nullable = false)
    private int proofs;

    /** How many of the claim's proofs are photos. 0 for a new claim. */
    @Column(name = "proofs_photo", nullable = false)
    private int proofsPhoto;

    /** How many of the claim's proofs are receipts; a proof of no kind is neither. 0 for a new claim. */
    @Column(name = "proofs_receipt", nullable = false)
    private int proofsReceipt;

    /** The number of the claim this one was split off from; null for a claim not made by a split. */
    @Column(name = "split_from")
    private String splitFrom;

    /** The signed-in name whose open editor locks the claim for everyone else; null while no editor holds it. */
    @Column(name = "locked_by")
    private String lockedBy;

    /** Whether the claim was added while the period its due-by falls in was soft-closed. */
    @Column(name = "late_entry", nullable = false)
    private boolean lateEntry;

    /** The claim's external reference, trimmed; unique on its sheet ignoring case; null for a claim with none. */
    @Column(name = "ref")
    private String ref;

    /** The claim's tags, trimmed, lower-cased, without repeats, sorted and comma separated; null for a claim with none. */
    @Column(name = "tags")
    private String tags;

    /** Every tag the claim has ever carried, sorted and comma separated; null while it never had any. */
    @Column(name = "tags_used")
    private String tagsUsed;

    /** The reason the claim was voided with, from the sheet's list; null when it was voided without one or is not VOID. */
    @Column(name = "void_reason")
    private String voidReason;

    /** The people the claim is shared with, in the order added: <person id>:<share>, comma separated; null for none. */
    @Column(name = "shares", length = 2000)
    private String shares;

    protected Claim() {
    }

    public Claim(String sheetId, Long personId, BigDecimal amount, LocalDate date, String note, String corrects) {
        this.corrects = corrects;
        this.sheetId = sheetId;
        this.personId = personId;
        this.amount = amount;
        this.date = date;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public String getSheetId() {
        return sheetId;
    }

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDueBy() {
        return dueBy;
    }

    public LocalDate getDueFrom() {
        return dueFrom;
    }

    public int getDueTerms() {
        return dueTerms;
    }

    /** Fixes the date and terms the due-by counts from, and works it out for the sheet's holidays. */
    public void fixDue(LocalDate from, int terms, Set<LocalDate> holidays) {
        this.dueFrom = from;
        this.dueTerms = terms;
        reworkDueBy(holidays);
    }

    /** Works the due-by out again for the sheet's holidays now; a due-by a steward set stays as set. */
    public void reworkDueBy(Set<LocalDate> holidays) {
        if (dueBySet) {
            return;
        }
        this.dueBy = BusinessDays.after(dueFrom, dueTerms, holidays);
    }

    /** Sets the due-by directly; it stays as set from then on. */
    public void setDueBy(LocalDate dueBy) {
        this.dueBy = dueBy;
        this.dueBySet = true;
    }

    public boolean isDueBySet() {
        return dueBySet;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public ClaimStatus getStatus() {
        return ClaimStatus.valueOf(status);
    }

    public void setStatus(ClaimStatus status) {
        this.status = status.name();
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public BigDecimal getDue() {
        return due;
    }

    public void setDue(BigDecimal due) {
        this.due = due;
    }

    public String getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(String openedBy) {
        this.openedBy = openedBy;
    }

    public String getEditedBy() {
        return editedBy;
    }

    public void setEditedBy(String editedBy) {
        this.editedBy = editedBy;
    }

    public Integer getSettleBatch() {
        return settleBatch;
    }

    public void setSettleBatch(Integer settleBatch) {
        this.settleBatch = settleBatch;
    }

    public String getCorrects() {
        return corrects;
    }

    public void setCorrects(String corrects) {
        this.corrects = corrects;
    }

    public LocalDate getCorrectsDate() {
        return correctsDate;
    }

    public void setCorrectsDate(LocalDate correctsDate) {
        this.correctsDate = correctsDate;
    }

    /** The date the claim's rounding goes by: a correction's is the date of the claim it corrects, else its own. */
    public LocalDate getRoundsOn() {
        return correctsDate != null ? correctsDate : date;
    }

    /** Whether the claim is a correction: it names a claim it corrects. */
    public boolean isCorrection() {
        return corrects != null;
    }

    public boolean isStated() {
        return stated;
    }

    public BigDecimal getStatedDue() {
        return statedDue;
    }

    /** Whether the claim shows and totals the figure its person's last statement bound it to: it is bound (a PENDING
     *  claim like any other). */
    public boolean isBound() {
        return stated;
    }

    /** Binds the claim to a figure (null: n/a), or unbinds it (live again). */
    public void bind(boolean stated, BigDecimal statedDue) {
        this.stated = stated;
        this.statedDue = stated ? statedDue : null;
    }

    public Long getListed() {
        return listed;
    }

    public LocalDate getDeferredUntil() {
        return deferredUntil;
    }

    public void setDeferredUntil(LocalDate deferredUntil) {
        this.deferredUntil = deferredUntil;
    }

    public LocalDate getRemindedOn() {
        return remindedOn;
    }

    public void setRemindedOn(LocalDate remindedOn) {
        this.remindedOn = remindedOn;
    }

    public LocalDate getReviewOn() {
        return reviewOn;
    }

    public void setReviewOn(LocalDate reviewOn) {
        this.reviewOn = reviewOn;
    }

    public LocalDate getPendingOn() {
        return pendingOn;
    }

    public void setPendingOn(LocalDate pendingOn) {
        this.pendingOn = pendingOn;
    }

    public String getReviewAssigned() {
        return reviewAssigned;
    }

    public void setReviewAssigned(String reviewAssigned) {
        this.reviewAssigned = reviewAssigned;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }

    public boolean isWaived() {
        return waived;
    }

    public void setWaived(boolean waived) {
        this.waived = waived;
    }

    public boolean isHeld() {
        return held;
    }

    public void setHeld(boolean held) {
        this.held = held;
    }

    /** Whether the claim is deferred on a day: it is OPEN and deferred until a date after that day. Until then it counts
     *  in no total, is not overdue, cannot be settled and is left unbound by a statement. */
    public boolean isDeferredOn(LocalDate today) {
        return getStatus() == ClaimStatus.OPEN && deferredUntil != null && deferredUntil.isAfter(today);
    }

    public int getProofs() {
        return proofs;
    }

    /** How many of the claim's proofs are receipts. */
    public int getProofsReceipt() {
        return proofsReceipt;
    }

    /** How many of the claim's proofs are photos. */
    public int getProofsPhoto() {
        return proofsPhoto;
    }

    /** How many of the claim's proofs are of a kind (receipt or photo). */
    public int proofsOf(String kind) {
        return ProofKind.PHOTO.equals(kind) ? getProofsPhoto() : ProofKind.RECEIPT.equals(kind) ? getProofsReceipt() : 0;
    }

    /** Raises the claim's proofs count by one, of a kind (receipt or photo; null for none). */
    public void addProof(String kind) {
        proofs++;
        if (ProofKind.PHOTO.equals(kind)) {
            proofsPhoto++;
        } else if (ProofKind.RECEIPT.equals(kind)) {
            proofsReceipt++;
        }
    }

    public String getSplitFrom() {
        return splitFrom;
    }

    public void setSplitFrom(String splitFrom) {
        this.splitFrom = splitFrom;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    /** Whether someone other than the user holds the claim's editor open. */
    public boolean isLockedFor(String user) {
        return lockedBy != null && !lockedBy.equals(user);
    }

    /** A copy of the claim as it stood at the end of an earlier date: the status it had then, its due fixed then
     *  (null: not settled then) and its binding then. Only worked out, never saved. */
    public Claim asOf(ClaimStatus status, BigDecimal due, boolean stated, BigDecimal statedDue) {
        Claim then = new Claim(sheetId, personId, amount, date, note, corrects);
        then.id = id;
        then.status = status.name();
        then.number = number;
        then.due = due;
        then.openedBy = openedBy;
        then.editedBy = editedBy;
        then.settleBatch = settleBatch;
        then.dueBy = dueBy;
        then.dueBySet = dueBySet;
        then.dueFrom = dueFrom;
        then.dueTerms = dueTerms;
        then.correctsDate = correctsDate;
        then.listed = listed;
        then.deferredUntil = deferredUntil;
        then.remindedOn = remindedOn;
        then.escalated = escalated;
        then.waived = waived;
        then.held = held;
        then.reviewOn = reviewOn;
        then.pendingOn = pendingOn;
        then.reviewAssigned = reviewAssigned;
        then.proofs = proofs;
        then.proofsPhoto = proofsPhoto;
        then.proofsReceipt = proofsReceipt;
        then.splitFrom = splitFrom;
        then.lateEntry = lateEntry;
        then.ref = ref;
        then.tags = tags;
        then.tagsUsed = tagsUsed;
        then.shares = shares;
        then.voidReason = status == ClaimStatus.VOID ? voidReason : null;
        then.bind(stated, statedDue);
        return then;
    }

    /** A copy of the claim for a person on another sheet, as a transfer carries it: its status, dates, note, frozen
     *  due, opener and editor, deferral, proofs and tags; unbound (a statement's figure is lost), with no number (the
     *  other sheet gives it one), no reference, batch, lock or split. Not saved. */
    public Claim transferTo(String toSheetId, Long toPersonId) {
        Claim moved = new Claim(toSheetId, toPersonId, amount, date, note, null);
        moved.status = status;
        moved.due = due;
        moved.openedBy = openedBy;
        moved.editedBy = editedBy;
        moved.dueBy = dueBy;
        moved.dueBySet = dueBySet;
        moved.dueFrom = dueFrom;
        moved.dueTerms = dueTerms;
        moved.correctsDate = correctsDate;
        moved.deferredUntil = deferredUntil;
        moved.remindedOn = remindedOn;
        moved.escalated = escalated;
        moved.waived = waived;
        moved.held = held;
        moved.reviewOn = reviewOn;
        moved.pendingOn = pendingOn;
        moved.reviewAssigned = reviewAssigned;
        moved.proofs = proofs;
        moved.proofsPhoto = proofsPhoto;
        moved.proofsReceipt = proofsReceipt;
        moved.lateEntry = lateEntry;
        moved.tags = tags;
        moved.tagsUsed = tagsUsed;
        moved.voidReason = voidReason;
        return moved;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getTags() {
        return tags;
    }

    /** Gives the claim its tags (null: none), adding them to every tag it has ever carried. */
    public void setTags(String tags) {
        this.tags = tags;
        TreeSet<String> used = new TreeSet<>(Tags.list(tagsUsed));
        used.addAll(Tags.list(tags));
        this.tagsUsed = used.isEmpty() ? null : String.join(",", used);
    }

    public String getTagsUsed() {
        return tagsUsed;
    }

    public boolean isLateEntry() {
        return lateEntry;
    }

    public void setLateEntry(boolean lateEntry) {
        this.lateEntry = lateEntry;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    public String getShares() {
        return shares;
    }

    /** Gives the claim its shares (null: none). */
    public void setShares(String shares) {
        this.shares = shares;
    }
}
