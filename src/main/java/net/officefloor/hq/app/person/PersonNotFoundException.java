package net.officefloor.hq.app.person;

/** No such person on the sheet. */
public class PersonNotFoundException extends Exception {
    public PersonNotFoundException(String message) {
        super(message);
    }
}
