package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A statement taken: the person's due-total at that moment (null when n/a), how many claims counted, and the date it
 *  was taken as of (null when taken now). */
public record Statement(BigDecimal dueTotal, int claims, LocalDate asOf) {
}
