package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Grades;
import net.officefloor.hq.app.person.Holds;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.DatedAmount;

/** Claim figures and due-totals, as the screen shows them. */
public final class Dues {
    private Dues() {
    }

    /** A claim's figure: the due fixed when it was settled, else the one its last statement bound it to, n/a
     *  included (not while it is PENDING: then it is live), else live: its amount times its person's rate in force on
     *  its date for their grade then, divided by 100, rounded to two decimals as the claim rounds (see Rounding); null
     *  (n/a) when no rate applies. The floor is not in it: it is applied last (see shown). */
    public static BigDecimal figure(Claim claim, Grades grades, RateRepository rates, Rounding rounding) {
        if (claim.settledDue() != null) {
            return claim.settledDue();
        }
        if (claim.stated() && claim.status() != ClaimStatus.PENDING) {
            return claim.statedDue();
        }
        return live(claim, grades, rates, rounding);
    }

    /** A claim's due as its row shows it: the due fixed when it was settled, else the greater of the floor in force
     *  on its date and its figure (bound or live) with the late charge on it added (a correction, a negative amount,
     *  has no floor and no charge); null (n/a) when it has no figure. */
    public static BigDecimal shown(Claim claim, Grades grades, RateRepository rates, DatedAmount floor,
            LateCharge late) {
        if (claim.settledDue() != null) {
            return claim.settledDue();
        }
        BigDecimal figure = figure(claim, grades, rates, late.rounding());
        if (figure == null || claim.amount().signum() < 0) {
            return figure;
        }
        return figure.add(late.on(claim, figure)).max(floor.on(claim.date()));
    }

    /** A claim's live figure: its amount times its person's rate in force on its date for their grade then, divided
     *  by 100, rounded to two decimals as the claim rounds (see Rounding); null (n/a) when no rate applies. */
    public static BigDecimal live(Claim claim, Grades grades, RateRepository rates, Rounding rounding) {
        return rates.on(claim.sheetId(), grades.on(claim.date()), claim.date())
                .map(r -> claim.amount().multiply(r.pct()).divide(BigDecimal.valueOf(100), 2, rounding.of(claim)))
                .orElse(null);
    }

    /** Whether a claim counts toward its person's figures and every live total: an OPEN, PENDING or SETTLED one,
     *  except an OPEN one dated within one of its person's holds and not bound by a statement, and one deferred on the
     *  sheet's today (see Claim.deferredOn). */
    public static boolean counts(Claim claim, Holds holds, LocalDate today) {
        return claim.status().counts() && !claim.deferredOn(today)
                && !(claim.status() == ClaimStatus.OPEN && !claim.stated() && holds.covers(claim.date()));
    }

    /** Whether a claim counts toward a period's due-totals: its status at the end of the period's last day (see
     *  ClaimLogRepository.statusesAt; null: it was added after) is OPEN, PENDING or SETTLED, and it is left out
     *  as the live rules leave it out: deferred on the sheet's today, or an OPEN one dated within one of its
     *  person's holds and not bound by a statement. */
    public static boolean countsAtEnd(Claim claim, ClaimStatus atEnd, Holds holds, LocalDate today) {
        return atEnd != null && atEnd.counts() && !claim.deferredOn(today)
                && !(claim.status() == ClaimStatus.OPEN && !claim.stated() && holds.covers(claim.date()));
    }

    /** A person's due-total: the dues (as shown) of their claims that count added up, skipping n/a; null (n/a) when
     *  their grade today has no rate today, unless a claim that counts has a figure (settled, stated, or live at
     *  the grade in force on its date). */
    public static BigDecimal total(String sheetId, List<Claim> claims, Grades grades, Holds holds, LocalDate today,
            RateRepository rates, DatedAmount floor, LateCharge late) {
        List<Claim> counted = claims.stream().filter(c -> counts(c, holds, today)).toList();
        List<BigDecimal> figures = counted.stream().map(c -> shown(c, grades, rates, floor, late))
                .filter(f -> f != null).toList();
        if (figures.isEmpty() && rates.on(sheetId, grades.on(today), today).isEmpty()) {
            return null;
        }
        return figures.stream().reduce(new BigDecimal("0.00"), BigDecimal::add);
    }

    /** A due-total at most the sheet's cap (null: none); n/a (null) is not capped. */
    public static BigDecimal capped(BigDecimal total, BigDecimal cap) {
        return total != null && cap != null ? total.min(cap) : total;
    }
}
