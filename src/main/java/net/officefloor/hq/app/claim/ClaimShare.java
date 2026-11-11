package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** A holder's share of a claim: the person holding it and their percent, two decimals. */
public record ClaimShare(long personId, BigDecimal share) {

    /** Their part of a due figure: the figure times their share divided by 100, rounded half to even. */
    public BigDecimal partOf(BigDecimal figure) {
        return figure.multiply(share).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Shares added up: 0.00 when there are none. */
    public static BigDecimal total(List<ClaimShare> shares) {
        return shares.stream().map(ClaimShare::share).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
