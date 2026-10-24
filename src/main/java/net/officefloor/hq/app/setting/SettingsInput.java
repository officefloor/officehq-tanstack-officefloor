package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A validated settings form: the clerk limit with two decimals (null: no limit). */
public record SettingsInput(BigDecimal clerkLimit) {
}
