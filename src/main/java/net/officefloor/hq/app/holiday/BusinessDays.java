package net.officefloor.hq.app.holiday;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/** Counting business days: every day that is not a Saturday, a Sunday or one of the sheet's holidays. */
public final class BusinessDays {

    private BusinessDays() {
    }

    /** The terms-th business day after the date, counting forward from the day after it (0 terms: the date itself). */
    public static LocalDate after(LocalDate from, int terms, Set<LocalDate> holidays) {
        LocalDate day = from;
        for (int n = 0; n < terms;) {
            day = day.plusDays(1);
            if (isBusinessDay(day, holidays)) {
                n++;
            }
        }
        return day;
    }

    public static boolean isBusinessDay(LocalDate day, Set<LocalDate> holidays) {
        return day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY
                && !holidays.contains(day);
    }
}
