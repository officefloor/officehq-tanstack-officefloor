package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated rollover: the old fiscal year's name (FY<yyyy>), its first and last day, and the new year's first day. */
public record RolloverYear(String fy, LocalDate from, LocalDate to, LocalDate next) {
}
