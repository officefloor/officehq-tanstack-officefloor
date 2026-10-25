package net.officefloor.hq.app.opening;

import java.util.List;

/** Every line of the opening balances paste box, in order. */
public record OpeningLines(List<OpeningLine> lines) {
}
