package net.officefloor.hq.app.person;

/** A person form that cannot be saved (no name, an unknown grade); nothing is written. */
public class InvalidPersonException extends Exception {
    public InvalidPersonException(String message) {
        super(message);
    }
}
