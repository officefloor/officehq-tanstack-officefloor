package net.officefloor.hq.app.history;

import java.time.LocalDate;

/**
 * Reading a sheet previewed at a date ({@link PreviewFilter}): the sheet's current records, with the date standing in
 * for the sheet's today. Only reads are previewed; a change always uses the sheet's real today.
 */
public final class Preview {

    private static final ThreadLocal<LocalDate> DATE = new ThreadLocal<>();

    private Preview() {
    }

    /** Previews reads at {@code date} (the real today when null) until set back; returns the date previewed before. */
    static LocalDate set(LocalDate date) {
        LocalDate was = DATE.get();
        DATE.set(date);
        return was;
    }

    /** The date reads are previewed at; null when they use the sheet's real today. */
    public static LocalDate date() {
        return DATE.get();
    }
}
