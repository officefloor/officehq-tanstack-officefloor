package net.officefloor.hq.app.rate;

import java.util.List;

/** A validated rate import: who is making it, and each line's rate set, in the order pasted. */
public record RateImportInput(String user, List<RateInput> rates) {
}
