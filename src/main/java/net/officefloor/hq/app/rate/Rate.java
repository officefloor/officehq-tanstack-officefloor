package net.officefloor.hq.app.rate;

/** A grade's rate on a sheet: a percent with two decimals, as text, in force from its from-date
 *  (yyyy-MM-dd; null applies from the beginning of time) through its to-date (null: no end). Its level
 *  is how far up the sheet's parent chain it comes from (0: the sheet's own, 1: its parent's, ...). Its
 *  version is how many versions the grade's rate from that from-date has had (it is the newest).
 *  A forward-only rate (saved with a from-date on or after the sheet's today) has {@code claimsAfter}:
 *  the highest claim id there was when it was saved; it applies only to claims with a higher id, and
 *  those up to it go by {@code prior}, the version it replaced (null: none from its from-date). A rate
 *  without {@code claimsAfter} applies to every claim. */
public record Rate(String sheetId, String grade, String pct, String from, String to, int level, int version,
        Long claimsAfter, Rate prior) {

    /** Whether the rate has ended before the date (yyyy-MM-dd). */
    public boolean endedBefore(String date) {
        return to != null && to.compareTo(date) < 0;
    }

    /** The rate as inherited from that many levels up. */
    public Rate at(int level) {
        return new Rate(sheetId, grade, pct, from, to, level, version, claimsAfter, prior == null ? null : prior.at(level));
    }

    /** The version of this rate that applies to the claim (by its id): this one, unless it is forward-only
     *  and the claim was added before it was saved, when the one it replaced does; null when none does. */
    public Rate forClaim(long claimId) {
        if (claimsAfter == null || claimId > claimsAfter) return this;
        return prior == null ? null : prior.forClaim(claimId);
    }

    /** This rate with the version it replaced. */
    Rate withPrior(Rate prior) {
        return new Rate(sheetId, grade, pct, from, to, level, version, claimsAfter, prior);
    }
}
