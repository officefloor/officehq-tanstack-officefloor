package net.officefloor.hq.app.person;

import java.util.List;

/** One of a person's holds: from its from-date to its to-date (yyyy-mm-dd, both inclusive; a null
 *  from-date is the beginning of time, a null to-date open-ended). */
public record Hold(String from, String to) {

    /** Whether the hold covers this date (yyyy-mm-dd). */
    public boolean covers(String date) {
        return (from == null || from.compareTo(date) <= 0) && (to == null || to.compareTo(date) >= 0);
    }

    /** Whether any of these holds covers this date. */
    public static boolean anyCovers(List<Hold> holds, String date) {
        return holds.stream().anyMatch(h -> h.covers(date));
    }
}
