package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/**
 * Validated settings, each scaled to two decimals: whether the clerk limit is changed and to what
 * (null: no limit), and the floor (null: left as it is).
 */
public record SettingsInput(boolean clerkLimitChanged, BigDecimal clerkLimit, BigDecimal floor) {
}
