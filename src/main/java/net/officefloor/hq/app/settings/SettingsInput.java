package net.officefloor.hq.app.settings;

import java.math.BigDecimal;

/** A validated settings form: which settings it saves, the clerk limit scaled to two decimals (null for
 *  no limit) and the floor scaled to two decimals. */
public record SettingsInput(boolean setsClerkLimit, BigDecimal clerkLimit, boolean setsFloor, BigDecimal floor) {
}
