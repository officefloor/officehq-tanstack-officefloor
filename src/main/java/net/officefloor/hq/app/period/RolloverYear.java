package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.Set;

/** A validated rollover: the old fiscal year's name (FY<yyyy>), its first and last day, the new year's first day, and
 *  the names of the periods on the sheet before it (so the ones it generates are known). */
public record RolloverYear(String fy, LocalDate from, LocalDate to, LocalDate next, Set<String> before) {
}
