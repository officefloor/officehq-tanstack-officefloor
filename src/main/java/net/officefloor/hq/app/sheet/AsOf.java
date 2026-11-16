package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * The as-of date of a read: a screen showing the sheet as it stood at the end of a date sends it as the {@code asOf}
 * query parameter of every read (and the {@value #HEADER} header of every change, which is refused while it is set,
 * see ViewerGuard). Every change made while the sheet's today was on or before it counts, every later one does not,
 * and it stands in for the sheet's today in every figure.
 */
public final class AsOf {

    /** The header a change carries while its screen shows the sheet as of a date. */
    public static final String HEADER = "X-As-Of";

    private AsOf() {
    }

    /** The as-of date given (yyyy-MM-dd), or null when none (or none readable) was: the sheet as it is now. */
    public static LocalDate parse(String asOf) {
        if (asOf == null || asOf.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(asOf.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Whether a change made on the date (the sheet's today then; null: before that was kept) counts as of the date. */
    public static boolean counts(LocalDate madeOn, LocalDate asOf) {
        return madeOn == null || !madeOn.isAfter(asOf);
    }
}
