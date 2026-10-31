package net.officefloor.hq.app.period;

import java.util.List;

/** A rollover that passed validation: the old fiscal year's name (FY<yyyy>), its periods by from-date (at least
 *  one), and the new fiscal year's twelve months to generate. */
public record ValidRollover(String fiscalYear, List<PeriodResponse> periods, ValidGenerate next, String user) {
}
