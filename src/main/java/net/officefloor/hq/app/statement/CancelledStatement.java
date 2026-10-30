package net.officefloor.hq.app.statement;

import java.math.BigDecimal;

/** A statement cancelled: its due-total (null when it was n/a). */
public record CancelledStatement(BigDecimal dueTotal) {
}
