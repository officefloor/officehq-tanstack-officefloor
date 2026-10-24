package net.officefloor.hq.app.importrates;

import java.util.List;
import net.officefloor.hq.app.rate.RateInput;

/** The rate import box's lines, each checked by the rate form's rules, in order. */
public record ImportedRates(List<RateInput> rates) {
}
