package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A statement taken of a person: their due-total then (null when it was n/a), how many of their
 *  claims counted, and the sheet's today when it was taken. */
public record Statement(long personId, LocalDate takenOn, BigDecimal dueTotal, int claims) {
}
