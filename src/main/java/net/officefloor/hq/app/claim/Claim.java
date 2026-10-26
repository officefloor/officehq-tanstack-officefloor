package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim of a person on a sheet. Mutable so an Apply step can change it in place. */
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

    private String editedBy;
    private Integer settleBatch;
    private Long addedSeq;
    private boolean stated;
    private BigDecimal statedDue;
    private String corrects;
    private LocalDate roundOn;
    private LocalDate dueFrom;
    private int terms;
    private LocalDate dueBy;
    private LocalDate deferredUntil;
    private int proofs;
    private int photoProofs;
    private String splitFrom;
    private boolean lateEntry;
    private LocalDate remindedOn;
    private boolean waived;
    private boolean held;
    private String ref;
    private String tags;
    private String voidReason;

    public Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal settledDue, String openedBy,
            boolean stated, BigDecimal statedDue, String corrects) {
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
        this.roundOn = date;
    }

    /** The date the claim's due-by counts from: its date when it was added, kept when the date later changes. */
    public LocalDate getDueFrom() {
        return dueFrom;
    }

    public void setDueFrom(LocalDate dueFrom) {
        this.dueFrom = dueFrom;
    }

    /** The terms the claim was fixed with when it was added: those in force on its date for its person, in business days. */
    public int getTerms() {
        return terms;
    }

    public void setTerms(int terms) {
        this.terms = terms;
    }

    /**
     * The date the claim is due by: the terms-th business day after its due-from date (skipping Saturdays, Sundays and
     * the sheet's holidays as they stand now), worked out whenever the claim is read.
     */
    public LocalDate getDueBy() {
        return dueBy;
    }

    public void setDueBy(LocalDate dueBy) {
        this.dueBy = dueBy;
    }

    /** The date the claim was last deferred until; null when it was never deferred. */
    public LocalDate getDeferredUntil() {
        return deferredUntil;
    }

    public void setDeferredUntil(LocalDate deferredUntil) {
        this.deferredUntil = deferredUntil;
    }

    /** The number of the claim this one was split off; null when it was not split off one. */
    public String getSplitFrom() {
        return splitFrom;
    }

    public void setSplitFrom(String splitFrom) {
        this.splitFrom = splitFrom;
    }

    /** Whether the claim was added while the period containing its date was soft-closed. */
    public boolean isLateEntry() {
        return lateEntry;
    }

    public void setLateEntry(boolean lateEntry) {
        this.lateEntry = lateEntry;
    }

    /** The last sheet-today a reminder was written for the claim; null when never reminded. */
    /** Whether a steward waived the claim's late charge: it bears none from then on. */
    public boolean isWaived() {
        return waived;
    }

    public void setWaived(boolean waived) {
        this.waived = waived;
    }

    /** Whether the claim is held: OPEN or PENDING and unbound, it then counts in no live total. */
    public boolean isHeld() {
        return held;
    }

    public void setHeld(boolean held) {
        this.held = held;
    }

    public LocalDate getRemindedOn() {
        return remindedOn;
    }

    public void setRemindedOn(LocalDate remindedOn) {
        this.remindedOn = remindedOn;
    }

    /** The claim's external reference; null when it has none. */
    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    /** The claim's tags, sorted and joined by commas; null when it has none. */
    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    /** The reason the claim was voided with, from the sheet's list; null when none was given. */
    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    /** Keep {@code amount} of the claim, the rest split off: it loses any figure a statement bound it to. */
    public void keep(BigDecimal amount) {
        this.amount = amount;
        this.stated = false;
        this.statedDue = null;
    }

    /** How many proofs the claim carries. */
    public int getProofs() {
        return proofs;
    }

    public void setProofs(int proofs) {
        this.proofs = proofs;
    }

    /** How many of the claim's proofs are photos. */
    public int getPhotoProofs() {
        return photoProofs;
    }

    public void setPhotoProofs(int photoProofs) {
        this.photoProofs = photoProofs;
    }

    /** How many of the claim's proofs are of the kind (receipt or photo): every proof not a photo is a receipt. */
    public int proofsOf(String kind) {
        return ProofKind.PHOTO.equals(kind) ? photoProofs : ProofKind.RECEIPT.equals(kind) ? proofs - photoProofs : 0;
    }

    /** Whether the claim is deferred as of {@code today}: OPEN, and deferred until a date after today. Until then it
     *  counts in no total, is not overdue, cannot be settled and is left unbound by a statement. */
    public boolean isDeferredOn(LocalDate today) {
        return status == ClaimStatus.OPEN && deferredUntil != null && today != null && today.isBefore(deferredUntil);
    }

    /** The date the claim's live figures round by: its own, or for a correction the date of the claim it corrects. */
    public LocalDate getRoundOn() {
        return roundOn;
    }

    public void setRoundOn(LocalDate roundOn) {
        this.roundOn = roundOn;
    }

    public long getId() {
        return id;
    }

    public String getSheetId() {
        return sheetId;
    }

    public long getPersonId() {
        return personId;
    }

    /** Move the claim to another person: it loses any figure a statement bound it to. */
    public void moveTo(long personId) {
        this.personId = personId;
        this.stated = false;
        this.statedDue = null;
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
        if (corrects == null) {
            this.roundOn = date;
        }
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    /** The number given when the claim was opened; null for a draft (or a claim voided as a draft); a voided claim keeps its number, never reused. */
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    /** The due fixed when the claim was settled; null when not settled or settled without a rate. */
    public BigDecimal getSettledDue() {
        return settledDue;
    }

    public void setSettledDue(BigDecimal settledDue) {
        this.settledDue = settledDue;
    }

    /** The signed-in user who opened the claim; null when it was never opened. */
    public String getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(String openedBy) {
        this.openedBy = openedBy;
    }

    /** The number of the bulk settle batch that settled the claim; null when no batch settled it. */
    public Integer getSettleBatch() {
        return settleBatch;
    }

    public void setSettleBatch(Integer settleBatch) {
        this.settleBatch = settleBatch;
    }

    /** Where the claim's adding stands in the order of changes: a forward-only rate saved later applies to it, one
     *  saved earlier does not (see {@link net.officefloor.hq.app.rate.Rate#forClaim}). Null for a claim added before
     *  forward-only rates, which counts as added before every one. */
    public Long getAddedSeq() {
        return addedSeq;
    }

    public void setAddedSeq(Long addedSeq) {
        this.addedSeq = addedSeq;
    }

    /** The signed-in user who last edited the claim through its form or amount entry; null when never edited. */
    public String getEditedBy() {
        return editedBy;
    }

    public void setEditedBy(String editedBy) {
        this.editedBy = editedBy;
    }

    /** Whether the person's last statement bound the claim to a figure. */
    public boolean isStated() {
        return stated;
    }

    /** Whether the last statement's figure is the claim's: it was stated (a PENDING claim too, like any other claim
     *  that counts). */
    public boolean isBound() {
        return stated;
    }

    /** The figure the last statement bound the claim to; null when bound without one (n/a) or not bound. */
    public BigDecimal getStatedDue() {
        return statedDue;
    }

    /** The number of the SETTLED claim this correction corrects; null for a claim that is not a correction. */
    public String getCorrects() {
        return corrects;
    }

    public void setCorrects(String corrects) {
        this.corrects = corrects;
    }

    /** Whether the claim is a correction: its amount is below 0.00 (no floor and no late charge apply). */
    public boolean isCorrection() {
        return amount.signum() < 0;
    }

    /**
     * The claim's rate figure: its amount times the rate, rounded to two decimals by the rounding given. The floor is
     * not applied here; it comes last, after the late charge (see LateCharge). Null without a rate.
     */
    public BigDecimal figureAt(BigDecimal rate, java.math.RoundingMode rounding) {
        if (rate == null) {
            return null;
        }
        return amount.multiply(rate).movePointLeft(2).setScale(2, rounding);
    }
}
