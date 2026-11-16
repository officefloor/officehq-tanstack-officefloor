package net.officefloor.hq.app.opening;

import java.util.List;

/** The checked lines of a paste of opening balances, in order, and who pasted them. */
public record ImportedOpenings(List<OpeningLine> lines, String by) {
}
