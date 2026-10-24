package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A validated settings form: whether the clerk limit is set and to what (two decimals; null: no limit), and
 *  whether the floor is set and to what (two decimals). */
public record SettingsInput(boolean setClerkLimit, BigDecimal clerkLimit, boolean setFloor, BigDecimal floor) {
}
