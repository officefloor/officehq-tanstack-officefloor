package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A validated settings save: who is making it and the clerk limit (two decimals; null: no limit). */
public record SettingsInput(String user, BigDecimal clerkLimit) {
}
