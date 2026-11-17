package net.officefloor.hq.app.period;

import java.util.List;

/** How many periods were generated, and their ids. */
public record GeneratedPeriods(int made, List<Long> ids) {
}
