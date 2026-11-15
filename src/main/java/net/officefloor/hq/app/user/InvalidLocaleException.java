package net.officefloor.hq.app.user;

/** The locale chosen is not one the app knows, or there is no user: refused and nothing is written. */
public class InvalidLocaleException extends Exception {
    public InvalidLocaleException(String message) {
        super(message);
    }
}
