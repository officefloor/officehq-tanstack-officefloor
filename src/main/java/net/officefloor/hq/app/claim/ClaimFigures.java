package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.officefloor.hq.app.rate.Rate;

/**
 * The figures of one person's claims, as the screen works them out: a settled claim's frozen due, else the
 * figure its person's last statement bound it to, else its live due (amount times its grade's rate in force on
 * its date, rounded half to even; none — n/a — when no rate applies then).
 */
public class ClaimFigures {

    private final List<Rate> rates;

    /** @param rates the rates of the person's grade. */
    public ClaimFigures(List<Rate> rates) {
        this.rates = rates;
    }

    /** Whether a claim counts toward the totals: only OPEN and SETTLED claims do. */
    public static boolean counts(Claim claim) {
        return claim.getStatus() == ClaimStatus.OPEN || claim.getStatus() == ClaimStatus.SETTLED;
    }

    private Optional<BigDecimal> rateOn(LocalDate on) {
        return rates.stream()
                .filter(r -> r.getFromDate() == null || !r.getFromDate().isAfter(on))
                .max(Comparator.comparing(Rate::getFromDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Rate::getPct);
    }

    /** The claim's figure; null when it has none (n/a). */
    public BigDecimal figure(Claim claim) {
        if (claim.getDue() != null) {
            return claim.getDue();
        }
        if (claim.isStated()) {
            return claim.getStatedDue();
        }
        return rateOn(claim.getDate())
                .map(pct -> claim.getAmount().multiply(pct).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN))
                .orElse(null);
    }

    /** The person's due-total: their counted claims' figures added up, 0.00 with none; n/a when their grade has
     *  no rate in force on the day, unless a counted claim keeps a fixed figure (settled or bound). */
    public String dueTotal(List<Claim> claims, LocalDate today) {
        List<Claim> counted = claims.stream().filter(ClaimFigures::counts).toList();
        boolean fixed = counted.stream().anyMatch(c -> c.getDue() != null || (c.isStated() && c.getStatedDue() != null));
        if (rateOn(today).isEmpty() && !fixed) {
            return "n/a";
        }
        return counted.stream().map(this::figure).filter(f -> f != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2).toPlainString();
    }
}
