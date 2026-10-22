package net.officefloor.hq.app.rate;

/** A rate as the screen shows it: the grade, the percent with two decimals, and the from-date
 *  (yyyy-mm-dd, or null when it applies from the beginning of time). */
public record RateResponse(String grade, String rate, String from) {
    public static RateResponse from(Rate r) {
        return new RateResponse(r.grade(), r.pct().toPlainString(), r.from() != null ? r.from().toString() : null);
    }
}
