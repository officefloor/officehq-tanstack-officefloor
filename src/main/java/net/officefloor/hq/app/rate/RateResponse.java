package net.officefloor.hq.app.rate;

/** A rate as the screen sees it: {@code rate} has two decimals ("12.50"). */
public record RateResponse(String grade, String rate) {

    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.rate().toPlainString());
    }
}
