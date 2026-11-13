package net.officefloor.hq.app.rate;

import java.util.List;

/** The imported lines, in the order they were typed, each as the rate form's validation leaves it. */
public record ImportedRates(List<RateRequest> lines) {
}
