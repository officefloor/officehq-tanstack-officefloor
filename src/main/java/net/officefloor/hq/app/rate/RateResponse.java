package net.officefloor.hq.app.rate;

/** A rate as the screen shows it; level 0 is the sheet's own, 1 its parent's, and so on up; version is how many
 * versions it has had. */
public record RateResponse(String grade, String rate, String from, String to, int level, int version) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.rate().toPlainString(),
                rate.from() == null ? null : rate.from().toString(),
                rate.to() == null ? null : rate.to().toString(), rate.level(), rate.version());
    }
}
