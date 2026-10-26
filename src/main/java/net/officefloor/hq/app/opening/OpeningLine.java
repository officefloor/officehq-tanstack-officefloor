package net.officefloor.hq.app.opening;

import java.math.BigDecimal;

/** One pasted opening balance line as typed: the person's name or alias, the period's name and the amount. */
public record OpeningLine(int line, String name, String period, BigDecimal amount) {
}
