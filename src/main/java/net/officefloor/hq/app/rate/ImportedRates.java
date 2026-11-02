package net.officefloor.hq.app.rate;

import java.util.List;

/** Every imported rate line, checked by the rate form's rules, in the order pasted. */
public record ImportedRates(List<RateInput> lines) {
}
