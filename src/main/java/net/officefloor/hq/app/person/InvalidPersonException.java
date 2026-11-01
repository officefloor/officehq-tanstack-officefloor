package net.officefloor.hq.app.person;

/** The person's name is empty or the grade is not one of A, B or C. */
public class InvalidPersonException extends Exception {
    public InvalidPersonException(String message) {
        super(message);
    }
}
