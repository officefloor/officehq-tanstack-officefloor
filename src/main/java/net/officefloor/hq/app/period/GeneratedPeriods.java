package net.officefloor.hq.app.period;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** The month periods a generate makes, in month order. */
public record GeneratedPeriods(List<Period> periods) {

    /** A period per month from {@code first} to {@code last}, each named yyyy-mm, leaving out a month whose name is taken or that shares a day with an existing period. */
    public static GeneratedPeriods months(String sheet, List<Period> existing, YearMonth first, YearMonth last) {
        List<Period> made = new ArrayList<>();
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            Period month = new Period(sheet, m.toString(), m.atDay(1), m.atEndOfMonth());
            boolean clash = existing.stream().anyMatch(p -> p.name().equals(month.name()) || p.overlaps(month));
            if (!clash) {
                made.add(month);
            }
        }
        return new GeneratedPeriods(made);
    }
}
