package net.officefloor.hq.app.rate;

/** The rate form was refused: a grade other than A, B or C, or a percent that is not 0.00 to 100.00
 *  with up to two decimals. */
public class InvalidRateException extends Exception {
    public InvalidRateException(String message) {
        super(message);
    }
}
