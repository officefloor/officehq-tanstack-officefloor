package net.officefloor.hq.app.rate;

import java.util.List;

/** The lines of a rate import, each passed by the rate form's rules, in order. */
public record ImportRateLines(List<ValidRate> lines) {
}
