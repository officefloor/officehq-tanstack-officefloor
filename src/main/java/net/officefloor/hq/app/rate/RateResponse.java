package net.officefloor.hq.app.rate;

/** A rate as sent to the screen: the percent as text with two decimals, so it is never rounded through a float. */
public record RateResponse(String sheetId, String grade, String rate) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.sheetId(), rate.grade(), rate.rate().toPlainString());
    }
}
