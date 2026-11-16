package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/** A statement taken for a person: its date (the sheet's today when taken, or the date it was taken as of), the
 *  person's due-total then (two decimals, or n/a when their grade had no rate and no claim kept a figure), how many of
 *  their claims counted, who took it, the date it was taken as of (null: taken on the sheet's today) and the sheet's
 *  today when it was taken. */
public record Statement(String sheetId, long personId, LocalDate takenOn, String dueTotal, int claims, String by,
        LocalDate asOf, LocalDate madeOn) {

    /** A statement taken on the sheet's today. */
    public Statement(String sheetId, long personId, LocalDate takenOn, String dueTotal, int claims, String by) {
        this(sheetId, personId, takenOn, dueTotal, claims, by, null, takenOn);
    }
}
