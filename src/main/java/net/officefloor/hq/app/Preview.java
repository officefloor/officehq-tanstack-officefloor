package net.officefloor.hq.app;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * The preview date the signed-in user's screen reads the sheet at, named on each read in the
 * {@code X-Preview} header (yyyy-MM-dd). While it is set, a read answers with the sheet's current
 * records, that date standing in for the sheet's today. Changes never carry it: they use the sheet's
 * real today, and nothing a change of today does happens.
 */
public final class Preview {

    public static final String HEADER = "X-Preview";

    private Preview() {
    }

    /** The preview date the request reads at, or null when it reads at the sheet's today. */
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
