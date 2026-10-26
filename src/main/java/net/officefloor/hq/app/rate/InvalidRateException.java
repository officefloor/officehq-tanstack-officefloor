package net.officefloor.hq.app.rate;

/** A rate was given for a grade that is not A, B or C, or was not a percent of 0.00 or more with up to two decimals. */
public class InvalidRateException extends Exception {

    public InvalidRateException(String message) {
        super(message);
    }
}
