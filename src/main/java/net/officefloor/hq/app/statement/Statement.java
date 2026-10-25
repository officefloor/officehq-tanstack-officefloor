package net.officefloor.hq.app.statement;

import java.math.BigDecimal;

/** A statement just taken: the person's due-total then (null: n/a) and how many of their claims counted. */
public record Statement(BigDecimal dueTotal, int claims) {
}
