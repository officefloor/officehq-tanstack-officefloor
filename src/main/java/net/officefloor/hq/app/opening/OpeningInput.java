package net.officefloor.hq.app.opening;

import java.util.List;

/** A bound opening balances paste: its lines, in order. */
public record OpeningInput(List<OpeningLine> lines) {
}
