package net.officefloor.hq.app.patron;

/** The patron is not someone else on the sheet, or is already one of the person's patrons. */
public class InvalidPatronException extends Exception {
    public InvalidPatronException(String message) {
        super(message);
    }
}
