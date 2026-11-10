package net.officefloor.hq.app.period;

import java.util.List;

/** The periods on a sheet, by from-date. */
public record PeriodsResponse(List<PeriodResponse> periods) {
}
