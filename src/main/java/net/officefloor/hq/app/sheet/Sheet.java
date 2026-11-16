package net.officefloor.hq.app.sheet;

import java.time.LocalDate;

/**
 * A sheet: everything else in the app is kept per sheet, keyed by its id. {@code today} is the
 * sheet's own date — every date decision and default date on the sheet uses it, never the clock.
 * An {@code archived} sheet stays visible and refuses every change. {@code parent} is the id of its parent sheet
 * (whose rates apply where it has none), null when none — always as it is now, even read as of a date.
 */
public record Sheet(String id, LocalDate today, boolean archived, String parent) {
}
