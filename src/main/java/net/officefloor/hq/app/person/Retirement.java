package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A retirement once validated: the date (null when none was given, so the sheet's today) and who gives it. */
public record Retirement(LocalDate from, String by) {
}
