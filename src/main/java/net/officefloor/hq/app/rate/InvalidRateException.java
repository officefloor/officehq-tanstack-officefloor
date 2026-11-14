package net.officefloor.hq.app.rate;

/** The grade is not A, B or C, or the percent is missing, negative or has more than two decimals, or the from-date is not a date: refused and nothing is written. */
public class InvalidRateException extends Exception {
    public InvalidRateException(String message) {
        super(message);
    }
}
