package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.sheet.Sheet;

/**
 * The figures of one person's claims, as the screen works them out: a settled claim's frozen due, else the
 * figure its person's last statement bound it to, else its live due (amount times its grade's rate in force on
 * its date, rounded half to even, and at least the sheet's floor; none — n/a — when no rate applies then). An
 * overdue claim's due adds, on top of its figure, the late charge: the sheet's late percent of the figure for every
 * full 30 days it is overdue on the sheet's today, rounded like a due. A statement never binds the charge; settling
 * freezes it in.
 */
public class ClaimFigures {

    private final List<Rate> rates;

    private final BigDecimal floor;

    private final BigDecimal latePct;

    private final int terms;

    private final LocalDate today;

    /** @param rates the rates of the person's grade.
     *  @param sheet the sheet: its floor (a live due is at least it), and its late percent, terms and today (the
     *  late charge). */
    public ClaimFigures(List<Rate> rates, Sheet sheet) {
        this.rates = rates;
        this.floor = sheet.getFloor();
        this.latePct = sheet.getLatePct();
        this.terms = sheet.getTerms();
        this.today = sheet.getToday();
    }

    /** Whether a claim is overdue: it is OPEN and its due-by (its date plus the terms) is before the sheet's today. */
    public boolean overdue(Claim claim) {
        return claim.getStatus() == ClaimStatus.OPEN && claim.getDate().plusDays(terms).isBefore(today);
    }

    /** The late charge on a figure: the late percent of it for every full 30 days the claim is overdue, rounded
     *  half to even; 0.00 when it is not overdue. */
    public BigDecimal lateCharge(Claim claim, BigDecimal figure) {
        if (figure == null || !overdue(claim)) {
            return BigDecimal.ZERO.setScale(2);
        }
        long periods = ChronoUnit.DAYS.between(claim.getDate().plusDays(terms), today) / 30;
        return figure.multiply(latePct).multiply(BigDecimal.valueOf(periods)).movePointLeft(2)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    /** A live due: the amount times the percent, divided by 100, rounded half to even, and at least the floor. */
    public static BigDecimal liveDue(BigDecimal amount, BigDecimal pct, BigDecimal floor) {
        BigDecimal due = amount.multiply(pct).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
        return floor != null && due.compareTo(floor) < 0 ? floor.setScale(2) : due;
    }

    /** Whether a claim counts toward the totals: only OPEN and SETTLED claims do. */
    public static boolean counts(Claim claim) {
        return claim.getStatus() == ClaimStatus.OPEN || claim.getStatus() == ClaimStatus.SETTLED;
    }

    /** Whether a claim counts toward the live totals: as {@link #counts}, except a held person's OPEN claims. */
    public static boolean countsLive(Claim claim, boolean held) {
        return counts(claim) && !(held && claim.getStatus() == ClaimStatus.OPEN);
    }

    private Optional<BigDecimal> rateOn(LocalDate on) {
        return rates.stream()
                .filter(r -> r.getFromDate() == null || !r.getFromDate().isAfter(on))
                .max(Comparator.comparing(Rate::getFromDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Rate::getPct);
    }

    /** The claim's due: its figure, plus the late charge while it is not settled; null when it has none (n/a). */
    public BigDecimal due(Claim claim) {
        if (claim.getDue() != null) {
            return claim.getDue();
        }
        BigDecimal figure = figure(claim);
        return figure == null ? null : figure.add(lateCharge(claim, figure));
    }

    /** The claim's figure, without any late charge; null when it has none (n/a). */
    public BigDecimal figure(Claim claim) {
        if (claim.getDue() != null) {
            return claim.getDue();
        }
        if (claim.isStated()) {
            return claim.getStatedDue();
        }
        return rateOn(claim.getDate())
                .map(pct -> liveDue(claim.getAmount(), pct, floor))
                .orElse(null);
    }

    /** The person's due-total: their counted claims' dues (with any late charge) added up, 0.00 with none; n/a when their grade has
     *  no rate in force on the day, unless a counted claim keeps a fixed figure (settled or bound). */
    public String dueTotal(List<Claim> claims, LocalDate today) {
        return dueTotal(claims, today, false);
    }

    /** The person's live due-total, as {@link #dueTotal(List, LocalDate)}; while they are held their OPEN
     *  claims do not count. */
    public String dueTotal(List<Claim> claims, LocalDate today, boolean held) {
        List<Claim> counted = claims.stream().filter(c -> countsLive(c, held)).toList();
        boolean fixed = counted.stream().anyMatch(c -> c.getDue() != null || (c.isStated() && c.getStatedDue() != null));
        if (rateOn(today).isEmpty() && !fixed) {
            return "n/a";
        }
        return counted.stream().map(this::due).filter(f -> f != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2).toPlainString();
    }
}
