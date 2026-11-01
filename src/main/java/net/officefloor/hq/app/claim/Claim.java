package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.rate.Rate;

/**
 * A claim as the screen shows it: amount with two decimals, date (ISO yyyy-MM-dd), note, status and its number on
 * the sheet (given when opened and kept once voided; null while a draft), and its due fixed when settled (two decimals;
 * null until settled, and when the grade had no rate then), and the user who opened it (null until opened), and
 * whether its person's last statement bound it, with the figure it bound (null when the grade had no rate then), and
 * the number of the claim it corrects (null unless it is a correction), and the date it rounds on: its own, or for
 * a correction the date of the claim it corrects, and its number within its period (<period>-<n>, or X-<n> in no
 * period) when it was opened while the sheet numbered by period, in place of a number on the sheet (null otherwise).
 */
public record Claim(long id, long person, String amount, String date, String note, ClaimStatus status,
        Integer number, String settledDue, String openedBy, boolean stated, String statedDue, String corrects,
        String roundsOn, String periodNumber) {

    /** Whether this claim is a correction: a negative amount, naming the number (as it shows) of the settled claim it corrects. */
    public boolean correction() {
        return amount.startsWith("-");
    }

    /** The audit fields of this claim for the named person, as {@link ClaimInput#auditFields}. */
    public String auditFields(String person) {
        return "person=" + person + " amount=" + amount + " date=" + date;
    }

    /** This claim, numbered: its audit fields end with number=<number> when it has one. */
    public Claim withNumber(Integer number) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, roundsOn, periodNumber);
    }

    /** This claim, numbered within its period: its audit fields end with number=<period number>. */
    public Claim withPeriodNumber(String periodNumber) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, roundsOn, periodNumber);
    }

    /** This claim, settled with its due fixed (null when its grade had no rate). */
    public Claim withSettledDue(String settledDue) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, roundsOn, periodNumber);
    }

    /** The number the claim shows: its number within its period, else its number on the sheet; null for none. */
    public String shownNumber() {
        return periodNumber != null ? periodNumber : number == null ? null : number.toString();
    }

    /** The audit fields of a status change: {@link #auditFields} then number=<number> when it has one. */
    public String statusAuditFields(String person) {
        String shown = shownNumber();
        return auditFields(person) + (shown == null ? "" : " number=" + shown);
    }

    /** Whether this claim counts toward the totals: OPEN and SETTLED claims do. */
    public boolean counts() {
        return status == ClaimStatus.OPEN || status == ClaimStatus.SETTLED;
    }

    /** Whether this claim counts toward the live totals when its person is {@code held}: then only SETTLED does. */
    public boolean counts(boolean held) {
        return held ? status == ClaimStatus.SETTLED : counts();
    }

    /**
     * Whether this claim keeps a figure whatever the rates: fixed when settled, or bound by a statement (a claim
     * settled or bound when its grade had no rate keeps none).
     */
    public boolean fixed() {
        return settledDue != null || (stated && statedDue != null);
    }

    /**
     * This claim's due: its {@link #figure} plus, while it is overdue, its late charge (settling fixes the charge
     * in); null when there is no figure.
     */
    public BigDecimal due(List<Rate> rates, String grade, BigDecimal floor, Late late) {
        BigDecimal figure = figure(rates, grade, floor, late.rounding(this));
        return figure == null || settledDue != null ? figure : figure.add(late.charge(this, figure));
    }

    /**
     * This claim's figure: the due fixed when settled, else the one its statement bound, else its amount at the
     * grade's rate in force on its date, raised to at least the sheet's floor (a correction has no floor); null when
     * there is none.
     */
    public BigDecimal figure(List<Rate> rates, String grade, BigDecimal floor) {
        return figure(rates, grade, floor, RoundingMode.HALF_EVEN);
    }

    /** This claim's {@link #figure}, its live figure rounded by the mode. */
    public BigDecimal figure(List<Rate> rates, String grade, BigDecimal floor, RoundingMode rounding) {
        if (settledDue != null) {
            return new BigDecimal(settledDue);
        }
        if (stated) {
            return statedDue == null ? null : new BigDecimal(statedDue);
        }
        BigDecimal live = Rate.due(amount, Rate.on(rates, grade, LocalDate.parse(date)), rounding);
        return live == null || correction() ? live : live.max(floor);
    }
}
