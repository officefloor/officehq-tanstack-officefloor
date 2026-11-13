package net.officefloor.hq.app.sheet;

import java.time.LocalDate;

/**
 * A sheet: the unit everything else in the app is kept under. {@code today} is the sheet's own
 * date — every date-dependent decision and every default date uses it, never the real clock.
 */
public record Sheet(String id, LocalDate today) {
}
