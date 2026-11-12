package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/** Body of a statement: who takes it, and the date it is taken as of (yyyy-MM-dd; blank or absent takes it
 *  on the sheet's today, as of now). */
public record StatementRequest(String user, String asOf) {

    /** The date the statement is taken as of; null when none was given. Throws when it is not a date. */
    public LocalDate asOfDate() {
        return asOf == null || asOf.isBlank() ? null : LocalDate.parse(asOf.trim());
    }
}
