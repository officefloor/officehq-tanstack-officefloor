package net.officefloor.hq.app.rate;

import java.time.LocalDate;

/** A grade and from-date whose newest rate version was withdrawn; {@code restored} is the version that applies again, null when none is left. */
public record WithdrawnRate(String sheetId, String grade, LocalDate from, Rate restored) {
}
