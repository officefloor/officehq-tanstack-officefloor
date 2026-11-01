package net.officefloor.hq.app.period;

import java.util.List;

/** How many periods a generate made, with every period on the sheet. */
public record GeneratedPeriods(int made, List<Period> periods) {
}
