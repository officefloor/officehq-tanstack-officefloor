package net.officefloor.hq.app.person;

/** One of a person's past retirements: from its retired-from date to the day they were unretired (yyyy-mm-dd). */
public record Retirement(String from, String to) {

    /** Whether the date (yyyy-mm-dd) is after its from and before its to, and so refused for a claim. */
    public boolean within(String date) {
        return date.compareTo(from) > 0 && date.compareTo(to) < 0;
    }
}
