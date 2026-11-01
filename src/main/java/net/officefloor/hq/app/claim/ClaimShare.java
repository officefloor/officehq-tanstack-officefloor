package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** A share of a claim held by another person on its sheet: a percent (two decimals, more than 0.00)
 *  of the claim, whose part (see {@link Figures#part}) counts in their figures in place of the claim's
 *  own person's. */
public record ClaimShare(long personId, BigDecimal share) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** The holder's part of a settled claim's fixed due, frozen with it: the due times the share divided by 100, rounded half to even. */
    public BigDecimal partOf(BigDecimal due) {
        return due.multiply(share).divide(HUNDRED, 2, RoundingMode.HALF_EVEN);
    }

    /** The shares as the claim's row lists them, "<person id>:<share>" comma separated; none for null. */
    static List<ClaimShare> parse(String listed) {
        List<ClaimShare> shares = new ArrayList<>();
        if (listed == null || listed.isBlank()) return shares;
        for (String one : listed.split(",")) {
            String[] parts = one.split(":");
            shares.add(new ClaimShare(Long.parseLong(parts[0].trim()), new BigDecimal(parts[1].trim()).setScale(2)));
        }
        return shares;
    }
}
