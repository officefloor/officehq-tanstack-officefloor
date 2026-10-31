package net.officefloor.hq.app.person;

import java.math.BigDecimal;

/** A statement taken for a person: their due-total then (null when n/a) and how many of their
 *  claims counted, by whom. */
public record Statement(BigDecimal dueTotal, int claims, String user) {
}
