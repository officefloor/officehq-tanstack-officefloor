package net.officefloor.hq.app.period;

import java.util.List;

/** The pasted opening balances, each well formed, in the order pasted. */
public record ImportOpeningsLines(List<OpeningLine> lines) {
}
