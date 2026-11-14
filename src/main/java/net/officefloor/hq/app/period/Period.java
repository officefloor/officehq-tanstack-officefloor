package net.officefloor.hq.app.period;

/** A period on a sheet: its name and its from and to dates (yyyy-MM-dd), both inclusive, whether it
 *  is closed and, once closed, its due-total fixed at that moment (two decimals). */
public record Period(String sheetId, String name, String from, String to, boolean closed,
        String closedTotal) {

    /** Whether the date (yyyy-MM-dd) falls inside it. */
    public boolean contains(String date) {
        return from.compareTo(date) <= 0 && date.compareTo(to) <= 0;
    }
}
