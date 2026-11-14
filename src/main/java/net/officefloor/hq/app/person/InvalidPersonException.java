package net.officefloor.hq.app.person;

/** The person's name is empty or the grade is not A, B or C: refused and nothing is written. */
public class InvalidPersonException extends Exception {
    public InvalidPersonException(String message) {
        super(message);
    }
}
