package net.officefloor.hq.app.sheet;

/** A sheet's today was given as something that is not a real yyyy-MM-dd date. */
public class InvalidTodayException extends Exception {

    public InvalidTodayException(String message) {
        super(message);
    }
}
