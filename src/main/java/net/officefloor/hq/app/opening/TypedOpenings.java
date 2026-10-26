package net.officefloor.hq.app.opening;

import java.util.List;

/** The opening balance paste's lines, parsed, in the order they were typed. */
public record TypedOpenings(List<OpeningLine> lines) {
}
