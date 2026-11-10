package net.officefloor.hq.app.rate;

import java.util.List;

/** The imported rates, each checked by the rate form's rules, in the order pasted. */
public record ImportRatesLines(List<RateInput> lines) {
}
