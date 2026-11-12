package net.officefloor.hq.app.period;

import java.time.YearMonth;
import java.util.List;

/** A validated rollover: the old fiscal year (yyyy), its periods on the sheet by from-date, and the first and
 *  last months of the new year. */
public record RolloverInput(int year, List<PeriodResponse> periods, YearMonth from, YearMonth to, String user) {
}
