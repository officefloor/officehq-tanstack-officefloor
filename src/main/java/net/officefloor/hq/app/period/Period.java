package net.officefloor.hq.app.period;

import java.util.Map;

/** A period on a sheet: its name and its from and to dates (yyyy-MM-dd), both inclusive, whether it
 *  is closed and, once closed, its due-total fixed at that moment (two decimals) and each person's
 *  due-total for it fixed then too, by person id (a person not in it shows 0.00). A soft-closed period
 *  ({@code soft}, not {@code closed}) has its figures fixed the same way, but claims due by a day in it can
 *  still be added and changed. */
public record Period(String sheetId, String name, String from, String to, boolean closed,
        String closedTotal, Map<Long, String> personTotals, boolean soft) {

    /** Whether its figures are fixed: closed or soft-closed. */
    public boolean fixed() {
        return closed || soft;
    }

    /** Whether the date (yyyy-MM-dd) falls inside it. */
    public boolean contains(String date) {
        return from.compareTo(date) <= 0 && date.compareTo(to) <= 0;
    }
}
