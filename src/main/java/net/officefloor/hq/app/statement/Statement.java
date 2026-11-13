package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/**
 * A statement just taken of a person: their due-total at that moment (n/a with none), how many claims counted, and
 * the date it was taken as of (null: none given, now).
 */
public record Statement(String name, String dueTotal, int claims, LocalDate asOf) {
}
