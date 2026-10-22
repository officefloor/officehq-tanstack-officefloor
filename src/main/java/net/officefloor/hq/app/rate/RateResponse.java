package net.officefloor.hq.app.rate;

/** A rate as the screen shows it: the grade, the percent with two decimals, and the from-date
 *  (yyyy-mm-dd, or null when it applies from the beginning of time) and the to-date (yyyy-mm-dd, or null when it has no end), and the level it comes from (0: the sheet's own; 1: its parent's,
 *  2: the parent's parent's, and so on). */
public record RateResponse(String grade, String rate, String from, String to, int level) {
    public static RateResponse from(Rate r) {
        return from(r, 0);
    }

    public static RateResponse from(Rate r, int level) {
        return new RateResponse(r.grade(), r.pct().toPlainString(), r.from() != null ? r.from().toString() : null,
                r.to() != null ? r.to().toString() : null, level);
    }
}
