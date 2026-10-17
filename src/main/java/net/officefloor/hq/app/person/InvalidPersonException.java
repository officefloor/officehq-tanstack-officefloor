package net.officefloor.hq.app.person;

/** The requested person is not valid (no name, a name already in use, or an unknown grade); nothing is changed. */
public class InvalidPersonException extends Exception {
    public InvalidPersonException(String message) {
        super(message);
    }
}
