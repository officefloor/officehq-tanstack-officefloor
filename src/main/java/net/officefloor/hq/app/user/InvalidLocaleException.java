package net.officefloor.hq.app.user;

/** A locale was chosen that is not one of {@link Locales#KNOWN}. */
public class InvalidLocaleException extends Exception {

    public InvalidLocaleException(String message) {
        super(message);
    }
}
