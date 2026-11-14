package net.officefloor.hq.app.opening;

import java.util.List;

/** The opening lines, in the order they were typed, each with its person and period found. */
public record FoundOpenings(List<PastedOpening> lines) {
}
