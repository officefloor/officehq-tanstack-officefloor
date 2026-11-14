package net.officefloor.hq.app.rate;

/**
 * A rate as the screen shows it: the grade, the percent with two decimals the from-date (null: from the start), the
 * to-date (null: no end), the sheet it is set on (the sheet's own, or a parent's it inherits) and its version count.
 */
public record RateResponse(String grade, String rate, String from, String to, String sheet, int version) {

    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.rate().setScale(2).toPlainString(),
                rate.from() == null ? null : rate.from().toString(),
                rate.to() == null ? null : rate.to().toString(), rate.sheetId(), rate.version());
    }
}
