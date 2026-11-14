package net.officefloor.hq.app.rate;

/**
 * A rate as the screen shows it: the grade, the percent with two decimals the from-date (null: from the start), the
 * to-date (null: no end), the sheet it is set on (the sheet's own, or a parent's it inherits) and its version count;
 * whether it is forward-only, when it was saved (on the sequence a claim's added is on) and its version before (null: none).
 */
public record RateResponse(String grade, String rate, String from, String to, String sheet, int version, boolean forward, long saved,
        RateResponse earlier) {

    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.rate().setScale(2).toPlainString(),
                rate.from() == null ? null : rate.from().toString(),
                rate.to() == null ? null : rate.to().toString(), rate.sheetId(), rate.version(), rate.forward(),
                rate.saved(), rate.earlier() == null ? null : from(rate.earlier()));
    }
}
