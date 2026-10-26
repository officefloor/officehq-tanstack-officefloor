package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A person's plan: an amount, a start date, every N days and an end date (not before the start), and how many claims it
 * has made so far.
 */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, int every, LocalDate end,
        int generated) {

    /** Its dates (the start, then every N days) up to the end and {@code until}, earliest first. */
    public List<LocalDate> datesThrough(LocalDate until) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate last = end.isBefore(until) ? end : until;
        for (LocalDate d = start; !d.isAfter(last); d = d.plusDays(every)) {
            dates.add(d);
        }
        return dates;
    }
}
