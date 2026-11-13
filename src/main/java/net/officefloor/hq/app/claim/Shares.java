package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** The people a claim is shared with, as stored: {@code <person id>:<share>} in the order shared, comma separated,
 *  each share a percent with two decimals; null when shared with nobody. A holder's part of the claim's due is the
 *  due times their share divided by 100, rounded half to even; the claim's own person keeps the due less the parts. */
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

    /** A holder's part of a due: the due times the share divided by 100, rounded half to even to two decimals. */
    public static BigDecimal part(BigDecimal due, BigDecimal share) {
        return due.multiply(share).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }
}
