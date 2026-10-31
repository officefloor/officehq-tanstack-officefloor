package net.officefloor.hq.app.settings;

import java.math.BigDecimal;

/** A validated settings change on a sheet: whether the clerk limit is set and to what (null for no
 *  limit), the floor and the terms, each null when it is not being set. */
public record SettingsChange(String sheetId, boolean setsClerkLimit, BigDecimal clerkLimit, BigDecimal floor, Integer terms, String by) {
}
