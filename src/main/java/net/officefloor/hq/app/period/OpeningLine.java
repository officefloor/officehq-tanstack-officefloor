package net.officefloor.hq.app.period;

import java.math.BigDecimal;

/** A pasted opening balance: the person's name (or alias), the period's name and the amount. */
public record OpeningLine(String name, String period, BigDecimal amount) {
}
