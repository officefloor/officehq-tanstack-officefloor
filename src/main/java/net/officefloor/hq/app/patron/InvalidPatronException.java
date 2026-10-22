package net.officefloor.hq.app.patron;

/** A patron that may not be named: no signed-in user, no such person on the sheet, the person themselves, or
 *  one already named. */
public class InvalidPatronException extends Exception {
    public InvalidPatronException(String message) {
        super(message);
    }
}
