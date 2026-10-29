package net.officefloor.hq.app.rate;

/**
 * A rate as the screen sees it: {@code rate} has two decimals ("12.50"); {@code from} and {@code to} are yyyy-mm-dd or null.
 * {@code version} is how many versions it has had (the newest is the one shown). {@code level} is the sheet it is the rate of: 0 the sheet's own, 1 its parent's, 2 its parent's parent, and so on, then the sheets of its clone source's lineage (see Rates#lineage).
 * {@code claimsAfter}, for a forward-only rate, is the claim id above which it applies (null: every claim).
 */
public record RateResponse(String grade, String rate, String from, String to, int level, int version, Long claimsAfter) {

    public static RateResponse from(Rate rate) {
        return from(rate, 0);
    }

    public static RateResponse from(Rate rate, int level) {
        return new RateResponse(rate.grade(), rate.rate().toPlainString(),
                rate.from() == null ? null : rate.from().toString(),
                rate.to() == null ? null : rate.to().toString(), level, rate.version(), rate.claimsAfter());
    }
}
