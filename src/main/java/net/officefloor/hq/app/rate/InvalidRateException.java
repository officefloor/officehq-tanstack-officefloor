package net.officefloor.hq.app.rate;

/** The rate's grade is not one of A, B or C, or its percent is not 0.00 or more with up to two decimals, or its from- or to-date is not a date, or its to-date is before its from-date. */
public class InvalidRateException extends Exception {
    public InvalidRateException(String message) {
        super(message);
    }
}
