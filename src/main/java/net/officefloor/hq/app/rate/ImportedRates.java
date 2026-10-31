package net.officefloor.hq.app.rate;

import java.util.List;

/** The checked lines of a rates import, in order, each as the rate form would send it and the rate it sets. */
public record ImportedRates(List<RateRequest> requests, List<Rate> rates) {
}
