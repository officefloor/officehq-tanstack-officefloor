package net.officefloor.hq.app.setting;

import java.util.List;

/** A dated setting's values by from-date, and the value it has before every from-date ({@code start};
 *  null: none). */
public record DatedSetting(List<Setting> values, String start) {

    /** The value in force on the date (yyyy-MM-dd): the one with the latest from-date on or before
     *  it (none: from the beginning of time), else the starting value; null when blank or none. */
    public String on(String date) {
        Setting found = null;
        for (Setting s : values) {
            if (s.from() != null && s.from().compareTo(date) > 0) continue;
            if (found == null || nz(found.from()).compareTo(nz(s.from())) < 0) found = s;
        }
        if (found == null) return start;
        return found.value().isEmpty() ? null : found.value();
    }

    private static String nz(String from) {
        return from == null ? "" : from;
    }
}
