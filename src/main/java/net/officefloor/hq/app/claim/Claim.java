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
    private boolean stated;
    private BigDecimal statedDue;
    private String corrects;
    private LocalDate correctedDate;
    private boolean relisted;
    private LocalDate dueBy;
    private LocalDate deferredUntil;

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
     * Move it to another person: it keeps its number and date, but loses any figure a statement
     * bound it to (it is live again until its new person's next statement). A SETTLED or VOID claim
     * cannot move.
     */
    public void reassign(long toPersonId) throws InvalidClaimStatusException {
        if (status == ClaimStatus.SETTLED || status == ClaimStatus.VOID) {
            throw new InvalidClaimStatusException("A " + status + " claim cannot be moved to another person");
        }
        personId = toPersonId;
        bind(false, null);
        relisted = true;
    }
    /** Whether it was moved since read, so it is listed again as its new person's newest. */
    public boolean isRelisted() { return relisted; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    /** Its due-by, fixed when it was made: its date plus its grade's terms then. Kept when its date changes. */
    public LocalDate getDueBy() { return dueBy; }
    public void setDueBy(LocalDate dueBy) { this.dueBy = dueBy; }
    /** The date it is deferred until; null when it was never deferred. */
    public LocalDate getDeferredUntil() { return deferredUntil; }
    public void setDeferredUntil(LocalDate deferredUntil) { this.deferredUntil = deferredUntil; }
    /**
     * Whether it is deferred on {@code today}: OPEN, and deferred until a date after today. From
     * that date on it is an OPEN claim like any other.
     */
    public boolean isDeferredOn(LocalDate today) {
        return status == ClaimStatus.OPEN && deferredUntil != null && today != null && today.isBefore(deferredUntil);
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
    /** The claim's number on its sheet, given when it is opened; null for a draft. A voided claim
     *  keeps the number it had; a number is never reused. */
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    /** The due fixed when it was settled; null while its due is live. */
    public BigDecimal getSettledDue() { return settledDue; }
    public void setSettledDue(BigDecimal settledDue) { this.settledDue = settledDue; }
    /** The user who opened it; null until it is opened. */
    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
    /** Whether its person's last statement bound it to a figure. */
    public boolean isStated() { return stated; }
    /** The figure its person's last statement bound it to; null for n/a, or when it is not stated. */
    public BigDecimal getStatedDue() { return statedDue; }
    /**
     * The figure a statement binds it to now: its stated figure (null for n/a), but null when it
     * is not stated or is PENDING (a PENDING claim shows its live figure; a rejection binds it again).
     */
    public BigDecimal boundDue() {
        return isBound() ? statedDue : null;
    }
    /** Whether its stated figure is in force: it is stated and not PENDING. */
    public boolean isBound() {
        return stated && status != ClaimStatus.PENDING;
    }
    /** Bind it to a figure (null: n/a), or leave it unbound (live) when {@code stated} is false. */
    public void bind(boolean stated, BigDecimal statedDue) {
        this.stated = stated;
        this.statedDue = stated ? statedDue : null;
    }
    /**
     * Whether it counts toward its person's claimed and due-total, the sheet's totals and a
     * period's total: OPEN, PENDING and SETTLED do, except an OPEN or PENDING one dated within one
     * of its person's holds ({@code heldOnDate}), unless a statement binds it; and never one
     * deferred on {@code today}.
     */
    public boolean counts(boolean heldOnDate, LocalDate today) {
        if (isDeferredOn(today)) return false;
        return status == ClaimStatus.SETTLED || (isOpen() && (isBound() || !heldOnDate));
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
    /** A steward's rejection of a PENDING claim: it is OPEN again, bound again by its statement. */
    public void reject() throws InvalidClaimStatusException {
        if (status != ClaimStatus.PENDING) throw new InvalidClaimStatusException(status, ClaimStatus.OPEN);
        status = ClaimStatus.OPEN;
    }

    /** Move to the next status, when the current one allows it. */
    public void moveTo(ClaimStatus next) throws InvalidClaimStatusException {
        if (!status.canMoveTo(next)) throw new InvalidClaimStatusException(status, next);
        status = next;
    }
}
