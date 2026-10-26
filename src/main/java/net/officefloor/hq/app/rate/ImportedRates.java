package net.officefloor.hq.app.rate;

import java.util.List;

/** The rate import's lines, validated, in the order they were typed. */
public record ImportedRates(List<RateInput> rates) {
}
