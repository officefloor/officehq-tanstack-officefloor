package net.officefloor.hq.app.person;

import java.time.LocalDate;

/**
 * A person on a sheet: name (unique on the sheet, ignoring case) and grade (A, B or C). The version
 * is raised by one on every save. The retired-from date (yyyy-mm-dd) is null unless they are retired;
 * once it is set they count as retired, whatever the date.
 */
public record Person(long id, String name, String grade, long version, String retiredFrom) {

    /** Whether a claim dated {@code date} is after this person's retirement, and so refused. */
    public boolean retiredBefore(LocalDate date) {
        return retiredFrom != null && date.isAfter(LocalDate.parse(retiredFrom));
    }
}
