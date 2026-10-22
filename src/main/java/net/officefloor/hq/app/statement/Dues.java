package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.rate.RateRepository;

/** Claim figures and due-totals, as the screen shows them. */
public final class Dues {
    private Dues() {
    }

    /** A claim's figure: the due fixed when it was settled, else the one its last statement bound it to, else
     *  live: its amount times its person's grade rate in force on its date, divided by 100, rounded half to
     *  even to two decimals; null (n/a) when no rate applies. */
    public static BigDecimal figure(Claim claim, String grade, RateRepository rates) {
        if (claim.settledDue() != null) {
            return claim.settledDue();
        }
        if (claim.statedDue() != null) {
            return claim.statedDue();
        }
        return rates.on(claim.sheetId(), grade, claim.date())
                .map(r -> claim.amount().multiply(r.pct()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN))
                .orElse(null);
    }

    /** A person's due-total: the figures of their claims that count added up, skipping n/a; null (n/a) when
     *  their grade has no rate today, unless a claim that counts keeps a figure (settled or stated). */
    public static BigDecimal total(String sheetId, List<Claim> claims, String grade, LocalDate today, RateRepository rates) {
        List<Claim> counted = claims.stream().filter(c -> c.status().counts()).toList();
        boolean kept = counted.stream().anyMatch(c -> c.settledDue() != null || c.statedDue() != null);
        if (!kept && rates.on(sheetId, grade, today).isEmpty()) {
            return null;
        }
        return counted.stream().map(c -> figure(c, grade, rates)).filter(f -> f != null)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
    }
}
