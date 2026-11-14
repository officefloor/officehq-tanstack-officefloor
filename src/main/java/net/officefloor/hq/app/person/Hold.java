package net.officefloor.hq.app.person;

/** A hold on a person: from its from-date (yyyy-MM-dd) to its to-date (null: open-ended), both included. */
public record Hold(String from, String to) {

    /** Whether the date (yyyy-MM-dd) falls within the hold. */
    public boolean covers(String date) {
        return from.compareTo(date) <= 0 && (to == null || date.compareTo(to) <= 0);
    }
}
