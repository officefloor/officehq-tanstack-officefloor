package net.officefloor.hq.app.period;

import java.util.List;

/** The checked lines of an openings paste, in order. */
public record ImportedOpenings(List<OpeningLine> lines) {
}
