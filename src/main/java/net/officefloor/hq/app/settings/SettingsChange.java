package net.officefloor.hq.app.settings;

import java.math.BigDecimal;

/** A validated settings change on a sheet: the clerk limit, null for no limit. */
public record SettingsChange(String sheetId, BigDecimal clerkLimit, String by) {
}
