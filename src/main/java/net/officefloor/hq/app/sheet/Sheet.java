package net.officefloor.hq.app.sheet;

import java.time.LocalDate;

/**
 * A sheet, identified by the id a user types. Everything else in the app is kept per sheet.
 * {@code today} is the sheet's own date: every decision that depends on the date, and every
 * default date, uses it and never the real clock.
 */
public record Sheet(String id, LocalDate today) {

    public Sheet withToday(LocalDate today) {
        return new Sheet(id, today);
    }
}
