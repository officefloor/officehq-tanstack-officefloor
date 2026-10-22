package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A person's holds, in the order they were put on: each runs from its from-date (null: from the beginning of time)
 *  to its to-date (null: open-ended), both included. */
public record Holds(List<Hold> history) {

    /** A hold from a from-date to a to-date (either null: unbounded on that side). */
    public record Hold(LocalDate from, LocalDate to) {
        boolean covers(LocalDate date) {
            return (from == null || !from.isAfter(date)) && (to == null || !to.isBefore(date));
        }
    }

    public static final Holds NONE = new Holds(List.of());

    /** Whether the person has any hold. */
    public boolean any() {
        return !history.isEmpty();
    }

    /** Whether a hold covers the date. */
    public boolean covers(LocalDate date) {
        return history.stream().anyMatch(h -> h.covers(date));
    }
}
