package net.officefloor.hq.app.rate;

import java.util.List;

/** A sheet's rates, by grade; a grade with no rate is left out. */
public record RatesResponse(List<RateResponse> rates) {

    public static RatesResponse from(List<Rate> rates) {
        return new RatesResponse(rates.stream().map(RateResponse::from).toList());
    }
}
