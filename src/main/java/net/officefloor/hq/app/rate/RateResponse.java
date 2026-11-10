package net.officefloor.hq.app.rate;

/** A rate, as the screen reads it: its grade, its percent with two decimals, its from-date and its to-date ('' for none), and its level: 0 when set on the sheet itself, 1 on its parent and so on up. */
public record RateResponse(String grade, String pct, String from, String to, int level) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.pct().toPlainString(),
                rate.from() == null ? "" : rate.from().toString(),
                rate.to() == null ? "" : rate.to().toString(), rate.level());
    }
}
