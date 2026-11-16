package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/** A statement taken for a person: the sheet's today when taken, the person's due-total then (two decimals, or
 *  n/a when their grade had no rate and no claim kept a figure), how many of their claims counted, and who took it. */
public record Statement(String sheetId, long personId, LocalDate takenOn, String dueTotal, int claims, String by) {
}
