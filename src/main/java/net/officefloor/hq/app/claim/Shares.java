package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** The people a claim is shared with, as stored: {@code <person id>:<share>} in the order shared, comma separated,
 *  each share a percent with two decimals; null when shared with nobody. A holder's part of the claim is its amount
 *  times their share divided by 100, times the rate of the holder's own grade in force on the claim's date divided by
 *  100, rounded like the claim (0.00 when their grade has no rate then); a SETTLED claim keeps its parts frozen: its
 *  settled due times each share divided by 100, rounded half to even. The claim's own person keeps the due less the
 *  parts. */
public final class Shares {
    private Shares() {
    }

    /** One holder of a claim's share. */
    public record Share(long personId, BigDecimal share) {
    }

    /** The holders, in the order shared; empty when none. */
    public static List<Share> list(String stored) {
        List<Share> shares = new ArrayList<>();
        if (stored == null || stored.isBlank()) {
            return shares;
        }
        for (String entry : stored.split(",")) {
            String[] parts = entry.trim().split(":");
            shares.add(new Share(Long.parseLong(parts[0]), new BigDecimal(parts[1]).setScale(2)));
        }
        return shares;
    }

    /** The holders as stored: null when none. */
    public static String stored(List<Share> shares) {
        return shares.isEmpty() ? null
                : shares.stream().map(s -> s.personId() + ":" + s.share().setScale(2).toPlainString())
                        .collect(Collectors.joining(","));
    }

    /** The shares added up. */
    public static BigDecimal total(List<Share> shares) {
        return shares.stream().map(Share::share).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    /** A holder's part of a claim's amount: amount × share ÷ 100 × pct ÷ 100, the pct being the rate of the holder's
     *  own grade in force on the claim's date, rounded by {@code mode} to two decimals; 0.00 when there is no rate. */
    public static BigDecimal part(BigDecimal amount, BigDecimal share, BigDecimal pct, RoundingMode mode) {
        if (pct == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return amount.multiply(share).multiply(pct).movePointLeft(4).setScale(2, mode);
    }

    /** A holder's part of the claim whose due is {@code figure}: frozen from the due when it is SETTLED, else from its
     *  amount at {@code pct}, the rate of the holder's own grade in force on its date, rounded like the claim. */
    public static BigDecimal part(Claim claim, BigDecimal figure, BigDecimal share, BigDecimal pct, Rounding rounding) {
        return claim.getStatus() == ClaimStatus.SETTLED ? part(figure, share)
                : part(claim.getAmount(), share, pct, rounding.mode(claim));
    }

    /** A holder's frozen part of a settled due: the due times the share divided by 100, rounded half to even to two decimals. */
    public static BigDecimal part(BigDecimal due, BigDecimal share) {
        return due.multiply(share).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }
}
