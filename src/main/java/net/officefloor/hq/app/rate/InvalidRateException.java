package net.officefloor.hq.app.rate;

/** A rate set with an unknown grade, a rate that is not a percent with two decimals, or no signed-in user. */
public class InvalidRateException extends Exception {
    public InvalidRateException(String message) {
        super(message);
    }
}
