package net.officefloor.hq.app.rate;

import java.util.List;

/** The checked lines of an import of rates, in the order pasted, and who imports them. */
public record ImportedRates(List<RateValues> rates, String by) {
}
