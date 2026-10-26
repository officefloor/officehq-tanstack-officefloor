package net.officefloor.hq.app.person;

/** A person was given without a name, with a grade that is not A, B or C, or with a name already in use on the sheet. */
public class InvalidPersonException extends Exception {

    public InvalidPersonException(String message) {
        super(message);
    }
}
