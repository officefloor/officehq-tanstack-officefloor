package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.setting.Settings;

/** A holder's share of a claim: the person holding it and their percent, two decimals. */
public record ClaimShare(long personId, BigDecimal share) {

    /** Their part of a due figure: the figure times their share divided by 100, rounded half to even. */
    public BigDecimal partOf(BigDecimal figure) {
        return figure.multiply(share).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Their part of a claim whose due is {@code figure}: a settled claim keeps its parts as frozen (its due
     * times their share / 100, rounded half to even); else the claim's amount times their share / 100, times
     * the rate of the holder's own grade in force on the claim's date / 100, rounded like the claim. Null
     * when their grade has no rate then (a part of 0.00 to the claim's own person).
     */
    public BigDecimal partOf(Claim claim, BigDecimal figure, Person holder, List<Rate> rates, Settings settings,
            LocalDate today) {
        if (claim.getSettledDue() != null) return partOf(figure);
        if (holder == null) return null;
        RoundingMode mode = ClaimFigure.rounding(claim, settings, today);
        return Rate.inForce(rates, holder.gradeOn(claim.getDate()), claim.getDate(), claim.getId())
                .map((pct) -> claim.getAmount().multiply(share).multiply(pct).divide(BigDecimal.valueOf(10000), 2, mode))
                .orElse(null);
    }

    /** Shares added up: 0.00 when there are none. */
    public static BigDecimal total(List<ClaimShare> shares) {
        return shares.stream().map(ClaimShare::share).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
