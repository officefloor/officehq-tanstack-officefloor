package net.officefloor.hq.app.rate;

/** The requested rate is not valid (an unknown grade, or not a percent of 0.00 or more); nothing is changed. */
public class InvalidRateException extends Exception {
    public InvalidRateException(String message) {
        super(message);
    }
}
