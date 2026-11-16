package net.officefloor.hq.app.person;

/** A person on a sheet: a name (unique on the sheet, case ignored; kept without surrounding spaces) and a grade, one of A, B or C.
 *  The version is 0 when added and goes up by one on each stored edit. {@code retiredFrom} (yyyy-MM-dd) is the date the
 *  person is retired from, null when not retired; a person with one counts as retired at once, whatever the date. */
public record Person(long id, String sheetId, String name, String grade, long version, String retiredFrom) {

    public boolean retired() {
        return retiredFrom != null;
    }

    /** Whether a claim of the person may be dated {@code date}: not after their retirement date. */
    public boolean mayClaimOn(java.time.LocalDate date) {
        return retiredFrom == null || !date.isAfter(java.time.LocalDate.parse(retiredFrom));
    }
}
