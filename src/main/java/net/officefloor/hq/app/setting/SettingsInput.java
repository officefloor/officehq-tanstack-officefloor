package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** Validated settings: the clerk limit scaled to two decimals, or null for no limit. */
public record SettingsInput(BigDecimal clerkLimit) {
}
