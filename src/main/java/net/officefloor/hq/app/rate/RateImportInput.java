package net.officefloor.hq.app.rate;

import java.util.List;

/** A validated import of rates: its rate changes in the order given. */
public record RateImportInput(List<RateInput> rates) {
}
