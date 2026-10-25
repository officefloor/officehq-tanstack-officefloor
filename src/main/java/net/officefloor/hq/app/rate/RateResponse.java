package net.officefloor.hq.app.rate;

public record RateResponse(String grade, String rate, String from) {
    public static RateResponse from(Rate rate) {
        return new RateResponse(rate.grade(), rate.rate().toPlainString(),
                rate.from() == null ? null : rate.from().toString());
    }
}
