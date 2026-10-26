package net.officefloor.hq.app.person;

/** No person with this id is on the sheet. */
public class PersonNotFoundException extends Exception {

    public PersonNotFoundException(String message) {
        super(message);
    }
}
