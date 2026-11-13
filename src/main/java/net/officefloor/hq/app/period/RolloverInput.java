package net.officefloor.hq.app.period;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/** A validated rollover: the old fiscal year (yyyy), its periods on the sheet by from-date, and the first and
 *  last months of the new year, and the names of every period on the sheet before it (so the periods it generated
 *  can be told). */
public record RolloverInput(int year, List<PeriodResponse> periods, YearMonth from, YearMonth to, String user,
        Set<String> before) {
}
