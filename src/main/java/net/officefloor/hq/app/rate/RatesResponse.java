package net.officefloor.hq.app.rate;

import java.util.List;

/** The rates set on a sheet, by grade then from-date (none first). */
public record RatesResponse(List<RateResponse> rates) {
}
