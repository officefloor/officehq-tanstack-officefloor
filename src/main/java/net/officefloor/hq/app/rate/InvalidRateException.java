package net.officefloor.hq.app.rate;

/** A rate that cannot be set (an unknown grade, a percent that is not 0.00 or more with at most two
 *  decimals, a to-date before the from-date); nothing is written. */
public class InvalidRateException extends Exception {
    public InvalidRateException(String message) {
        super(message);
    }
}
