package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A period as the front end reads it: its name and its from and to dates (yyyy-MM-dd, inclusive). */
public record PeriodResponse(String name, String from, String to) {
    public static PeriodResponse of(String name, LocalDate from, LocalDate to) {
        return new PeriodResponse(name, from.toString(), to.toString());
    }
}
