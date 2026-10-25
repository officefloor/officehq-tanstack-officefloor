package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** The people a claim is shared with, as its shares column holds them (<person id>:<share>, comma separated, in the
 *  order added), and each holder's part of its due. */
public final class ClaimShares {

    /** One holder's share of a claim: the person and their percent (two decimals). */
    public record Share(Long personId, BigDecimal share) {
    }

    private ClaimShares() {
    }

    /** The claim's shares, in the order added; empty when it is shared with no one. */
    public static List<Share> of(Claim claim) {
        List<Share> shares = new ArrayList<>();
        String held = claim.getShares();
        if (held == null || held.isBlank()) {
            return shares;
        }
        for (String pair : held.split(",")) {
            int colon = pair.indexOf(':');
            shares.add(new Share(Long.valueOf(pair.substring(0, colon)), new BigDecimal(pair.substring(colon + 1)).setScale(2)));
        }
        return shares;
    }

    /** The claim's shares with one more added at the end, as its shares column holds them. */
    public static String adding(Claim claim, Long personId, BigDecimal share) {
        String pair = personId + ":" + share.setScale(2).toPlainString();
        String held = claim.getShares();
        return held == null || held.isBlank() ? pair : held + "," + pair;
    }

    /** The shares added up (0.00 with none). */
    public static BigDecimal total(List<Share> shares) {
        return shares.stream().map(Share::share).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    /** A holder's part of a due: the due times their share divided by 100, rounded half to even. */
    public static BigDecimal part(BigDecimal due, BigDecimal share) {
        return due.multiply(share).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** The holders' parts of a due added up (0.00 with no holders). */
    public static BigDecimal parts(Claim claim, BigDecimal due) {
        return of(claim).stream().map(s -> part(due, s.share())).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }
}
