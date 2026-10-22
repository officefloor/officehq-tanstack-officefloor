package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A sheet's settings: the clerk limit (two decimals; null: no limit) the floor (two decimals) and the terms
 *  (days; a claim's due-by is its date plus them). */
public record Settings(String sheetId, BigDecimal clerkLimit, BigDecimal floor, int terms) {
}
