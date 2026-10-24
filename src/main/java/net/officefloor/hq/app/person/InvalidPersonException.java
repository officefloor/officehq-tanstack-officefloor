package net.officefloor.hq.app.person;

/** A person form that cannot be saved: an empty name, a name already in use on the sheet, or a grade other than A, B or C. */
public class InvalidPersonException extends Exception {
    public InvalidPersonException(String message) {
        super(message);
    }
}
