package net.officefloor.hq.app.rate;

import java.util.List;

/** An import whose every line passed the rate form's rules, in the order pasted. */
public record ValidImportRates(List<ValidRate> rates, String user) {
}
