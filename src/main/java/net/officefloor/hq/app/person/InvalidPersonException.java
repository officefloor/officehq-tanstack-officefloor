package net.officefloor.hq.app.person;

/** A person was given without a name, or with a grade that is not A, B or C. */
public class InvalidPersonException extends Exception {

    public InvalidPersonException(String message) {
        super(message);
    }
}
