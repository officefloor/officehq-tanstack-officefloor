package net.officefloor.hq.app.period;

import java.util.List;

/** A rollover once validated: the old fiscal year's name (FY<yyyy>) and its periods by from-date, and who rolls it
 *  over. The new year's dates are published apart, as GeneratePeriodsValues, for the period generator. */
public record RolloverValues(String fy, List<Period> oldPeriods, String by) {
}
