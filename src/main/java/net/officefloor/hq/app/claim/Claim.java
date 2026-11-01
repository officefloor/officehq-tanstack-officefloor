package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.rate.Rate;

/**
 * A claim as the screen shows it: amount with two decimals, date (ISO yyyy-MM-dd), note, status and its number on
 * the sheet (given when opened and kept once voided; null while a draft), and its due fixed when settled (two decimals;
 * null until settled, and when the grade had no rate then), and the user who opened it (null until opened), and
 * whether its person's last statement bound it, with the figure it bound (null when the grade had no rate then).
 */
public record Claim(long id, long person, String amount, String date, String note, ClaimStatus status,
        Integer number, String settledDue, String openedBy, boolean stated, String statedDue) {

    /** The audit fields of this claim for the named person, as {@link ClaimInput#auditFields}. */
    public String auditFields(String person) {
        return "person=" + person + " amount=" + amount + " date=" + date;
    }

    /** This claim, numbered: its audit fields end with number=<number> when it has one. */
    public Claim withNumber(Integer number) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue);
    }

    /** This claim, settled with its due fixed (null when its grade had no rate). */
    public Claim withSettledDue(String settledDue) {
        return new Claim(id, person, amount, date, note, status, number, settledDue, openedBy, stated, statedDue);
    }

    /** The audit fields of a status change: {@link #auditFields} then number=<number> when it has one. */
    public String statusAuditFields(String person) {
        return auditFields(person) + (number == null ? "" : " number=" + number);
    }

    /** Whether this claim counts toward the totals: OPEN and SETTLED claims do. */
    public boolean counts() {
        return status == ClaimStatus.OPEN || status == ClaimStatus.SETTLED;
    }

    /** Whether this claim keeps a figure whatever the rates: fixed when settled, or bound by a statement. */
    public boolean fixed() {
        return settledDue != null || stated;
    }

    /**
     * This claim's due: the one fixed when settled, else the one its statement bound, else its amount at the
     * grade's rate in force on its date; null when there is none.
     */
    public BigDecimal due(List<Rate> rates, String grade) {
        if (settledDue != null) {
            return new BigDecimal(settledDue);
        }
        if (stated) {
            return statedDue == null ? null : new BigDecimal(statedDue);
        }
        return Rate.due(amount, Rate.on(rates, grade, LocalDate.parse(date)));
    }
}
