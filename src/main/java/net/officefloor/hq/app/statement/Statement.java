package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/**
 * A statement taken of a person: their due-total at that moment ({@code "n/a"} when they had none), how many
 * of their claims counted then, and the sheet's today when it was taken.
 */
public record Statement(String sheetId, long personId, String dueTotal, int claims, LocalDate statedOn) {
}
