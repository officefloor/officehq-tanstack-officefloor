package net.officefloor.hq.app.rate;

/** A rate with a grade other than A, B or C, or a percent that is not 0.00 or more with two decimals. */
public class InvalidRateException extends Exception {
    public InvalidRateException(String message) {
        super(message);
    }
}
