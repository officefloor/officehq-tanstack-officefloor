package net.officefloor.hq.app.holiday;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/** Terms count business days: every day but a Saturday, a Sunday and a holiday on the sheet. */
public final class BusinessDays {

    private BusinessDays() {
    }

    public static boolean isBusinessDay(LocalDate day, Set<LocalDate> holidays) {
        DayOfWeek dow = day.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(day);
    }

    /** A claim's due-by: the {@code terms}-th business day after its date, counting forward from the
     *  day after it (terms 0: the date itself). */
    public static LocalDate dueBy(LocalDate date, int terms, Set<LocalDate> holidays) {
        LocalDate due = date;
        for (int left = terms; left > 0; ) {
            due = due.plusDays(1);
            if (isBusinessDay(due, holidays)) left--;
        }
        return due;
    }
}
