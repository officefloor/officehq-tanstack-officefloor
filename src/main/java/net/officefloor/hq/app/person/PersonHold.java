package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A hold on a person from a date until a date (both included); a null {@code to} is open-ended. */
public record PersonHold(LocalDate from, LocalDate to) {

    public boolean covers(LocalDate date) {
        return !date.isBefore(from) && (to == null || !date.isAfter(to));
    }
}
