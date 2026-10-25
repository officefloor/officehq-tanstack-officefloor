package net.officefloor.hq.app.opening;

import java.math.BigDecimal;

/** One pasted opening balance line once checked: the trimmed name and period, and the amount (two decimals). */
public record OpeningLine(String name, String period, BigDecimal amount) {
}
