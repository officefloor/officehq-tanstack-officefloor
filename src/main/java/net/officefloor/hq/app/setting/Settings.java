package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A sheet's settings: the clerk limit (two decimals; null: no limit) and the floor (two decimals). */
public record Settings(String sheetId, BigDecimal clerkLimit, BigDecimal floor) {
}
