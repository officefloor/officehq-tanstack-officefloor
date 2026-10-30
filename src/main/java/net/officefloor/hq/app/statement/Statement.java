package net.officefloor.hq.app.statement;

import java.math.BigDecimal;

/** A statement taken: the person's due-total at that moment (null when n/a) and how many claims counted. */
public record Statement(BigDecimal dueTotal, int claims) {
}
