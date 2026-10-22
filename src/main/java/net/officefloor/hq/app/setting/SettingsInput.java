package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A validated settings save: who is making it, and the settings it changes, in order — whether it sets the clerk
 *  limit and to what (two decimals; null: no limit), and the floor it sets (two decimals; null: left as it is). */
public record SettingsInput(String user, boolean setsClerkLimit, BigDecimal clerkLimit, BigDecimal floor) {
}
