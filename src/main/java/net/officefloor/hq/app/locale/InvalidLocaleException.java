package net.officefloor.hq.app.locale;

/** A locale choice with no signed-in user, or a locale not offered. */
public class InvalidLocaleException extends Exception {
    public InvalidLocaleException(String message) {
        super(message);
    }
}
