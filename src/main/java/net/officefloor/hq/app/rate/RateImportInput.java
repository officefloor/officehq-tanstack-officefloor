package net.officefloor.hq.app.rate;

import java.util.List;

/** A validated rate import: its rates, in order. */
public record RateImportInput(List<RateInput> rates) {
}
