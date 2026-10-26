package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;

/** A fiscal year: named FY<yyyy> by the calendar year its first day falls in, starting on the first of its start month. */
public record FiscalYear(int year) {

    /** The fiscal year the date falls in, the year starting in the month given (1 to 12). */
    public static FiscalYear of(LocalDate date, int startMonth) {
        return new FiscalYear(date.getMonthValue() >= startMonth ? date.getYear() : date.getYear() - 1);
    }

    /** The fiscal year's first month, the year starting in the month given (1 to 12). */
    public YearMonth first(int startMonth) {
        return YearMonth.of(year, startMonth);
    }

    /** The fiscal year's last month: eleven after its first. */
    public YearMonth last(int startMonth) {
        return first(startMonth).plusMonths(11);
    }

    public String name() {
        return "FY" + year;
    }
}
