package net.officefloor.hq.app.rate;

/** The rate's grade is not one of A, B or C, or its percent is not 0.00 or more with up to two decimals. */
public class InvalidRateException extends Exception {
    public InvalidRateException(String message) {
        super(message);
    }
}
