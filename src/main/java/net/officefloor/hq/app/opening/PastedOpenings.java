package net.officefloor.hq.app.opening;

import java.util.List;

/** The opening lines, in the order they were typed. */
public record PastedOpenings(List<PastedOpening> lines) {
}
