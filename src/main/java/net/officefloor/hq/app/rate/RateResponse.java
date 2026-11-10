package net.officefloor.hq.app.rate;

/** A rate, as the screen reads it: its grade, its percent with two decimals, its from-date and its to-date ('' for none). */
public record RateResponse(String grade, String pct, String from, String to) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.pct().toPlainString(),
                rate.from() == null ? "" : rate.from().toString(),
                rate.to() == null ? "" : rate.to().toString());
    }
}
