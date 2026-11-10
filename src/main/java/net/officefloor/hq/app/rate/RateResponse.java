package net.officefloor.hq.app.rate;

/** A rate, as the screen reads it: its grade, its percent with two decimals, and its from-date ('' for none). */
public record RateResponse(String grade, String pct, String from) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.pct().toPlainString(),
                rate.from() == null ? "" : rate.from().toString());
    }
}
