package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A statement just taken: the person's due-total then (null: n/a), how many of their claims counted, and the date it
 *  was taken as of (null: none given, taken now). */
public record Statement(BigDecimal dueTotal, int claims, LocalDate asOf) {
}
