package net.officefloor.hq.app.person;

/** A person's values that cannot be kept: an empty name, or a grade other than A, B or C. */
public class InvalidPersonException extends Exception {
    public InvalidPersonException(String message) {
        super(message);
    }
}
