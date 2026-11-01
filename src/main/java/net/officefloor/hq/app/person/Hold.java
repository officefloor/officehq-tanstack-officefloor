package net.officefloor.hq.app.person;

/** One of a person's holds: from its from-date to its to-date, both included (yyyy-mm-dd; to is null when it never ends). */
public record Hold(String from, String to) {

    /** Whether the date (yyyy-mm-dd) is within this hold. */
    public boolean covers(String date) {
        return date.compareTo(from) >= 0 && (to == null || date.compareTo(to) <= 0);
    }
}
