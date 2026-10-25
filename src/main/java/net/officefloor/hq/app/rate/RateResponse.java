package net.officefloor.hq.app.rate;

public record RateResponse(String grade, String rate) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.rate().toPlainString());
    }
}
