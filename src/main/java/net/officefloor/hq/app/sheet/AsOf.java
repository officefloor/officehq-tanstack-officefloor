package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** The as-of date a screen reads the sheet as of (?asOf=yyyy-mm-dd): the sheet as it stood at the end of that day,
 *  every change made while the sheet's today was on or before it counting and every later one not, the date standing
 *  in for the sheet's today. */
public final class AsOf {

    private AsOf() {
    }

    /** The date a read is figured on: the as-of date when one is given, else the preview date (?preview=: the sheet's
     *  current records figured as if its today were that date), else the sheet's today (null). */
    public static LocalDate figuredOn(LocalDate end, String preview) {
        return end != null ? end : parse(preview);
    }

    /** The as-of date given (null: blank or not a date, the sheet as it is now). */
    public static LocalDate parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
