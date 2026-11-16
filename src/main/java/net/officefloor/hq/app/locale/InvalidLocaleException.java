package net.officefloor.hq.app.locale;

/** A locale that is neither en nor de, or a blank user. */
public class InvalidLocaleException extends Exception {
    public InvalidLocaleException(String message) {
        super(message);
    }
}
