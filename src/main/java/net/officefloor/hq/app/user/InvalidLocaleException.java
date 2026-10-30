package net.officefloor.hq.app.user;

/** A locale the app does not know; the change is refused and nothing is written. */
public class InvalidLocaleException extends Exception {
    public InvalidLocaleException(String value) {
        super("Not a locale (" + String.join(", ", UserLocales.LOCALES) + "): " + value);
    }
}
