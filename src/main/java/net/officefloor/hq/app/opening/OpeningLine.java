package net.officefloor.hq.app.opening;

import java.math.BigDecimal;

/** One pasted line as written: its line number, the name or alias, the period name and the amount. */
public record OpeningLine(int number, String name, String period, BigDecimal amount) {
}
