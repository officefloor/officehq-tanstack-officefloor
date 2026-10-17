package net.officefloor.hq.app.rate;

/** A grade's rate as shown: a percent with two decimals, from its from-date (null: from the start). */
public record RateResponse(String grade, String rate, String from) {

    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.getGrade(), rate.getRate().setScale(2).toPlainString(),
                rate.getFromDate() == null ? null : rate.getFromDate().toString());
    }
}
