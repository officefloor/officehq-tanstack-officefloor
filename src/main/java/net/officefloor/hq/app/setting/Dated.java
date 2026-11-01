package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;

/**
 * A figure in force by date: on a date, the value of the latest from-date on or before it, and before every from-date
 * its starting value.
 */
public record Dated(BigDecimal start, NavigableMap<LocalDate, BigDecimal> values) {

    /** The value in force on the date. */
    public BigDecimal on(LocalDate date) {
        Map.Entry<LocalDate, BigDecimal> found = values.floorEntry(date);
        return found == null ? start : found.getValue();
    }

    /** The value in force on the date (yyyy-mm-dd). */
    public BigDecimal on(String date) {
        return on(LocalDate.parse(date));
    }
}
