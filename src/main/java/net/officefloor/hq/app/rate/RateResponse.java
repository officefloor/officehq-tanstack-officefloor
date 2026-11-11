package net.officefloor.hq.app.rate;

/** A rate, as the screen reads it: its grade, its percent with two decimals, its from-date and its to-date ('' for none), its level: 0 when set on the sheet itself, 1 on its parent and so on up, and its version: how many times a rate has been set for its grade and from-date. */
public record RateResponse(String grade, String pct, String from, String to, int level, int version) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.pct().toPlainString(),
                rate.from() == null ? "" : rate.from().toString(),
                rate.to() == null ? "" : rate.to().toString(), rate.level(), rate.version());
    }
}
