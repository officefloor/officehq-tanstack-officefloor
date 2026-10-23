package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** A person's plan: a DRAFT claim of the amount for every plan date — the start, then every `every` days — up to
 *  the end, made when the plans are generated; `generated` is how many claims it has made so far. A monthly plan has
 *  no `every` but a `day` of the month (1 to 31): its plan dates are that day in every month from the start's month, a
 *  day beyond the month's length falling on its last day, dates before the start left out. */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, Integer every, Integer day,
        LocalDate end, int generated) {

    /** The plan's dates, in order, up to its end and the given last day. */
    public List<LocalDate> dates(LocalDate last) {
        List<LocalDate> dates = new ArrayList<>();
        if (day == null) {
            for (LocalDate date = start; !date.isAfter(end) && !date.isAfter(last); date = date.plusDays(every)) {
                dates.add(date);
            }
            return dates;
        }
        for (YearMonth month = YearMonth.from(start);; month = month.plusMonths(1)) {
            LocalDate date = month.atDay(Math.min(day, month.lengthOfMonth()));
            if (date.isAfter(end) || date.isAfter(last)) {
                return dates;
            }
            if (!date.isBefore(start)) {
                dates.add(date);
            }
        }
    }

    /** The first `count` plan dates after the given today and on or before the end, in order. */
    public List<LocalDate> nextDates(LocalDate today, int count) {
        List<LocalDate> next = new ArrayList<>();
        for (LocalDate date : dates(end)) {
            if (date.isAfter(today) && next.size() < count) {
                next.add(date);
            }
        }
        return next;
    }
}
