package net.officefloor.hq.app.user;

/** The column is not one the people list has, or there is no user or sheet: refused and nothing is written. */
public class InvalidColumnException extends Exception {
    public InvalidColumnException(String message) {
        super(message);
    }
}
