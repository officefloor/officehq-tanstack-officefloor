package net.officefloor.hq.app.holiday;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/** Business days: every day that is not a Saturday, a Sunday or one of the sheet's holidays. */
public final class BusinessDays {

    private BusinessDays() {
    }

    /** The terms-th business day after a date, counting forward from the day after it; the date itself for no terms. */
    public static LocalDate after(LocalDate from, int terms, Set<LocalDate> holidays) {
        LocalDate day = from;
        for (int left = terms; left > 0;) {
            day = day.plusDays(1);
            if (isBusinessDay(day, holidays)) {
                left--;
            }
        }
        return day;
    }

    public static boolean isBusinessDay(LocalDate day, Set<LocalDate> holidays) {
        return day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY
                && !holidays.contains(day);
    }
}
