package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim of a person on a sheet. Mutable so an Apply step can change it in place. */
public class Claim {

    private final long id;
    private final String sheetId;
    private final long personId;
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
    private LocalDate roundOn;

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

    /** Whether the person's last statement bound the claim to a figure. */
    public boolean isStated() {
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
     * The claim's live figure at the rate: its amount times the rate, rounded to two decimals by the rounding given and
     * at least the floor — a correction has no floor. Null without a rate.
     */
    public BigDecimal figureAt(BigDecimal rate, BigDecimal floor, java.math.RoundingMode rounding) {
        if (rate == null) {
            return null;
        }
        BigDecimal figure = amount.multiply(rate).movePointLeft(2).setScale(2, rounding);
        return isCorrection() ? figure : figure.max(floor);
    }
}
