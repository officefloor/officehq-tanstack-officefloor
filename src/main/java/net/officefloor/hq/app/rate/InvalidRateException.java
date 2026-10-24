package net.officefloor.hq.app.rate;

/** A rate form that cannot be saved: a grade other than A, B or C, or a percent that is missing, negative or has more than two decimals,
 *  or a to-date before its from-date. */
public class InvalidRateException extends Exception {
    public InvalidRateException(String message) {
        super(message);
    }
}
