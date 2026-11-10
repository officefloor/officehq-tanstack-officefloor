package net.officefloor.hq.app.rate;

/**
 * A rate with a grade other than A, B or C, a percent that is not 0.00 or more with two decimals,
 * a from-date or to-date that is not a date, or a to-date before the from-date.
 */
public class InvalidRateException extends Exception {
    public InvalidRateException(String message) {
        super(message);
    }
}
