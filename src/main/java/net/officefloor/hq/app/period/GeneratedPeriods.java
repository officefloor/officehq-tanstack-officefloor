package net.officefloor.hq.app.period;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** The periods a generate makes, in date order. */
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

    /** The four calendar quarters of {@code year}, named yyyy-Q1 to yyyy-Q4, leaving out a quarter whose name is taken or that shares a day with an existing period. */
    public static GeneratedPeriods quarters(String sheet, List<Period> existing, int year) {
        List<Period> made = new ArrayList<>();
        for (int q = 1; q <= 4; q++) {
            YearMonth first = YearMonth.of(year, q * 3 - 2);
            Period quarter = new Period(sheet, year + "-Q" + q, first.atDay(1), first.plusMonths(2).atEndOfMonth());
            boolean clash = existing.stream().anyMatch(p -> p.name().equals(quarter.name()) || p.overlaps(quarter));
            if (!clash) {
                made.add(quarter);
            }
        }
        return new GeneratedPeriods(made);
    }
}
