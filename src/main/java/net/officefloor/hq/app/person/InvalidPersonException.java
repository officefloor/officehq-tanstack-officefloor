package net.officefloor.hq.app.person;

/** A person add or edit with no name, an unknown grade, or no signed-in user. */
public class InvalidPersonException extends Exception {
    public InvalidPersonException(String message) {
        super(message);
    }
}
