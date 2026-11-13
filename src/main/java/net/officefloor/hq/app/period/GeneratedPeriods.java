package net.officefloor.hq.app.period;

import java.util.List;

/** The monthly periods a generate makes, earliest first: those whose name and days are free on the sheet. */
public record GeneratedPeriods(List<Period> periods) {
}
