package net.officefloor.hq.app.locale;

/** The locale chosen is not en or de. */
public class InvalidLocaleException extends Exception {
    public InvalidLocaleException(String message) {
        super(message);
    }
}
