package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/**
 * A statement taken of a person: their due-total at that moment ({@code "n/a"} when they had none), how many
 * of their claims counted then, and the sheet's today when it was taken, or the date it was taken as of ({@code asOf}, null
 * when it was taken now).
 */
public record Statement(String sheetId, long personId, String dueTotal, int claims, LocalDate statedOn, LocalDate asOf) {
}
