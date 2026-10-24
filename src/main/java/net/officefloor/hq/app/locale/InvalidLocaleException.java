package net.officefloor.hq.app.locale;

/** A locale other than en or de, or a blank user name. */
public class InvalidLocaleException extends Exception {
    public InvalidLocaleException(String message) {
        super(message);
    }
}
