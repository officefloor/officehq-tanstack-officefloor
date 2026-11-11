package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's claim, as the store holds it. {@code id} is null until first saved. */
public class Claim {
    private Long id;
    private final String sheetId;
    private long personId;
    private BigDecimal amount;
    private LocalDate date;
    private String note;
    private ClaimStatus status;
    private String number;
    private BigDecimal settledDue;
    private String openedBy;
    private String editedBy;
    private boolean stated;
    private BigDecimal statedDue;
    private BigDecimal statedLate;
    private String corrects;
    private LocalDate correctedDate;
    private boolean relisted;
    private LocalDate dueBy;
    private int terms;
    private boolean dueBySet;
    private LocalDate deferredUntil;
    private int proofs;
    private int proofPhotos;
    private String splitFrom;
    private String lockedBy;
    private boolean lateEntry;
    private LocalDate remindedOn;
    private boolean waived;
    private boolean held;
    private String reviewUser;
    private LocalDate reviewOn;
    /** The sheet's today when it last went to PENDING (null: never). */
    private LocalDate pendingOn;
    private String ref;
    private String tags;
    private String voidReason;
    private Integer settleBatch;
    private LocalDate settledOn;

    public Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal settledDue,
            String openedBy) {
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSheetId() { return sheetId; }
    public long getPersonId() { return personId; }
    /**
     * Move it to another person: it keeps its date (its new number is given by {@link RenumberClaim}), but loses any figure a statement
     * bound it to (it is live again until its new person's next statement). A SETTLED, VOID or DELETED claim
     * cannot move.
     */
    public void reassign(long toPersonId) throws InvalidClaimStatusException {
        if (status == ClaimStatus.SETTLED || status == ClaimStatus.VOID) {
            throw new InvalidClaimStatusException("A " + status + " claim cannot be moved to another person");
        }
        if (status == ClaimStatus.DELETED) {
            throw new InvalidClaimStatusException("A DELETED claim cannot be moved to another person");
        }
        personId = toPersonId;
        bind(false, null);
        relisted = true;
    }
    /**
     * Move it, whatever its status, with the rest of its person's claims to the person they were merged
     * into: it keeps its number and date, and any figure a statement bound it to, and is listed again.
     */
    public void mergeTo(long toPersonId) {
        personId = toPersonId;
        relisted = true;
    }
    /** Whether it was moved since read, so it is listed again as its new person's newest. */
    public boolean isRelisted() { return relisted; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    /** Its due-by, fixed when it was made: the terms-th business day after its date (skipping weekends and the sheet's holidays then), its terms those in force on the day it was made (the sheet's today then): its person's own, else those of the grade they had that day, else the sheet's. Kept when its date changes; fixed again with the same terms when a holiday on or before it is added while it is not SETTLED or VOID. */
    public LocalDate getDueBy() { return dueBy; }
    public void setDueBy(LocalDate dueBy) { this.dueBy = dueBy; }
    /** Whether a steward set its due-by directly: it stays as set, a holiday added later does not move it. */
    public boolean isDueBySet() { return dueBySet; }
    public void setDueBySet(boolean dueBySet) { this.dueBySet = dueBySet; }
    /** The terms (business days) its due-by was fixed with. */
    public int getTerms() { return terms; }
    public void setTerms(int terms) { this.terms = terms; }
    /** Fix its due-by: the {@code terms}-th business day after its date, skipping weekends and {@code holidays}. */
    public void fixDueBy(int terms, java.util.Set<LocalDate> holidays) {
        this.terms = terms;
        this.dueBy = net.officefloor.hq.app.holiday.HolidayRepository.businessDaysAfter(date, terms, holidays);
    }
    /** The date it is deferred until; null when it was never deferred. */
    public LocalDate getDeferredUntil() { return deferredUntil; }
    public void setDeferredUntil(LocalDate deferredUntil) { this.deferredUntil = deferredUntil; }
    /**
     * Whether it is deferred on {@code today}: not bound by a statement, and deferred until a date
     * after today. From that date on it is a claim like any other. While a statement binds it, its
     * deferral waits until the binding ends, and then takes effect whatever its status since (one
     * settled while bound drops out of the totals until that date too).
     */
    public boolean isDeferredOn(LocalDate today) {
        return !isBound() && deferredUntil != null && today != null && today.isBefore(deferredUntil);
    }
    /**
     * Defer it until a date after {@code today}: only an OPEN claim may be deferred.
     */
    public void defer(LocalDate until, LocalDate today) throws InvalidClaimStatusException, InvalidClaimException {
        if (status != ClaimStatus.OPEN) {
            throw new InvalidClaimStatusException("A " + status + " claim cannot be deferred");
        }
        if (!until.isAfter(today)) throw new InvalidClaimException("A claim is deferred to a date after today");
        deferredUntil = until;
    }
    /** How many proofs it has had added. */
    public int getProofs() { return proofs; }
    public void setProofs(int proofs) { this.proofs = proofs; }
    /** How many of its proofs are photos. */
    public int getProofPhotos() { return proofPhotos; }
    public void setProofPhotos(int proofPhotos) { this.proofPhotos = proofPhotos; }
    /** How many of its proofs are receipts: those that are not photos. */
    public int getProofReceipts() { return proofs - proofPhotos; }
    /** How many of its proofs are of a kind (receipt or photo). */
    public int proofsOf(String kind) {
        return ProofKind.PHOTO.equals(kind) ? getProofPhotos() : ProofKind.RECEIPT.equals(kind) ? getProofReceipts() : 0;
    }
    /** Add a proof of a kind (null: a receipt): not to a SETTLED or VOID claim. */
    public void addProof(String kind) throws InvalidClaimStatusException {
        if (status == ClaimStatus.SETTLED || status == ClaimStatus.VOID || status == ClaimStatus.DELETED) {
            throw new InvalidClaimStatusException("A " + status + " claim cannot have a proof added");
        }
        proofs++;
        if (ProofKind.PHOTO.equals(kind)) proofPhotos++;
    }
    /** The last sheet's today a reminder was written for it; null when it was never reminded. */
    public LocalDate getRemindedOn() { return remindedOn; }
    public void setRemindedOn(LocalDate remindedOn) { this.remindedOn = remindedOn; }
    /** Whether a steward waived its late charge: it bears none from then on. */
    public boolean isWaived() { return waived; }
    public void setWaived(boolean waived) { this.waived = waived; }
    /** Waive its late charge: only an OPEN or PENDING claim, and only once. */
    public void waive() throws InvalidClaimStatusException {
        if (!isOpen()) throw new InvalidClaimStatusException("A " + status + " claim cannot have its late charge waived");
        if (waived) throw new InvalidClaimStatusException("The claim's late charge was already waived");
        waived = true;
    }
    /** Whether it is held: it still counts in every total. */
    public boolean isHeld() { return held; }
    public void setHeld(boolean held) { this.held = held; }
    /** The user a steward assigned it to review while in REVIEW; null when none. Kept once it leaves REVIEW. */
    public String getReviewUser() { return reviewUser; }
    public void setReviewUser(String reviewUser) { this.reviewUser = reviewUser; }
    /** Hold it, or release it when held: only an OPEN or PENDING claim. */
    public void toggleHold() throws InvalidClaimStatusException {
        if (!isOpen()) throw new InvalidClaimStatusException("A " + status + " claim cannot be held or released");
        held = !held;
    }
    /** The sheet's today when it went to REVIEW (null: it never did). */
    public LocalDate getReviewOn() { return reviewOn; }
    public void setReviewOn(LocalDate reviewOn) { this.reviewOn = reviewOn; }
    public LocalDate getPendingOn() { return pendingOn; }
    public void setPendingOn(LocalDate pendingOn) { this.pendingOn = pendingOn; }
    /** Remind it on {@code today}: only an OPEN or PENDING claim, and once per sheet's today. */
    public void remind(LocalDate today) throws InvalidClaimStatusException {
        if (!isOpen()) throw new InvalidClaimStatusException("A " + status + " claim cannot be reminded");
        if (today.equals(remindedOn)) throw new InvalidClaimStatusException("The claim was already reminded today");
        remindedOn = today;
    }
    /** Whether it was added while its period was soft-closed. */
    public boolean isLateEntry() { return lateEntry; }
    public void setLateEntry(boolean lateEntry) { this.lateEntry = lateEntry; }
    /** The number of the claim it was split off; null when it was not split off one. */
    public String getSplitFrom() { return splitFrom; }
    public void setSplitFrom(String splitFrom) { this.splitFrom = splitFrom; }
    /**
     * Split {@code part} (more than 0.00, less than its amount) off an OPEN claim that is not
     * deferred on {@code today}: it keeps the rest and loses any figure a statement bound it to.
     * The new claim is made by the caller.
     */
    public void split(BigDecimal part, LocalDate today) throws InvalidClaimStatusException, InvalidClaimException {
        if (status != ClaimStatus.OPEN) {
            throw new InvalidClaimStatusException("A " + status + " claim cannot be split");
        }
        if (isDeferredOn(today)) throw new InvalidClaimStatusException("A deferred claim cannot be split");
        if (part.signum() <= 0 || part.compareTo(amount) >= 0) {
            throw new InvalidClaimException("A split is more than 0.00 and less than the claim's amount");
        }
        amount = amount.subtract(part);
        bind(false, null);
    }
    /** The user whose editor holds it locked on the sheet's today, as read; null when no one's does. */
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    /** Its external reference, unique on the sheet regardless of case; null when it has none. */
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }
    /** Its tags: lower-cased, without repeats, sorted and comma separated; null when it has none. */
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    /** The reason it was voided with, from the sheet's list; null when it was voided without one or is not VOID. */
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
    /** The number of the bulk settle batch that settled it; null when no bulk settle did. */
    public Integer getSettleBatch() { return settleBatch; }
    public void setSettleBatch(Integer settleBatch) { this.settleBatch = settleBatch; }
    /** The date it was just settled as of, for the settle's audit line; null when settled as of today. Not stored. */
    public LocalDate getSettledOn() { return settledOn; }
    public void setSettledOn(LocalDate settledOn) { this.settledOn = settledOn; }
    /** Its tags, sorted; empty when it has none. */
    public java.util.List<String> tagList() {
        return tags == null ? java.util.List.of() : java.util.List.of(tags.split(","));
    }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public ClaimStatus getStatus() { return status; }
    /** The number of the settled claim this correction corrects; null when it is no correction. */
    public String getCorrects() { return corrects; }
    public void setCorrects(String corrects) { this.corrects = corrects; }
    /** A correction (a negative amount) has no floor and no late charge. */
    public boolean isCorrection() { return corrects != null; }
    /** The date of the claim this correction corrects; null when it is no correction or not yet read. */
    public LocalDate getCorrectedDate() { return correctedDate; }
    public void setCorrectedDate(LocalDate correctedDate) { this.correctedDate = correctedDate; }
    /** The date its figures round by: a correction's is the date of the claim it corrects. */
    public LocalDate getRoundDate() {
        return isCorrection() && correctedDate != null ? correctedDate : date;
    }
    /** The claim's number on its sheet, given when it is settled (or approved); null until then. A voided claim
     *  keeps the number it had; a number is never reused. */
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    /** The due fixed when it was settled; null while its due is live. */
    public BigDecimal getSettledDue() { return settledDue; }
    public void setSettledDue(BigDecimal settledDue) { this.settledDue = settledDue; }
    /** The user who opened it; null until it is opened. */
    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
    /** The user who last edited it (its form or its row's amount entry); null when it was never edited. */
    public String getEditedBy() { return editedBy; }
    public void setEditedBy(String editedBy) { this.editedBy = editedBy; }
    /** Whether {@code user} opened it or last edited it, and so may not settle or approve it. */
    public boolean isOwnedBy(String user) {
        return user.equals(openedBy) || user.equals(editedBy);
    }
    /** Whether its person's last statement bound it to a figure. */
    public boolean isStated() { return stated; }
    /** The figure its person's last statement bound it to; null for n/a, or when it is not stated. */
    public BigDecimal getStatedDue() { return statedDue; }
    /**
     * The figure a statement binds it to now: its stated figure (null for n/a), but null when it
     * is not stated.
     */
    public BigDecimal boundDue() {
        return isBound() ? statedDue : null;
    }
    /** Whether its stated figure is in force: it is stated (whether OPEN or PENDING). */
    public boolean isBound() {
        return stated;
    }
    /**
     * The late charge its person's last statement bound it to, with its figure; null when none is
     * bound (it is not stated, or its figure is n/a): its late charge then follows the sheet's today.
     */
    public BigDecimal getStatedLate() { return statedLate; }
    /** Bind it to a figure (null: n/a), or leave it unbound (live) when {@code stated} is false. */
    public void bind(boolean stated, BigDecimal statedDue) {
        bind(stated, statedDue, null);
    }
    /**
     * Bind it to a figure (null: n/a) and the late charge on it then (null: none bound), or leave it
     * unbound (live) when {@code stated} is false.
     */
    public void bind(boolean stated, BigDecimal statedDue, BigDecimal statedLate) {
        this.stated = stated;
        this.statedDue = stated ? statedDue : null;
        this.statedLate = stated && statedDue != null ? statedLate : null;
    }
    /**
     * Whether it counts toward its person's claimed and due-total, the sheet's totals and a
     * period's total: OPEN, PENDING and SETTLED do, held or not, except an OPEN or PENDING one once
     * their person's retirement date is reached ({@code heldOnDate}), unless
     * a statement binds it; and never one
     * deferred on {@code today}.
     */
    public boolean counts(boolean heldOnDate, LocalDate today) {
        if (isDeferredOn(today)) return false;
        return status == ClaimStatus.SETTLED || (isOpen() && (isBound() || !heldOnDate));
    }
    /**
     * Whether its amount counts toward its person's claimed and the sheet's claimed-total: as
     * {@link #counts}, but a hold (its person's or its own) does not take it out; only their
     * retirement date being reached ({@code retired}) does, unless a statement binds it.
     */
    public boolean countsClaimed(boolean retired, LocalDate today) {
        if (isDeferredOn(today)) return false;
        return status == ClaimStatus.SETTLED || (isOpen() && (isBound() || !retired));
    }
    /**
     * Whether it counts toward a period's total as it was at the period's end, when its status then
     * was {@code statusThen} (null: it did not exist yet): as {@link #counts} with that status, its
     * statement binding and deferral as they are now.
     */
    public boolean countsAsOf(ClaimStatus statusThen, boolean heldOnDate, LocalDate today) {
        if (statusThen == null || isDeferredOn(today)) return false;
        boolean openThen = statusThen == ClaimStatus.OPEN || statusThen == ClaimStatus.PENDING;
        return statusThen == ClaimStatus.SETTLED || (openThen && (isBound() || !heldOnDate));
    }
    /**
     * A copy of it as it stood at the end of a day: with the status, settled due and statement
     * binding it had then, the rest as it is now.
     */
    public Claim asThen(ClaimStatus statusThen, BigDecimal settledDueThen, boolean statedThen, BigDecimal statedDueThen,
            BigDecimal statedLateThen) {
        Claim then = new Claim(id, sheetId, personId, amount, date, note, statusThen, number, settledDueThen, openedBy);
        then.bind(statedThen, statedDueThen, statedLateThen);
        then.corrects = corrects;
        then.correctedDate = correctedDate;
        then.dueBy = dueBy;
        then.terms = terms;
        then.dueBySet = dueBySet;
        then.deferredUntil = deferredUntil;
        then.proofs = proofs;
        then.proofPhotos = proofPhotos;
        then.splitFrom = splitFrom;
        then.lateEntry = lateEntry;
        then.remindedOn = remindedOn;
        then.waived = waived;
        then.held = held;
        then.reviewUser = reviewUser;
        then.reviewOn = reviewOn;
        then.pendingOn = pendingOn;
        then.ref = ref;
        then.tags = tags;
        then.voidReason = voidReason;
        then.settleBatch = settleBatch;
        then.editedBy = editedBy;
        return then;
    }
    /** Whether it is open: OPEN, or PENDING a steward's approval. */
    public boolean isOpen() {
        return status == ClaimStatus.OPEN || status == ClaimStatus.PENDING;
    }
    /** Only a draft may be edited; to change an open claim, void it and add a new one. */
    public void checkEditable() throws InvalidClaimStatusException {
        if (status != ClaimStatus.DRAFT) throw new InvalidClaimStatusException(status);
    }

    /** A steward's approval of a PENDING claim: it is settled, its due fixed at {@code due}. */
    public void approve(BigDecimal due) throws InvalidClaimStatusException {
        if (status != ClaimStatus.PENDING) throw new InvalidClaimStatusException(status, ClaimStatus.SETTLED);
        status = ClaimStatus.SETTLED;
        settledDue = due;
    }
    /** A steward's rejection of a PENDING claim: it is OPEN again, still bound by its statement. */
    public void reject() throws InvalidClaimStatusException {
        if (status != ClaimStatus.PENDING) throw new InvalidClaimStatusException(status, ClaimStatus.OPEN);
        status = ClaimStatus.OPEN;
        settleBatch = null;
    }

    /** Move to the next status, when the current one allows it. */
    public void moveTo(ClaimStatus next) throws InvalidClaimStatusException {
        if (!status.canMoveTo(next)) throw new InvalidClaimStatusException(status, next);
        status = next;
    }
}
