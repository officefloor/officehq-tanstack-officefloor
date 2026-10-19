package net.officefloor.hq.app.rate;

/** A grade's rate as the screen shows it: the percent with two decimals, and its from-date
 *  (yyyy-MM-dd, null when it applies from the beginning of time). */
public record RateResponse(String grade, String pct, String from) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.pct().toPlainString(),
                rate.from() == null ? null : rate.from().toString());
    }
}
