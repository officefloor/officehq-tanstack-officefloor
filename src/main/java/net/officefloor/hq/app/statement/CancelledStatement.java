package net.officefloor.hq.app.statement;

import java.math.BigDecimal;

/** A statement just cancelled: the due-total it recorded (null: n/a). */
public record CancelledStatement(BigDecimal dueTotal) {
}
