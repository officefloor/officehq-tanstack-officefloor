package net.officefloor.hq.app.rate;

import java.util.List;

/** The rates of a parent sheet as the screen sees them: `level` levels up (1 for the parent). */
public record InheritedRatesResponse(int level, String sheet, List<RateResponse> rates) {
    public static InheritedRatesResponse from(Rates.Inherited inherited) {
        return new InheritedRatesResponse(inherited.level(), inherited.sheetId(),
                inherited.rates().stream().map(RateResponse::from).toList());
    }
}
