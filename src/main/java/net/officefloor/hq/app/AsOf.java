package net.officefloor.hq.app;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * The as-of date the signed-in user's screen reads the sheet at, named on each read in the
 * {@code X-As-Of} header (yyyy-MM-dd). While it is set, a read answers with the sheet as it stood at
 * the end of that date: every change made while the sheet's today was on or before it, and that
 * date standing in for the sheet's today. Changes never carry it.
 */
public final class AsOf {

    public static final String HEADER = "X-As-Of";

    private AsOf() {
    }

    /** The as-of date the request reads at, or null when it reads the sheet as it is now. */
    public static LocalDate of(HttpServletRequest request) {
        String raw = request == null ? null : request.getHeader(HEADER);
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
