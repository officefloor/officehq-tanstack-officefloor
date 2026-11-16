package net.officefloor.hq.app.rate;

/** A rate as sent to the screen: the percent as text with two decimals, so it is never rounded through a float;
 *  from is its from-date (yyyy-MM-dd), null when it applies from the beginning of time; to its to-date, null when it has no end. */
public record RateResponse(String sheetId, String grade, String rate, String from, String to) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.sheetId(), rate.grade(), rate.rate().toPlainString(),
                rate.from() == null ? null : rate.from().toString(), rate.to() == null ? null : rate.to().toString());
    }
}
