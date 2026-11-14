package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.List;

/**
 * A version of the sheet's policy: every change to one of its policy settings (the rounding mode, the floor, the cap, the
 * terms, the late percent and the clerk limit) is a new one, numbered from 1 in the order made, in force from the
 * from-date it was saved with (null: none, it counts always).
 */
public record PolicyVersion(int number, String key, LocalDate from) {

    /** The version in force on the date: how many versions are from a date on or before it (one with none counts always). */
    public static int inForce(List<PolicyVersion> versions, LocalDate date) {
        return (int) versions.stream().filter((v) -> v.from() == null || (date != null && !v.from().isAfter(date))).count();
    }
}
