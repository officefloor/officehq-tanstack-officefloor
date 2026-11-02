package net.officefloor.hq.app.settings;

import java.math.BigDecimal;

/** A validated settings form: the clerk limit scaled to two decimals, null for no limit. */
public record SettingsInput(BigDecimal clerkLimit) {
}
