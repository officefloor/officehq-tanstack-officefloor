package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/**
 * A statement just taken: the person's due-total then (n/a when none), how many claims counted, and
 * the date it was taken as of (null: now).
 */
public record Statement(String dueTotal, int claims, LocalDate asOf) {
}
