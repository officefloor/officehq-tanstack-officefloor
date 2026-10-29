package net.officefloor.hq.app.rate;

import java.util.List;

/** Every line of an import of rates, checked by the rate form's rules, in the order typed. */
public record ImportedRates(List<Rate> rates) {
}
