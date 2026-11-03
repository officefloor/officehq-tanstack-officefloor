package net.officefloor.hq.app.opening;

import java.math.BigDecimal;

/** A bound opening balance line: the name (or alias) and period name as given (trimmed), and its amount. */
public record OpeningLine(int line, String name, String period, BigDecimal amount) {
}
