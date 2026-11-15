package net.officefloor.hq.app.person;

/** A past retirement: from its from-date to the day the person was unretired (yyyy-MM-dd). */
public record Retirement(String from, String to) {

    /** Whether a claim on this date is refused by it: dated after its from and before its to. */
    public boolean refuses(java.time.LocalDate date) {
        return date.isAfter(java.time.LocalDate.parse(from)) && date.isBefore(java.time.LocalDate.parse(to));
    }
}
