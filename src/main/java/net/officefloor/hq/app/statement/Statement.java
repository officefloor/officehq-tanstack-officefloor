package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A statement taken of a person: their name, due-total then (null: n/a), how many of their claims counted, and the
 *  date it was taken as of (null: taken now). */
public record Statement(String sheetId, String name, BigDecimal dueTotal, int claims, LocalDate asOf) {
}
