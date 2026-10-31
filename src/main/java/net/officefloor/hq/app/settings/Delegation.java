package net.officefloor.hq.app.settings;

import java.time.LocalDate;

/** A validated delegation of the steward role on a sheet: to `name` from `from` to `to`
 *  (inclusive), by the steward `by`. */
public record Delegation(String sheetId, String name, LocalDate from, LocalDate to, String by) {
}
